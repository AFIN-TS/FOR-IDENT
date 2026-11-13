package de.hswt.fi.mail.service.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Server-side limits and checks for the free-text fields of a report (SP-20, review finding S2). The report dialog is
 * open to anonymous visitors, so its input is checked on the server, not only in the browser.
 */
public final class ReportInput {

	public static final int MAX_COMMENT_LENGTH = 2000;

	public static final int MAX_NAME_LENGTH = 100;

	public static final int MAX_ORGANISATION_LENGTH = 200;

	/** RFC 5321 limit of a forward path. */
	public static final int MAX_MAIL_LENGTH = 254;

	/**
	 * A plain address local@domain.tld: no white space, line breaks, quotes, angle brackets or separators, so the
	 * value cannot add header lines, recipients or markup.
	 */
	private static final String PLAIN = "[^\\s@<>()\\[\\]\\\\,;:\"'?&=%/]";

	/** Top-level domain: as {@link #PLAIN}, without dots. */
	private static final String PLAIN_NO_DOT = PLAIN.substring(0, PLAIN.length() - 1) + ".]";

	private static final Pattern MAIL = Pattern.compile(PLAIN + "+@" + PLAIN + "+\\." + PLAIN_NO_DOT + "+");

	private ReportInput() {
	}

	/** {@code true} for an empty value (the contact address is optional) or a plain address. */
	public static boolean isValidOptionalMail(String mail) {
		return mail == null || mail.isEmpty() || (mail.length() <= MAX_MAIL_LENGTH && MAIL.matcher(mail).matches());
	}

	public static boolean isWithin(String value, int maxLength) {
		return value == null || value.length() <= maxLength;
	}

	/**
	 * The reply link of the report mail, or {@code null} without a contact address. The address must have been
	 * checked with {@link #isValidOptionalMail}; subject and copy addresses are URL-encoded.
	 */
	public static String replyLink(String mail, String subject, List<String> bcc) {
		if (mail == null || mail.isEmpty()) {
			return null;
		}
		StringBuilder link = new StringBuilder("mailto:").append(mail).append("?subject=").append(encode(subject));
		if (bcc != null && !bcc.isEmpty()) {
			link.append("&bcc=").append(encode(String.join(",", bcc)));
		}
		return link.toString();
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
