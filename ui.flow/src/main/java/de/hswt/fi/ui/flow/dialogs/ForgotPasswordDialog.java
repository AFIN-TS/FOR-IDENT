package de.hswt.fi.ui.flow.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.spring.annotation.SpringComponent;
import de.hswt.fi.security.service.vaadin.PasswordResetService;
import de.hswt.fi.ui.flow.mail.AccountMails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * "Forgotten your password?": sends a link to set a new password (valid for one hour) to the e-mail address of an
 * enabled account. The answer is the same whether the account exists or not, so the dialog does not reveal which
 * addresses are registered.
 */
@SpringComponent
@Scope("prototype")
public class ForgotPasswordDialog extends Dialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPasswordDialog.class);

	private final transient PasswordResetService passwordResetService;

	private final transient AccountMails accountMails;

	private final boolean logResetLinks;

	private final EmailField mail;

	public ForgotPasswordDialog(PasswordResetService passwordResetService, AccountMails accountMails,
								@Value("${de.hswt.fi.security.log-reset-links:false}") boolean logResetLinks) {
		this.passwordResetService = passwordResetService;
		this.accountMails = accountMails;
		this.logResetLinks = logResetLinks;
		setHeaderTitle(getTranslation("ui.forgot.title"));
		setWidth("min(30rem, 95vw)");

		mail = UserFields.mailField(this);
		add(new Paragraph(getTranslation("ui.forgot.description")), mail);

		Button ok = new Button(getTranslation("ui.button.ok"), e -> requestLink());
		ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button cancel = new Button(getTranslation("ui.button.cancel"), e -> close());
		getFooter().add(cancel, ok);
		mail.focus();
	}

	private void requestLink() {
		if (mail.isEmpty() || mail.isInvalid()) {
			mail.setInvalid(true);
			mail.setErrorMessage(getTranslation("ui.validation.emailInvalid"));
			return;
		}
		String address = mail.getValue().trim();
		passwordResetService.createToken(address).ifPresent(token -> {
			String link = ServletUriComponentsBuilder
					.fromContextPath(VaadinServletRequest.getCurrent().getHttpServletRequest())
					.path("/reset-password").queryParam("token", token).build().toUriString();
			if (logResetLinks) {
				// Local tests without mail server only (property de.hswt.fi.security.log-reset-links)
				LOGGER.warn("Password reset link (log-reset-links is on): {}", link);
			}
			accountMails.sendPasswordResetLink(address, link, getLocale());
		});
		close();
		UserFields.showSuccess(getTranslation("ui.forgot.sent"));
	}
}
