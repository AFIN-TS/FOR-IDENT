package de.hswt.fi.ui.flow.mail;

import de.hswt.fi.application.properties.ApplicationProperties;
import de.hswt.fi.mail.service.MailService;
import de.hswt.fi.mail.service.model.MailTemplate;
import de.hswt.fi.mail.service.model.RequestUserAccountNotifyAdminTemplate;
import de.hswt.fi.security.service.model.RegisteredUser;
import de.hswt.fi.ui.flow.i18n.FiI18NProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.Locale;
import java.util.Map;

/**
 * E-mails of the account functions (registration, activation, password reset), with the texts of the Vaadin 8 UI.
 * A failed mail (e.g. no mail server configured) is logged and does not abort the action.
 */
@Component
public class AccountMails {

	private static final Logger LOGGER = LoggerFactory.getLogger(AccountMails.class);

	// Session scoped bean: fetched for every mail
	private final ObjectProvider<MailService> mailService;

	private final ApplicationProperties applicationProperties;

	private final FiI18NProvider i18n;

	public AccountMails(ObjectProvider<MailService> mailService, ApplicationProperties applicationProperties, FiI18NProvider i18n) {
		this.mailService = mailService;
		this.applicationProperties = applicationProperties;
		this.i18n = i18n;
	}

	/** After a registration: notification to the contact addresses (admins) and confirmation to the user. */
	public void sendRegistrationMails(RegisteredUser user) {
		String caption = caption();
		for (String admin : applicationProperties.getContactAdresses()) {
			send(() -> mailService.getObject().sendTemplateMail(admin, caption + " [REQUEST] : New User Account Request",
					MailTemplate.USER_ACCOUNT_REQUEST_NOTIFY_ADMIN.getLocation(),
					Map.of("accountRequest", new RequestUserAccountNotifyAdminTemplate(user.getUsername(), caption))),
					"registration notification to the admins");
		}
		send(() -> mailService.getObject().sendTemplateMail(user.getMail(), caption + " Account request",
				MailTemplate.USER_ACCOUNT_REQUEST_NOTIFY_USER.getLocation(), Map.of("caption", caption)),
				"registration confirmation to the user");
	}

	/** After an administrator enabled an account: mail to the user and notification to the contact addresses. */
	public void sendActivationMails(RegisteredUser user, Locale locale) {
		String caption = caption();
		send(() -> mailService.getObject().sendMail(user.getMail(), i18n.getTranslation("ui.admin.activation.subject", locale, caption),
				html(i18n.getTranslation("ui.admin.activation.text", locale, caption))), "activation mail");
		for (String admin : applicationProperties.getContactAdresses()) {
			send(() -> mailService.getObject().sendMail(admin, caption + " [ACTIVATION]: A new user account has been created",
					html("This email is to inform you, that a member of the " + caption + " admin group"
							+ " has activated the user with username: " + user.getUsername())),
					"activation notification to the admins");
		}
	}

	/** Link to set a new password (valid for one hour). */
	public void sendPasswordResetLink(String mail, String link, Locale locale) {
		String caption = caption();
		String text = html(i18n.getTranslation("ui.reset.mail.text", locale, caption, "LINK"))
				.replace("LINK", "<a href=\"" + HtmlUtils.htmlEscape(link) + "\">" + HtmlUtils.htmlEscape(link) + "</a>");
		send(() -> mailService.getObject().sendMail(mail, i18n.getTranslation("ui.reset.mail.subject", locale, caption), text),
				"password reset link");
	}

	/**
	 * Test mail to check the mail settings on a server (user administration, SP-16). Unlike the other mails, an error
	 * is passed on, so the administrator sees why sending failed.
	 */
	public void sendTestMail(String to, Locale locale) {
		String caption = caption();
		mailService.getObject().sendMail(to, i18n.getTranslation("ui.admin.testMail.subject", locale, caption),
				html(i18n.getTranslation("ui.admin.testMail.text", locale, caption)));
	}

	private String caption() {
		return applicationProperties.getUi().getHeader().getCaption();
	}

	/** The mail service sends HTML: escape the text and keep the line breaks. */
	private static String html(String text) {
		return HtmlUtils.htmlEscape(text).replace("\n", "<br>");
	}

	private static void send(Runnable mail, String description) {
		try {
			mail.run();
		} catch (RuntimeException e) {
			LOGGER.error("Could not send the {}: {}", description, e.getMessage());
		}
	}
}
