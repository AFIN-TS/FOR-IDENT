package de.hswt.fi.ui.flow.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.spring.annotation.SpringComponent;
import de.hswt.fi.application.properties.ApplicationProperties;
import de.hswt.fi.common.PasswordGenerator;
import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.model.Database;
import de.hswt.fi.security.service.model.Group;
import de.hswt.fi.security.service.model.RegisteredUser;
import de.hswt.fi.ui.flow.mail.AccountMails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adding and editing a user by an administrator (AdminUserWindow of the Vaadin 8 UI): account data, enabled, groups,
 * accessible databases, optional new password and activation mail.
 */
@SpringComponent
@Scope("prototype")
public class UserEditDialog extends Dialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserEditDialog.class);

	private final transient SecurityService securityService;

	private final transient AccountMails accountMails;

	private final transient PasswordGenerator passwordGenerator;

	private final transient ApplicationProperties applicationProperties;

	private final Binder<RegisteredUser> binder = new Binder<>();

	private final PasswordField password;

	private final Checkbox enabled = new Checkbox();

	private final Checkbox activationMail = new Checkbox();

	private final CheckboxGroup<Group> groups = new CheckboxGroup<>();

	private final CheckboxGroup<Database> databases = new CheckboxGroup<>();

	private RegisteredUser user;

	private boolean newUser;

	private Runnable onSaved = () -> { };

	public UserEditDialog(SecurityService securityService, AccountMails accountMails,
						  PasswordGenerator passwordGenerator, ApplicationProperties applicationProperties) {
		this.securityService = securityService;
		this.accountMails = accountMails;
		this.passwordGenerator = passwordGenerator;
		this.applicationProperties = applicationProperties;
		setWidth("min(44rem, 95vw)");

		EmailField mail = UserFields.mailField(this);
		TextField username = UserFields.textField(this, "ui.user.username", UserFields.MAIL_LENGTH);
		TextField firstname = UserFields.textField(this, "ui.user.firstname", UserFields.NAME_LENGTH);
		TextField lastname = UserFields.textField(this, "ui.user.lastname", UserFields.NAME_LENGTH);
		TextField organisation = UserFields.textField(this, "ui.user.organisation", UserFields.ORGANISATION_LENGTH);
		password = UserFields.passwordField(this, "ui.user.newPassword");
		password.setRevealButtonVisible(true);
		Button generate = new Button(getTranslation("ui.user.generate"),
				e -> password.setValue(passwordGenerator.generatePassword(passwordLength())));
		HorizontalLayout passwordRow = new HorizontalLayout(password, generate);
		passwordRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
		passwordRow.setWidthFull();
		passwordRow.expand(password);
		enabled.setLabel(getTranslation("ui.user.enabled"));
		activationMail.setLabel(getTranslation("ui.user.activationMail"));
		groups.setLabel(getTranslation("ui.user.groups"));
		groups.setItems(securityService.findAllGroups());
		groups.setItemLabelGenerator(Group::getName);
		databases.setLabel(getTranslation("ui.user.databases"));
		databases.setItems(securityService.findAllDatabases());
		databases.setItemLabelGenerator(Database::getName);

		binder.forField(mail).asRequired(getTranslation("ui.validation.required"))
				.withValidator(UserFields.mailValidator(this, value -> !value.equals(user.getMail())
						&& securityService.mailExists(value)))
				.bind(RegisteredUser::getMail, (u, value) -> u.setMail(value.trim()));
		binder.forField(username).asRequired(getTranslation("ui.validation.required"))
				.withValidator((value, context) -> !newUser || !securityService.userExists(value)
						? ValidationResult.ok() : ValidationResult.error(getTranslation("ui.validation.emailInUse")))
				.bind(RegisteredUser::getUsername, (u, value) -> u.setUsername(value.trim()));
		binder.forField(firstname).bind(RegisteredUser::getFirstname, RegisteredUser::setFirstname);
		binder.forField(lastname).bind(RegisteredUser::getLastname, RegisteredUser::setLastname);
		binder.forField(organisation).bind(RegisteredUser::getOrganisation, RegisteredUser::setOrganisation);
		binder.forField(password)
				.withValidator((value, context) -> !newUser || !value.isEmpty()
						? ValidationResult.ok() : ValidationResult.error(getTranslation("ui.admin.passwordRequired")))
				.withValidator(UserFields.passwordValidator(this, true))
				.bind(u -> "", (u, value) -> { });
		// New users: the user name follows the e-mail address as in the registration
		mail.addValueChangeListener(e -> {
			if (newUser && (username.isEmpty() || username.getValue().equalsIgnoreCase(e.getOldValue()))) {
				username.setValue(e.getValue());
			}
		});

		FormLayout form = new FormLayout(mail, username, firstname, lastname, organisation, passwordRow, enabled,
				activationMail, groups, databases);
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("30rem", 2));
		form.setColspan(passwordRow, 2);
		add(form);

		Button save = new Button(getTranslation("ui.button.save"), e -> save());
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button cancel = new Button(getTranslation("ui.button.cancel"), e -> close());
		getFooter().add(cancel, save);
	}

	/** Opens the dialog for the given user, or for a new user if {@code null}. */
	public void open(RegisteredUser userToEdit, Runnable onSaved) {
		this.onSaved = onSaved;
		newUser = userToEdit == null;
		user = newUser ? new RegisteredUser() : userToEdit;
		setHeaderTitle(getTranslation(newUser ? "ui.admin.addTitle" : "ui.admin.editTitle"));
		binder.readBean(user);
		password.clear();
		enabled.setValue(user.isEnabled());
		activationMail.setValue(false);
		groups.setValue(selected(groups.getListDataView().getItems().toList(), user.getGroups()));
		databases.setValue(selected(databases.getListDataView().getItems().toList(), user.getAccessibleDatabases()));
		open();
	}

	private static <T> Set<T> selected(List<T> items, java.util.Collection<T> assigned) {
		Set<T> result = new HashSet<>(items);
		result.retainAll(new HashSet<>(assigned));
		return result;
	}

	private void save() {
		if (!binder.validate().isOk()) {
			return;
		}
		try {
			RegisteredUser target = user;
			binder.writeBean(target);
			if (newUser) {
				target.setPassword(password.getValue());
				// Hashes the password; the account is created disabled, in the user group and with the public databases
				target = securityService.createUser(target);
				if (target == null) {
					UserFields.showError(getTranslation("ui.admin.error"));
					return;
				}
			} else if (!password.getValue().isEmpty()) {
				target.setPassword(password.getValue());
				securityService.hashPassword(target);
			}
			applyAccess(target);
			if (securityService.updateUser(target) == null) {
				UserFields.showError(getTranslation("ui.admin.error"));
				return;
			}
			if (activationMail.getValue()) {
				accountMails.sendActivationMails(target, getLocale());
			}
		} catch (Exception e) {
			LOGGER.warn("User not saved: {}", e.getMessage());
			UserFields.showError(getTranslation("ui.admin.error"));
			return;
		}
		close();
		UserFields.showSuccess(getTranslation("ui.admin.saved"));
		onSaved.run();
	}

	private void applyAccess(RegisteredUser target) {
		target.setEnabled(enabled.getValue());
		for (Group group : new ArrayList<>(target.getGroups())) {
			target.removeGroup(group);
		}
		groups.getValue().forEach(target::addGroup);
		target.removeAllAccessibleDatabases();
		databases.getValue().forEach(target::addAccessbileDatabase);
	}

	private int passwordLength() {
		int length = applicationProperties.getSecurity().getPasswordLength();
		return Math.max(length, 12);
	}
}
