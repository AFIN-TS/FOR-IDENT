package de.hswt.fi.mail.service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@EnableConfigurationProperties(value = MailProperties.class)
public class MailServiceConfiguration {

	@Autowired
	private MailProperties mailProperties;

	/**
	 * SMTP connection from the spring.mail properties: host and port, and since SP-16 also the login of a mailbox and
	 * the JavaMail properties (STARTTLS/SSL, timeouts; see application.properties).
	 */
	@Bean
	public JavaMailSender javaMailService() {
		JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
		javaMailSender.setHost(mailProperties.getHost());
		javaMailSender.setPort(mailProperties.getPort());
		javaMailSender.setUsername(emptyToNull(mailProperties.getUsername()));
		javaMailSender.setPassword(emptyToNull(mailProperties.getPassword()));
		javaMailSender.getJavaMailProperties().putAll(mailProperties.getProperties());
		javaMailSender.setDefaultEncoding("UTF-8");
		return javaMailSender;
	}

	private static String emptyToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

}
