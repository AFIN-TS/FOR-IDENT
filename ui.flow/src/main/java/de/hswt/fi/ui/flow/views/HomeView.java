package de.hswt.fi.ui.flow.views;

import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.hswt.fi.application.properties.ApplicationProperties;
import de.hswt.fi.ui.flow.i18n.FiI18NProvider;
import de.hswt.fi.ui.flow.layout.MainLayout;
import de.hswt.fi.ui.flow.views.search.SearchView;
import org.springframework.beans.factory.annotation.Value;

/**
 * Start page: shows the static home page of the html folder ({@code spring.web.resources.static-locations}) in the
 * language of the UI, as the HomeView of the Vaadin 8 UI.
 */
@Route(value = "", layout = MainLayout.class)
@AnonymousAllowed
public class HomeView extends VerticalLayout implements HasDynamicTitle, MainLayout.HasViewTitle, BeforeEnterObserver {

	private final transient ApplicationProperties applicationProperties;

	public HomeView(ApplicationProperties applicationProperties,
					@Value("${html-home-english:home_en.html}") String englishPage,
					@Value("${html-home-german:home_de.html}") String germanPage) {
		this.applicationProperties = applicationProperties;
		setSizeFull();
		setPadding(false);
		boolean german = FiI18NProvider.GERMAN.equals(FiI18NProvider.supported(getLocale()));
		IFrame page = new IFrame("/" + (german ? germanPage : englishPage));
		page.addClassName("fi-home-frame");
		page.setTitle(getViewTitle());
		add(page);
	}

	/** Old links to a substance (/?stoffidentid=... or ?inchikey=...) open the search view. */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (SearchView.isDeepLink(event.getLocation().getQueryParameters())) {
			event.forwardTo("search", event.getLocation().getQueryParameters());
		}
	}

	@Override
	public String getViewTitle() {
		return getTranslation("ui.nav.home");
	}

	@Override
	public String getPageTitle() {
		return applicationProperties.getUi().getHeader().getCaption();
	}
}
