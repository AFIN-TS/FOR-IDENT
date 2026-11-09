package de.hswt.fi.ui.flow.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.hswt.fi.application.properties.ApplicationProperties;
import de.hswt.fi.ui.flow.dialogs.ForgotPasswordDialog;
import de.hswt.fi.ui.flow.dialogs.RegistrationDialog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

/**
 * Login page with the links to the registration and to the password reset, as the login UI of the Vaadin 8 UI. The
 * login form posts to Spring Security ({@code /login}); the user name is the e-mail address.
 */
@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

	private final LoginForm login = new LoginForm();

	private final transient ApplicationProperties applicationProperties;

	public LoginView(ApplicationProperties applicationProperties, ObjectProvider<RegistrationDialog> registrationDialog,
					 ObjectProvider<ForgotPasswordDialog> forgotPasswordDialog,
					 @Value("${de.hswt.fi.ui.header.logo:img/logo.png}") String logo) {
		this.applicationProperties = applicationProperties;
		setSizeFull();
		setPadding(false);
		addClassName("fi-login-page");

		Image logoImage = new Image(logo, applicationProperties.getUi().getHeader().getCaption());
		logoImage.addClassName("fi-login-logo");

		login.setI18n(createI18n());
		login.setAction("login");
		login.addForgotPasswordListener(e -> forgotPasswordDialog.getObject().open());

		Button register = new Button(getTranslation("ui.login.register"), e -> registrationDialog.getObject().open());
		register.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		register.setVisible(applicationProperties.getSecurity().isRegistrationEnabled());
		RouterLink home = new RouterLink(applicationProperties.getUi().getHeader().getCaption(), HomeView.class);
		Div links = new Div(register, home);
		links.addClassName("fi-login-links");

		VerticalLayout center = new VerticalLayout(login, links);
		center.setAlignItems(FlexComponent.Alignment.CENTER);
		center.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		center.setSizeFull();

		Span browsers = new Span(getTranslation("ui.login.browsers"));
		browsers.addClassName("fi-login-browsers");

		add(logoImage, center, browsers);
		expand(center);
	}

	private LoginI18n createI18n() {
		LoginI18n.Form form = new LoginI18n.Form();
		form.setTitle(getTranslation("ui.login.title"));
		form.setUsername(getTranslation("ui.login.username"));
		form.setPassword(getTranslation("ui.login.password"));
		form.setSubmit(getTranslation("ui.login.submit"));
		form.setForgotPassword(getTranslation("ui.login.forgotPassword"));
		LoginI18n.ErrorMessage error = new LoginI18n.ErrorMessage();
		error.setTitle(getTranslation("ui.login.error.title"));
		error.setMessage(getTranslation("ui.login.error.message"));
		LoginI18n i18n = new LoginI18n();
		i18n.setForm(form);
		i18n.setErrorMessage(error);
		return i18n;
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		login.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
	}

	@Override
	public String getPageTitle() {
		return getTranslation("ui.login.title") + " | " + applicationProperties.getUi().getHeader().getCaption();
	}
}
