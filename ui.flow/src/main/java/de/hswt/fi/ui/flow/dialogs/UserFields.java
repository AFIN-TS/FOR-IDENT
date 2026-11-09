package de.hswt.fi.ui.flow.dialogs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.function.Predicate;

/**
 * Fields and checks of the user forms (registration, settings, administration) with the limits of the Vaadin 8 UI:
 * e-mail and user name 60, first and last name 40, organisation 80 characters, password at least 6 characters.
 */
final class UserFields {

	static final int MAIL_LENGTH = 60;

	static final int NAME_LENGTH = 40;

	static final int ORGANISATION_LENGTH = 80;

	static final int PASSWORD_MIN_LENGTH = 6;

	private UserFields() {
	}

	static EmailField mailField(Component owner) {
		EmailField field = new EmailField(owner.getTranslation("ui.user.email"));
		field.setMaxLength(MAIL_LENGTH);
		field.setWidthFull();
		return field;
	}

	static TextField textField(Component owner, String key, int maxLength) {
		TextField field = new TextField(owner.getTranslation(key));
		field.setMaxLength(maxLength);
		field.setWidthFull();
		return field;
	}

	static PasswordField passwordField(Component owner, String key) {
		PasswordField field = new PasswordField(owner.getTranslation(key, PASSWORD_MIN_LENGTH));
		field.setWidthFull();
		return field;
	}

	/** Valid e-mail address that is not used by another account ({@code inUse} gets the lower case address). */
	static Validator<String> mailValidator(Component owner, Predicate<String> inUse) {
		return (value, context) -> {
			String mail = value == null ? "" : value.trim().toLowerCase();
			if (!EmailValidator.getInstance().isValid(mail)) {
				return ValidationResult.error(owner.getTranslation("ui.validation.emailInvalid"));
			}
			if (inUse.test(mail)) {
				return ValidationResult.error(owner.getTranslation("ui.validation.emailInUse"));
			}
			return ValidationResult.ok();
		};
	}

	/** At least {@link #PASSWORD_MIN_LENGTH} characters; empty is allowed if {@code optional}. */
	static Validator<String> passwordValidator(Component owner, boolean optional) {
		return (value, context) -> (optional && (value == null || value.isEmpty()))
				|| (value != null && value.length() >= PASSWORD_MIN_LENGTH)
				? ValidationResult.ok()
				: ValidationResult.error(owner.getTranslation("ui.validation.passwordShort", PASSWORD_MIN_LENGTH));
	}

	static Validator<String> confirmationValidator(Component owner, PasswordField password) {
		return (value, context) -> java.util.Objects.equals(value == null ? "" : value, password.getValue())
				? ValidationResult.ok()
				: ValidationResult.error(owner.getTranslation("ui.validation.passwordMismatch"));
	}

	static void showSuccess(String text) {
		Notification.show(text, 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
	}

	static void showError(String text) {
		Notification.show(text, 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
	}
}
