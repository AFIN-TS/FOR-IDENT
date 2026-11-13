package de.hswt.fi.mail.service;

import de.hswt.fi.application.properties.ApplicationProperties;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.Assert;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Map;

/**
 * Sends the HTML mails of the application. Mails from templates ({@code /templates/*.ftlh}) are rendered with
 * automatic HTML escaping, so text entered by a visitor (report comment, user name, ...) arrives as plain text and
 * cannot add links or markup (SP-20, review finding S2).
 *
 * <p>A mail that cannot be built (template error, invalid address) is not sent; the error is passed on as a
 * {@link org.springframework.mail.MailException}, so the caller can tell the user (SP-20, finding R10).
 */
@Component
@Scope("session")
public class MailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

	/** Shared, read-only after creation (thread safe); not the Spring Boot FreeMarker bean, which is not modified. */
	private static final Configuration TEMPLATES = createTemplateConfiguration();

	private final JavaMailSender mailSender;

	private final ApplicationProperties applicationProperties;

	@Autowired
	public MailService(JavaMailSender mailSender, ApplicationProperties applicationProperties) {
		this.mailSender = mailSender;
		this.applicationProperties = applicationProperties;
	}

	static Configuration createTemplateConfiguration() {
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
		configuration.setClassForTemplateLoading(MailService.class, "/templates");
		configuration.setDefaultEncoding("UTF-8");
		// HTML output format with auto-escaping for every template (.ftlh by extension, and as default for any other)
		configuration.setRecognizeStandardFileExtensions(true);
		configuration.setOutputFormat(HTMLOutputFormat.INSTANCE);
		configuration.setAutoEscapingPolicy(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
		configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		configuration.setLogTemplateExceptions(false);
		configuration.setWrapUncheckedExceptions(true);
		return configuration;
	}

	/**
	 * Renders a mail template to HTML.
	 *
	 * @throws MailPreparationException if the template is missing or cannot be rendered with the model
	 */
	public static String renderTemplate(String template, Map<String, Object> model) {
		try {
			Template freemarkerTemplate = TEMPLATES.getTemplate(template);
			return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerTemplate, model);
		} catch (IOException | TemplateException e) {
			throw new MailPreparationException("Mail template " + template + " could not be rendered", e);
		}
	}

	/**
	 * Renders the template and sends it.
	 *
	 * @throws org.springframework.mail.MailException if the mail could not be built or sent
	 */
	public void sendTemplateMail(String to, String subject, String template, Map<String, Object> model) {
		sendMail(to, subject, renderTemplate(template, model));
	}

	/**
	 * Sends an HTML mail; the caller is responsible for escaping {@code text}.
	 *
	 * @throws org.springframework.mail.MailException if the mail could not be built or sent
	 */
	public void sendMail(String to, String subject, String text) {
		Assert.notNull(to, "To must not be null");
		Assert.notNull(subject, "Subject must not be null");
		Assert.notNull(text, "Text must not be null");

		MimeMessage mimeMessage = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
			helper.setTo(to);
			helper.setText(text, true);
			helper.setSubject(subject);
			helper.setFrom(applicationProperties.getFromEmailAddress());
		} catch (MessagingException | IllegalArgumentException e) {
			// Never send a half-built message
			throw new MailPreparationException("Mail could not be built", e);
		}
		mailSender.send(mimeMessage);
		LOGGER.debug("mail sent");
	}

}
