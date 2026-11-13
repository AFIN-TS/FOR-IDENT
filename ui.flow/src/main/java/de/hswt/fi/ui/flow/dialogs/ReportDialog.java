package de.hswt.fi.ui.flow.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldBase;
import de.hswt.fi.mail.service.model.BasicMailTemplate;
import de.hswt.fi.mail.service.model.EntryReportTemplate;
import de.hswt.fi.mail.service.model.MailTemplate;
import de.hswt.fi.mail.service.model.ReportInput;
import de.hswt.fi.mail.service.model.SearchHistoryReportTemplate;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.security.service.model.RegisteredUser;
import de.hswt.fi.ui.flow.search.SearchViewServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Report of faulty records or a faulty search to the contact addresses of the application (ReportWindow and the report
 * handlers of the Vaadin 8 UI, same mail templates). Needs a mail server (topic T5): if the mail cannot be sent, the
 * user gets an error message.
 */
public class ReportDialog extends Dialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportDialog.class);

	private final transient SearchViewServices services;

	private final TextField name = new TextField();

	private final EmailField mail = new EmailField();

	private final TextField organisation = new TextField();

	private final TextArea comment = new TextArea();

	private final MailTemplate template;

	private final transient BasicMailTemplate report;

	private final String subject;

	private ReportDialog(SearchViewServices services, String description, MailTemplate template,
						 BasicMailTemplate report, String subject) {
		this.services = services;
		this.template = template;
		this.report = report;
		this.subject = subject;
		setHeaderTitle(getTranslation("ui.report.title"));
		setWidth("min(40rem, 95vw)");

		TextArea data = new TextArea(getTranslation("ui.report.data"));
		data.setValue(description);
		data.setReadOnly(true);
		data.setWidthFull();
		data.setMaxHeight("14rem");
		name.setLabel(getTranslation("ui.user.username"));
		mail.setLabel(getTranslation("ui.user.email"));
		organisation.setLabel(getTranslation("ui.user.organisation"));
		comment.setLabel(getTranslation("ui.report.comment"));
		comment.setWidthFull();
		comment.setMinHeight("6rem");
		name.setMaxLength(ReportInput.MAX_NAME_LENGTH);
		mail.setMaxLength(ReportInput.MAX_MAIL_LENGTH);
		organisation.setMaxLength(ReportInput.MAX_ORGANISATION_LENGTH);
		comment.setMaxLength(ReportInput.MAX_COMMENT_LENGTH);
		RegisteredUser user = services.security().getCurrentUser();
		if (user != null) {
			name.setValue(nullToEmpty(user.getUsername()));
			mail.setValue(nullToEmpty(user.getMail()));
			organisation.setValue(nullToEmpty(user.getOrganisation()));
		}
		FormLayout form = new FormLayout(name, mail, organisation);
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("30rem", 3));
		add(data, form, comment);

		Button send = new Button(getTranslation("ui.report.send"), e -> send());
		send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button cancel = new Button(getTranslation("ui.button.cancel"), e -> close());
		getFooter().add(cancel, send);
	}

	/** Report of one or more records (results list or details). */
	public static ReportDialog forEntries(SearchViewServices services, List<Entry> entries) {
		EntryReportTemplate report = new EntryReportTemplate();
		report.setEntries(entries);
		String description = entries.stream()
				.map(entry -> nullToEmpty(entry.getPublicID()) + "  " + (entry.getName() == null ? "" : entry.getName().getValue()))
				.collect(Collectors.joining("\n"));
		String kind = entries.size() == 1 ? "Search Record" : "Search Results";
		return new ReportDialog(services, description, MailTemplate.ENTRY_REPORT, report, kind);
	}

	/** Report of a search (search history). */
	public static ReportDialog forSearch(SearchViewServices services, SearchParameter parameter, String description) {
		SearchHistoryReportTemplate<SearchParameter> report = new SearchHistoryReportTemplate<>();
		report.setSearchParameter(parameter);
		return new ReportDialog(services, description, MailTemplate.SEARCH_HISTORY_REPORT, report, "Search History");
	}

	private void send() {
		// Without contact addresses (FI_CONTACT_ADDRESSES) the report would go nowhere; the Vaadin 8 UI still said "sent"
		if (services.properties().getContactAdresses() == null || services.properties().getContactAdresses().isEmpty()) {
			LOGGER.warn("Report not sent: no contact addresses configured (FI_CONTACT_ADDRESSES)");
			UserFields.showError(getTranslation("ui.report.notConfigured"));
			return;
		}
		// Checked on the server as well, the dialog is open to anonymous visitors (SP-20, S2); the templates escape
		// every value, so the text arrives as plain text
		String mailValue = mail.getValue() == null ? "" : mail.getValue().trim();
		if (!ReportInput.isValidOptionalMail(mailValue)) {
			mail.setInvalid(true);
			mail.setErrorMessage(getTranslation("ui.validation.emailInvalid"));
			return;
		}
		if (!checkLength(name, ReportInput.MAX_NAME_LENGTH)
				|| !checkLength(organisation, ReportInput.MAX_ORGANISATION_LENGTH)
				|| !checkLength(comment, ReportInput.MAX_COMMENT_LENGTH)) {
			return;
		}
		String errorNumber = "#" + UUID.randomUUID().toString().substring(0, 8);
		report.setUsername(name.getValue());
		report.setEmail(mailValue);
		report.setOrganisation(organisation.getValue());
		report.setComment(comment.getValue());
		report.setMailTo(ReportInput.replyLink(mailValue, "Report:" + errorNumber,
				services.properties().getContactAdresses()));
		if (report.getSourceLists() == null) {
			report.setSourceLists(List.of());
		}
		try {
			for (String address : services.properties().getContactAdresses()) {
				services.mail().sendTemplateMail(address, "Bug report [" + subject + " " + errorNumber + "]",
						template.getLocation(), Map.of("report", report));
			}
		} catch (RuntimeException e) {
			LOGGER.error("Could not send the report {}: {}", errorNumber, e.getMessage());
			UserFields.showError(getTranslation("ui.report.error"));
			return;
		}
		close();
		UserFields.showSuccess(getTranslation("ui.report.sent"));
	}

	private boolean checkLength(TextFieldBase<?, String> field, int maxLength) {
		if (ReportInput.isWithin(field.getValue(), maxLength)) {
			return true;
		}
		field.setInvalid(true);
		field.setErrorMessage(getTranslation("ui.validation.maxLength", maxLength));
		return false;
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
