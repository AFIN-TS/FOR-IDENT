package de.hswt.fi.ui.flow.views.search;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.ui.flow.layout.MainLayout;
import de.hswt.fi.ui.flow.search.SearchHistory;
import de.hswt.fi.ui.flow.search.SearchViewServices;

/**
 * Quick search by monoisotopic mass, ion species, logP and halogens (QuickSearchView of the Vaadin 8 UI); open to
 * everybody.
 */
@Route(value = "quicksearch", layout = MainLayout.class)
@AnonymousAllowed
public class QuickSearchView extends AbstractSearchView {

	private SearchForm form;

	public QuickSearchView(SearchViewServices services) {
		super(services);
	}

	@Override
	protected Component createForm() {
		form = new SearchForm(false, this::search,
				(value, field) -> warn("ui.search.invalidTitle", "ui.search.invalid", value, field));
		return form;
	}

	@Override
	protected SearchHistory.Kind getKind() {
		return SearchHistory.Kind.QUICK;
	}

	@Override
	protected void showParameter(SearchParameter parameter) {
		form.show(parameter);
	}

	@Override
	public String getViewTitle() {
		return getTranslation("ui.nav.quickSearch");
	}
}
