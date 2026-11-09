package de.hswt.fi.ui.flow.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.hswt.fi.application.properties.ApplicationProperties;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.model.RegisteredUser;
import de.hswt.fi.ui.flow.dialogs.RegistrationDialog;
import de.hswt.fi.ui.flow.dialogs.UserSettingsDialog;
import de.hswt.fi.ui.flow.i18n.FiI18NProvider;
import de.hswt.fi.ui.flow.views.HomeView;
import de.hswt.fi.ui.flow.views.LoginView;
import de.hswt.fi.ui.flow.views.admin.UserAdministrationView;
import de.hswt.fi.ui.flow.views.processing.ProcessingView;
import de.hswt.fi.ui.flow.views.search.IndexSearchView;
import de.hswt.fi.ui.flow.views.search.QuickSearchView;
import de.hswt.fi.ui.flow.views.search.SearchView;
import de.hswt.fi.userproperties.service.api.UserPropertiesService;
import de.hswt.fi.userproperties.service.api.UserPropertyKeys;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Frame of all views (SP-11): side bar with the application name, language switch, user area, the sections of the
 * views and the dates of the last data updates; header with the title of the view and the logo. Replaces DefaultMainUi,
 * SideBar and UserMenu of the Vaadin 8 UI.
 */
@AnonymousAllowed
public class MainLayout extends AppLayout implements AfterNavigationObserver {

	private static final String COOKIES_ACCEPTED = "de.hswt.fi.ui.flow.cookies-accepted";

	private final transient AuthenticationContext authenticationContext;

	private final transient SecurityService securityService;

	private final transient ApplicationProperties applicationProperties;

	private final transient List<CompoundSearchService> compoundSearchServices;

	private final transient ObjectProvider<UserPropertiesService> userPropertiesService;

	private final transient ObjectProvider<RegistrationDialog> registrationDialog;

	private final transient ObjectProvider<UserSettingsDialog> userSettingsDialog;

	private final String logo;

	private final H2 viewTitle = new H2();

	public MainLayout(AuthenticationContext authenticationContext, SecurityService securityService,
					  ApplicationProperties applicationProperties, List<CompoundSearchService> compoundSearchServices,
					  ObjectProvider<UserPropertiesService> userPropertiesService,
					  ObjectProvider<RegistrationDialog> registrationDialog,
					  ObjectProvider<UserSettingsDialog> userSettingsDialog,
					  @Value("${de.hswt.fi.ui.header.logo:img/logo.png}") String logo) {
		this.logo = logo;
		this.authenticationContext = authenticationContext;
		this.securityService = securityService;
		this.applicationProperties = applicationProperties;
		this.compoundSearchServices = compoundSearchServices;
		this.userPropertiesService = userPropertiesService;
		this.registrationDialog = registrationDialog;
		this.userSettingsDialog = userSettingsDialog;

		setPrimarySection(Section.DRAWER);
		addToNavbar(createNavbar());
		addToDrawer(createDrawer());
		addCookieConsentIfNeeded();
	}

	private Component createNavbar() {
		viewTitle.addClassName("fi-navbar-title");
		Image logoImage = new Image(logo,
				applicationProperties.getUi().getHeader().getCaption());
		logoImage.addClassName("fi-navbar-logo");
		HorizontalLayout navbar = new HorizontalLayout(new DrawerToggle(), viewTitle, logoImage);
		navbar.setWidthFull();
		navbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		return navbar;
	}

	private Component createDrawer() {
		Div drawer = new Div(createBrand(), createLanguageSwitch(), createUserArea(), createNavigation(), createFooter());
		drawer.addClassName("fi-drawer");
		return drawer;
	}

	private Component createBrand() {
		H1 caption = new H1(applicationProperties.getUi().getHeader().getCaption());
		Div brand = new Div(caption);
		brand.addClassName("fi-brand");
		return brand;
	}

	private Component createLanguageSwitch() {
		Locale current = FiI18NProvider.supported(UI.getCurrent().getLocale());
		HorizontalLayout languages = new HorizontalLayout();
		languages.addClassName("fi-language");
		languages.setSpacing(false);
		languages.getStyle().set("padding", "var(--lumo-space-xs) var(--lumo-space-m)");
		for (Locale locale : List.of(FiI18NProvider.GERMAN, FiI18NProvider.ENGLISH)) {
			Button button = new Button(locale.getLanguage().toUpperCase(Locale.ROOT), e -> switchLanguage(locale));
			button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
			button.getElement().setAttribute("aria-pressed", String.valueOf(locale.equals(current)));
			button.getElement().setAttribute("title", getTranslation("ui.nav.language") + ": "
					+ locale.getDisplayLanguage(locale));
			languages.add(button);
		}
		return languages;
	}

	private static void switchLanguage(Locale locale) {
		VaadinSession.getCurrent().setLocale(locale);
		UI.getCurrent().getPage().reload();
	}

	private Component createUserArea() {
		Div area = new Div();
		area.addClassName("fi-user-area");
		if (authenticationContext.isAuthenticated()) {
			RegisteredUser user = securityService.getCurrentUser();
			Span name = new Span(VaadinIcon.USER.create(), new Span(" " + (user == null ? "" : user.getUsername())));
			name.addClassName("fi-user-name");
			Button settings = new Button(getTranslation("ui.menu.settings"), VaadinIcon.COG.create(),
					e -> userSettingsDialog.getObject().open());
			Button logout = new Button(getTranslation("ui.menu.logout"), VaadinIcon.SIGN_OUT.create(),
					e -> authenticationContext.logout());
			settings.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
			logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
			area.add(name, new Div(settings, logout));
		} else {
			Button login = new Button(getTranslation("ui.menu.login"), VaadinIcon.SIGN_IN.create(),
					e -> UI.getCurrent().navigate(LoginView.class));
			Button register = new Button(getTranslation("ui.menu.register"), VaadinIcon.USER_CHECK.create(),
					e -> registrationDialog.getObject().open());
			login.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
			register.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
			register.setVisible(applicationProperties.getSecurity().isRegistrationEnabled());
			area.add(register, login);
		}
		return area;
	}

	private Component createNavigation() {
		Div navigation = new Div();
		navigation.addClassName("fi-nav");

		SideNav top = new SideNav(getTranslation("ui.section.top"));
		top.addItem(new SideNavItem(getTranslation("ui.nav.home"), HomeView.class, VaadinIcon.HOME.create()));
		navigation.add(top);

		SideNav search = new SideNav(getTranslation("ui.section.search"));
		search.addItem(new SideNavItem(getTranslation("ui.nav.quickSearch"), QuickSearchView.class,
				VaadinIcon.SEARCH.create()));
		search.addItem(new SideNavItem(getTranslation("ui.nav.search"), SearchView.class,
				VaadinIcon.SEARCH_PLUS.create()));
		search.addItem(new SideNavItem(getTranslation("ui.nav.indexSearch"), IndexSearchView.class,
				VaadinIcon.GRID_SMALL.create()));
		navigation.add(search);

		// Molecular screening needs a login; without one the view leads to the login page
		SideNav processing = new SideNav(getTranslation("ui.section.processing"));
		processing.addItem(new SideNavItem(getTranslation("ui.nav.processing"), ProcessingView.class,
				VaadinIcon.COG.create()));
		navigation.add(processing);

		if (authenticationContext.hasRole("ADMIN")) {
			SideNav administration = new SideNav(getTranslation("ui.section.administration"));
			administration.addItem(new SideNavItem(getTranslation("ui.nav.users"), UserAdministrationView.class,
					VaadinIcon.USERS.create()));
			navigation.add(administration);
		}
		return navigation;
	}

	private Component createFooter() {
		Div footer = new Div();
		footer.addClassName("fi-drawer-footer");
		footer.add(new Div(new Span(getTranslation("ui.sidebar.lastUpdate"))));
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
		for (CompoundSearchService service : compoundSearchServices) {
			Div line = new Div(new Span(service.getDatasourceName() + ": "));
			line.add(new Span(format.format(new Date(service.getLastUpdateTime()))));
			line.getElement().getChild(1).getStyle().set("font-weight", "600");
			footer.add(line);
		}
		// Version for administrators, as in the Vaadin 8 UI
		String version = MainLayout.class.getPackage().getImplementationVersion();
		if (authenticationContext.hasRole("ADMIN") && version != null) {
			footer.add(new Div(new Span(getTranslation("ui.sidebar.version", version))));
		}
		return footer;
	}

	private void addCookieConsentIfNeeded() {
		VaadinSession session = VaadinSession.getCurrent();
		boolean accepted = Boolean.TRUE.equals(session.getAttribute(COOKIES_ACCEPTED))
				|| (authenticationContext.isAuthenticated() && userPropertiesService.getObject()
						.getBooleanValueOrDefault(UserPropertyKeys.ACCEPT_COOKIES, false));
		if (accepted) {
			return;
		}
		Div consent = new Div(new Span(getTranslation("ui.cookie.message")));
		consent.addClassName("fi-cookie-consent");
		Button ok = new Button(getTranslation("ui.cookie.button"), e -> {
			session.setAttribute(COOKIES_ACCEPTED, true);
			if (authenticationContext.isAuthenticated()) {
				userPropertiesService.getObject().setBooleanValue(UserPropertyKeys.ACCEPT_COOKIES, true);
			}
			consent.removeFromParent();
		});
		consent.add(ok);
		getElement().appendChild(consent.getElement());
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		Component content = getContent();
		PageTitle title = content == null ? null : content.getClass().getAnnotation(PageTitle.class);
		viewTitle.setText(content instanceof HasViewTitle hasTitle ? hasTitle.getViewTitle()
				: title == null ? "" : title.value());
	}

	/** Views with a translated title in the header. */
	public interface HasViewTitle {
		String getViewTitle();
	}
}
