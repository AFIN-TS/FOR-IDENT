package de.hswt.fi.ui.flow.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.hswt.fi.security.service.vaadin.PasswordResetService;

import java.util.List;

/**
 * Target of the password reset link: sets a new password if the token in the link is valid (one hour, single use).
 */
@Route("reset-password")
@AnonymousAllowed
public class ResetPasswordView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

	private static final int PASSWORD_MIN_LENGTH = 6;

	private final transient PasswordResetService passwordResetService;

	private final Div card = new Div();

	private String token;

	public ResetPasswordView(PasswordResetService passwordResetService) {
		this.passwordResetService = passwordResetService;
		setSizeFull();
		addClassName("fi-login-page");
		setAlignItems(FlexComponent.Alignment.CENTER);
		setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		card.addClassName("fi-card");
		add(card);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		token = event.getLocation().getQueryParameters().getParameters()
				.getOrDefault("token", List.of()).stream().findFirst().orElse(null);
		card.removeAll();
		card.add(new H2(getTranslation("ui.reset.title")));
		if (passwordResetService.validate(token).isEmpty()) {
			card.add(new Paragraph(getTranslation("ui.reset.invalid")), loginLink());
			return;
		}
		PasswordField password = new PasswordField(getTranslation("ui.user.passwordMin", PASSWORD_MIN_LENGTH));
		PasswordField confirmation = new PasswordField(getTranslation("ui.user.passwordConfirmation"));
		password.setWidthFull();
		confirmation.setWidthFull();
		Button save = new Button(getTranslation("ui.button.save"), e -> save(password, confirmation));
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		card.add(new Paragraph(getTranslation("ui.reset.description")), password, confirmation, save);
		password.focus();
	}

	private void save(PasswordField password, PasswordField confirmation) {
		password.setInvalid(false);
		confirmation.setInvalid(false);
		if (password.getValue().length() < PASSWORD_MIN_LENGTH) {
			password.setErrorMessage(getTranslation("ui.validation.passwordShort", PASSWORD_MIN_LENGTH));
			password.setInvalid(true);
			return;
		}
		if (!password.getValue().equals(confirmation.getValue())) {
			confirmation.setErrorMessage(getTranslation("ui.validation.passwordMismatch"));
			confirmation.setInvalid(true);
			return;
		}
		card.removeAll();
		card.add(new H2(getTranslation("ui.reset.title")));
		if (passwordResetService.resetPassword(token, password.getValue())) {
			card.add(new Paragraph(getTranslation("ui.reset.success")), loginLink());
		} else {
			card.add(new Paragraph(getTranslation("ui.reset.invalid")), loginLink());
		}
	}

	private RouterLink loginLink() {
		return new RouterLink(getTranslation("ui.reset.toLogin"), LoginView.class);
	}

	@Override
	public String getPageTitle() {
		return getTranslation("ui.reset.title");
	}
}
