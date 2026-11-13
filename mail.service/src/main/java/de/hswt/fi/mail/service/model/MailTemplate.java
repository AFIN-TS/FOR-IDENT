package de.hswt.fi.mail.service.model;

public enum MailTemplate {

	SEARCH_HISTORY_REPORT("search_history_report.ftlh"),
	PROCESSING_HISTORY_REPORT("processing_history_report.ftlh"),
	ENTRY_REPORT("entry_report.ftlh"),
	USER_ACCOUNT_REQUEST_NOTIFY_ADMIN("user_account_request_notify_admin.ftlh"),
	USER_ACCOUNT_REQUEST_NOTIFY_USER("user_account_request_notify_user.ftlh");

	MailTemplate(String templateLocation) {
		this.templateLocation = templateLocation;
	}

	private final String templateLocation;

	public String getLocation() {
		return templateLocation;
	}

}
