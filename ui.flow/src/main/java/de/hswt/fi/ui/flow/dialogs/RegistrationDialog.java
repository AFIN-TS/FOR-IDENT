package de.hswt.fi.ui.flow.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.SpringComponent;
import de.hswt.fi.security.service.api.RegistrationService;
import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.model.RegisteredUser;
import de.hswt.fi.ui.flow.mail.AccountMails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

/**
 * Request of a new user account (RegisterUserWindow of the Vaadin 8 UI). The e-mail address is the user name; the
 * account is created disabled and is enabled by an administrator.
 */
@SpringComponent
@Scope("prototype")
public class RegistrationDialog extends Dialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationDialog.class);

	private final transient RegistrationService registrationService;

	private final transient AccountMails accountMails;

	private final Binder<RegisteredUser> binder = new Binder<>();

	public RegistrationDialog(RegistrationService registrationService, SecurityService securityService,
							  AccountMails accountMails) {
		this.registrationService = registrationService;
		this.accountMails = accountMails;
		setHeaderTitle(getTranslation("ui.register.title"));
		setWidth("min(32rem, 95vw)");

		EmailField mail = UserFields.mailField(this);
		PasswordField password = UserFields.passwordField(this, "ui.user.passwordMin");
		PasswordField confirmation = UserFields.passwordField(this, "ui.user.passwordConfirmation");
		TextField firstname = UserFields.textField(this, "ui.user.firstname", UserFields.NAME_LENGTH);
		TextField lastname = UserFields.textField(this, "ui.user.lastname", UserFields.NAME_LENGTH);
		TextField organisation = UserFields.textField(this, "ui.user.organisation", UserFields.ORGANISATION_LENGTH);

		binder.forField(mail).asRequired(getTranslation("ui.validation.required"))
				.withValidator(UserFields.mailValidator(this, securityService::mailExists))
				.bind(RegisteredUser::getMail, (user, value) -> {
					user.setMail(value.trim());
					user.setUsername(value.trim());
				});
		binder.forField(password).asRequired(getTranslation("ui.validation.required"))
				.withValidator(UserFields.passwordValidator(this, false))
				.bind(RegisteredUser::getPassword, RegisteredUser::setPassword);
		binder.forField(confirmation).asRequired(getTranslation("ui.validation.required"))
				.withValidator(UserFields.confirmationValidator(this, password))
				.bind(user -> "", (user, value) -> { });
		binder.forField(firstname).bind(RegisteredUser::getFirstname, RegisteredUser::setFirstname);
		binder.forField(lastname).bind(RegisteredUser::getLastname, RegisteredUser::setLastname);
		binder.forField(organisation).bind(RegisteredUser::getOrganisation, RegisteredUser::setOrganisation);

		FormLayout form = new FormLayout(mail, password, confirmation, firstname, lastname, organisation);
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		add(new Paragraph(getTranslation("ui.register.description")), form);

		Button ok = new Button(getTranslation("ui.button.ok"), e -> register());
		ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button cancel = new Button(getTranslation("ui.button.cancel"), e -> close());
		getFooter().add(cancel, ok);
		mail.focus();
	}

	private void register() {
		RegisteredUser user = new RegisteredUser();
		if (!binder.writeBeanIfValid(user)) {
			return;
		}
		try {
			if (!registrationService.registerUser(user)) {
				UserFields.showError(getTranslation("ui.register.error"));
				return;
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Registration failed: {}", e.getMessage());
			UserFields.showError(getTranslation("ui.register.error"));
			return;
		}
		accountMails.sendRegistrationMails(user);
		close();
		UserFields.showSuccess(getTranslation("ui.register.success.title") + ". "
				+ getTranslation("ui.register.success.message"));
	}
}
