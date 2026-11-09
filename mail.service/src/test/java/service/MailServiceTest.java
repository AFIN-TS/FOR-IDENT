//package de.hswt.fi.mail.service;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Properties;
//
//import jakarta.mail.Message;
//import jakarta.mail.MessagingException;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
//import org.springframework.ui.velocity.VelocityEngineFactoryBean;
//
//import com.icegreen.greenmail.util.GreenMail;
//import com.icegreen.greenmail.util.ServerSetupTest;
//
//import ApplicationProperties;
//import de.hswt.fi.service.mail.MailService;
//import de.hswt.fi.service.mail.model.MailTemplate;
//
//public class MailServiceTest {
//
//	private ApplicationProperties applicationProperties;
//
//	private JavaMailSenderImpl mailSender;
//
//	private VelocityEngine velocityEngine;
//
//	private GreenMail greenMail;
//
//	@Before
//	public void init() throws VelocityException, IOException {
//		initMailSender();
//		initApplicationProperties();
//		initVelocityEngine();
//		initGreenMail();
//	}
//
//	@After
//	public void cleanup() {
//		greenMail.stop();
//	}
//
//	@Test
//	public void mailService_sendMail_messageSentWithoutException() throws MessagingException {
//		MailService mailService = new MailService(mailSender, applicationProperties, velocityEngine);
//		mailService.sendMail("test@reciever.com", "test subject", "test message");
//
//		Message[] messages = greenMail.getReceivedMessages();
//		assertEquals(1, messages.length);
//
//		Message message = messages[0];
//		assertEquals("test subject", message.getSubject());
//
//		assertEquals(1, message.getAllRecipients().length);
//	}
//
//	@Test
//	public void mailService_sendTemplateMail_allTemplateExists() {
//		MailService mailService = new MailService(mailSender, applicationProperties, velocityEngine);
//		String to = "test@reciever.com";
//		String subject = "template test subject";
//		Arrays.stream(MailTemplate.values())
//				.forEach(t -> mailService.sendTemplateMail(to, subject, t.getLocation(), Collections.emptyMap()));
//		assertEquals(greenMail.getReceivedMessages().length, MailTemplate.values().length);
//	}
//
//	@Test(expected = VelocityException.class)
//	public void mailService_sendTemplateMail_missingTemplateThrowsException() {
//		MailService mailService = new MailService(mailSender, applicationProperties, velocityEngine);
//		String to = "test@reciever.com";
//		String subject = "template test subject";
//		mailService.sendTemplateMail(to, subject, "mising", Collections.emptyMap());
//	}
//
//
//	private void initGreenMail() {
//		greenMail = new GreenMail(ServerSetupTest.SMTP);
//		greenMail.start();
//	}
//
//	private void initVelocityEngine() throws IOException {
//		VelocityEngineFactoryBean factory = new VelocityEngineFactoryBean();
//		Properties props = new Properties();
//		props.put("resource.loader", "class");
//		props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
//		factory.setVelocityProperties(props);
//		velocityEngine = factory.createVelocityEngine();
//	}
//
//	private void initApplicationProperties() {
//		applicationProperties = mock(ApplicationProperties.class);
//		when(applicationProperties.getFromEmailAddress()).thenReturn("junit@localhost");
//	}
//
//	private void initMailSender() {
//		mailSender = new JavaMailSenderImpl();
//		mailSender.setHost("localhost");
//		mailSender.setPort(3025);
//		mailSender.setDefaultEncoding("UTF-8");
//	}
//
//}
