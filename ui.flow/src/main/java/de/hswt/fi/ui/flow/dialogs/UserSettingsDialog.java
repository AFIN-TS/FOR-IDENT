package de.hswt.fi.ui.flow.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.SpringComponent;
import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.model.RegisteredUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

/**
 * Settings of the logged in user (UserSettingsWindow of the Vaadin 8 UI): e-mail address, names, organisation and a
 * new password. The user name (login) stays unchanged.
 */
@SpringComponent
@Scope("prototype")
public class UserSettingsDialog extends Dialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserSettingsDialog.class);

	private final transient SecurityService securityService;

	private final Binder<RegisteredUser> binder = new Binder<>();

	private final PasswordField password;

	public UserSettingsDialog(SecurityService securityService) {
		this.securityService = securityService;
		setHeaderTitle(getTranslation("ui.settings.title"));
		setWidth("min(32rem, 95vw)");

		RegisteredUser user = securityService.getCurrentUser();
		String currentMail = user == null ? "" : user.getMail();

		TextField username = UserFields.textField(this, "ui.user.username", UserFields.MAIL_LENGTH);
		username.setReadOnly(true);
		username.setValue(user == null ? "" : user.getUsername());
		EmailField mail = UserFields.mailField(this);
		TextField firstname = UserFields.textField(this, "ui.user.firstname", UserFields.NAME_LENGTH);
		TextField lastname = UserFields.textField(this, "ui.user.lastname", UserFields.NAME_LENGTH);
		TextField organisation = UserFields.textField(this, "ui.user.organisation", UserFields.ORGANISATION_LENGTH);
		password = UserFields.passwordField(this, "ui.user.newPassword");
		password.setHelperText(getTranslation("ui.user.newPasswordHint"));
		PasswordField confirmation = UserFields.passwordField(this, "ui.user.passwordConfirmation");

		binder.forField(mail).asRequired(getTranslation("ui.validation.required"))
				.withValidator(UserFields.mailValidator(this,
						value -> !value.equals(currentMail) && securityService.mailExists(value)))
				.bind(RegisteredUser::getMail, (u, value) -> u.setMail(value.trim()));
		binder.forField(firstname).bind(RegisteredUser::getFirstname, RegisteredUser::setFirstname);
		binder.forField(lastname).bind(RegisteredUser::getLastname, RegisteredUser::setLastname);
		binder.forField(organisation).bind(RegisteredUser::getOrganisation, RegisteredUser::setOrganisation);
		binder.forField(password).withValidator(UserFields.passwordValidator(this, true))
				.bind(u -> "", (u, value) -> { });
		binder.forField(confirmation).withValidator(UserFields.confirmationValidator(this, password))
				.bind(u -> "", (u, value) -> { });
		binder.readBean(user);

		FormLayout form = new FormLayout(username, mail, firstname, lastname, organisation, password, confirmation);
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		add(form);

		Button save = new Button(getTranslation("ui.button.save"), e -> save(user));
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		save.setEnabled(user != null);
		Button cancel = new Button(getTranslation("ui.button.cancel"), e -> close());
		getFooter().add(cancel, save);
	}

	private void save(RegisteredUser user) {
		if (!binder.validate().isOk()) {
			return;
		}
		try {
			binder.writeBean(user);
			if (!password.getValue().isEmpty()) {
				user.setPassword(password.getValue());
				securityService.hashPassword(user);
			}
			if (securityService.updateUser(user) == null) {
				UserFields.showError(getTranslation("ui.settings.error"));
				return;
			}
		} catch (Exception e) {
			LOGGER.warn("User settings not saved: {}", e.getMessage());
			UserFields.showError(getTranslation("ui.settings.error"));
			return;
		}
		close();
		UserFields.showSuccess(getTranslation("ui.settings.saved"));
	}
}
