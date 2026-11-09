package de.hswt.fi.mail.service;

import de.hswt.fi.application.properties.ApplicationProperties;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.Assert;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Map;

@Component
@Scope("session")
public class MailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

	private JavaMailSender mailSender;

	private ApplicationProperties applicationProperties;

	private Configuration configuration;

	@Autowired
	public MailService(JavaMailSender mailSender, ApplicationProperties applicationProperties,
			Configuration configuration) {
		this.mailSender = mailSender;
		this.applicationProperties = applicationProperties;
		this.configuration = configuration;
	}
	
	public void sendTemplateMail(String to, String subject, String template, Map<String, Object> model) {

		configuration.setClassForTemplateLoading(this.getClass(), "/templates");

		Template freemarkerTemplate;
		try {
			freemarkerTemplate = configuration.getTemplate(template,"UTF-8");
			String text = FreeMarkerTemplateUtils
					.processTemplateIntoString(freemarkerTemplate, model);
			sendMail(to, subject, text);
		} catch (IOException | TemplateException e) {
			LOGGER.error("An error occurred: {}", e);
		}

	}

	public void sendMail(String to, String subject, String text) {
		Assert.notNull(to, "To must not be null");
		Assert.notNull(subject, "Subject must not be null");
		Assert.notNull(text, "Text must not be null");

		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
		try {
			helper.setTo(to);
			helper.setText(text, true);
			helper.setSubject(subject);
			helper.setFrom(applicationProperties.getFromEmailAddress());
		} catch (MessagingException e) {
			LOGGER.error("An error occurred", e);
		}
		mailSender.send(mimeMessage);
		LOGGER.debug("successfully send Email to: {}", to);
	}

}
