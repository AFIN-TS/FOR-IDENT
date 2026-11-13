package de.hswt.fi.mail.service;

import de.hswt.fi.application.properties.ApplicationProperties;
import de.hswt.fi.mail.service.model.BasicMailTemplate;
import de.hswt.fi.mail.service.model.EntryReportTemplate;
import de.hswt.fi.mail.service.model.MailTemplate;
import de.hswt.fi.mail.service.model.RequestUserAccountNotifyAdminTemplate;
import de.hswt.fi.mail.service.model.SearchHistoryReportTemplate;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.search.service.mass.search.model.SourceList;
import de.hswt.fi.search.service.mass.search.model.properties.NameStringProperty;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Report mails escape the text of the visitor (SP-20, S2) and every template renders, also with missing values
 * (R10). A mail that cannot be built is not sent.
 */
class MailServiceTemplateTest {

	private static final String INJECTION = "<a href=\"https://phish.example\">Login</a><script>x()</script>";

	@Test
	void entryReportEscapesTheVisitorsText() {
		EntryReportTemplate report = new EntryReportTemplate();
		fillVisitorFields(report);
		NameStringProperty name = new NameStringProperty();
		name.setValue("<b>Caffeine</b>");
		report.setEntries(List.of(Entry.EntryBuilder.newBuilder().withPublicID("SI00001").withName(name).build(),
				Entry.EntryBuilder.newBuilder().build()));

		String html = MailService.renderTemplate(MailTemplate.ENTRY_REPORT.getLocation(), Map.of("report", report));

		assertFalse(html.contains("<a href=\"https://phish.example\">"), html);
		assertFalse(html.contains("<script>"), html);
		assertFalse(html.contains("<b>Caffeine</b>"), html);
		assertTrue(html.contains("&lt;a href=&quot;https://phish.example&quot;&gt;Login&lt;/a&gt;"), html);
		assertTrue(html.contains("&lt;b&gt;Caffeine&lt;/b&gt;"), html);
		// The attribute is quoted and its value escaped, so it cannot be closed early
		assertTrue(html.contains("<a href=\"mailto:visitor@example.test?subject=Report%3A%231&amp;bcc="), html);
		assertTrue(html.contains("&quot; onmouseover=&quot;x()\">"), html);
	}

	@Test
	void searchHistoryReportRendersWithDefaultParameters() {
		SearchHistoryReportTemplate<SearchParameter> report = new SearchHistoryReportTemplate<>();
		report.setSearchParameter(new SearchParameter());
		report.setSourceLists(List.of());

		String html = assertDoesNotThrow(() -> MailService.renderTemplate(
				MailTemplate.SEARCH_HISTORY_REPORT.getLocation(), Map.of("report", report)));

		assertTrue(html.contains("Ionisation:"), html);
		assertFalse(html.contains("Index:"), html);
		assertFalse(html.contains("PPM:"), html);
	}

	@Test
	void searchHistoryReportRendersWithIndexConstructor() {
		// The constructor with the index letter leaves ppm, ionisation and halogens unset (null)
		SearchHistoryReportTemplate<SearchParameter> report = new SearchHistoryReportTemplate<>();
		report.setSearchParameter(new SearchParameter('\u0000'));

		String html = assertDoesNotThrow(() -> MailService.renderTemplate(
				MailTemplate.SEARCH_HISTORY_REPORT.getLocation(), Map.of("report", report)));

		assertFalse(html.contains("Index:"), html);
	}

	@Test
	void searchHistoryReportShowsTheParametersEscaped() {
		SearchParameter parameter = new SearchParameter();
		parameter.setIndexChar('C');
		parameter.setCas("58-08-2");
		parameter.setName("<i>caffeine</i>");
		parameter.setAccurateMass(194.080376);
		parameter.setPpm(5.0);
		parameter.setHalogens(Set.of("Cl"));
		parameter.setDatasourceNames(List.of("STOFF-IDENT"));
		SearchHistoryReportTemplate<SearchParameter> report = new SearchHistoryReportTemplate<>();
		fillVisitorFields(report);
		report.setSearchParameter(parameter);
		report.setSourceLists(List.of(new SourceList("<list>", "")));

		String html = MailService.renderTemplate(MailTemplate.SEARCH_HISTORY_REPORT.getLocation(),
				Map.of("report", report));

		assertTrue(html.contains("CAS: 58-08-2"), html);
		assertTrue(html.contains("Name: &lt;i&gt;caffeine&lt;/i&gt;"), html);
		assertTrue(html.contains("Accurate Mass: 194.080376"), html);
		assertTrue(html.contains("PPM: 5"), html);
		assertTrue(html.contains("Halogens: Cl"), html);
		assertTrue(html.contains("Index: C"), html);
		assertTrue(html.contains("<li>STOFF-IDENT</li>"), html);
		assertTrue(html.contains("<li>&lt;list&gt;</li>"), html);
		assertFalse(html.contains("<script>"), html);
	}

	@Test
	void reportWithoutContactAddressHasNoReplyLink() {
		EntryReportTemplate report = new EntryReportTemplate();
		report.setComment("text");

		String html = MailService.renderTemplate(MailTemplate.ENTRY_REPORT.getLocation(), Map.of("report", report));

		assertFalse(html.contains("mailto:"), html);
		assertTrue(html.contains("text"), html);
	}

	@Test
	void accountTemplatesEscapeTheUserName() {
		String admin = MailService.renderTemplate(MailTemplate.USER_ACCOUNT_REQUEST_NOTIFY_ADMIN.getLocation(),
				Map.of("accountRequest", new RequestUserAccountNotifyAdminTemplate("<b>x</b>", "FOR-IDENT")));
		String user = MailService.renderTemplate(MailTemplate.USER_ACCOUNT_REQUEST_NOTIFY_USER.getLocation(),
				Map.of("caption", "FOR-IDENT"));

		assertTrue(admin.contains("&lt;b&gt;x&lt;/b&gt; has requested a new account on FOR-IDENT"), admin);
		assertTrue(user.contains("account for FOR-IDENT"), user);
	}

	@Test
	void templateErrorIsReportedAndNothingIsSent() {
		JavaMailSender sender = mock(JavaMailSender.class);
		MailService service = new MailService(sender, properties("sender@example.test"));

		assertThrows(MailPreparationException.class,
				() -> service.sendTemplateMail("to@example.test", "subject", "missing.ftlh", Map.of()));
		// Model without "report": the template fails instead of sending a half-filled mail
		assertThrows(MailPreparationException.class, () -> service.sendTemplateMail("to@example.test", "subject",
				MailTemplate.ENTRY_REPORT.getLocation(), Map.of()));
		verify(sender, never()).send(any(MimeMessage.class));
	}

	@Test
	void messageThatCannotBeBuiltIsNotSent() {
		JavaMailSender sender = mock(JavaMailSender.class);
		when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
		MailService service = new MailService(sender, properties("not an address <"));

		assertThrows(MailPreparationException.class, () -> service.sendMail("to@example.test", "subject", "text"));
		verify(sender, never()).send(any(MimeMessage.class));
	}

	@Test
	void validMessageIsSent() {
		JavaMailSender sender = mock(JavaMailSender.class);
		when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
		MailService service = new MailService(sender, properties("sender@example.test"));

		service.sendMail("to@example.test", "subject", "text");

		verify(sender).send(any(MimeMessage.class));
	}

	private static void fillVisitorFields(BasicMailTemplate report) {
		report.setUsername("<b>visitor</b>");
		report.setEmail("visitor@example.test");
		report.setOrganisation("Org & Co");
		report.setComment(INJECTION);
		report.setMailTo("mailto:visitor@example.test?subject=Report%3A%231&bcc=\" onmouseover=\"x()");
	}

	private static ApplicationProperties properties(String from) {
		ApplicationProperties properties = new ApplicationProperties();
		properties.setFromEmailAddress(from);
		return properties;
	}
}
