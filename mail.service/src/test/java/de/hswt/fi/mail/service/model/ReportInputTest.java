package de.hswt.fi.mail.service.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Server-side checks of the report fields (SP-20, S2). */
class ReportInputTest {

	@Test
	void contactAddressIsOptionalButMustBePlain() {
		assertTrue(ReportInput.isValidOptionalMail(null));
		assertTrue(ReportInput.isValidOptionalMail(""));
		assertTrue(ReportInput.isValidOptionalMail("first.last+tag@sub.example.org"));
		assertFalse(ReportInput.isValidOptionalMail("no-at-sign"));
		assertFalse(ReportInput.isValidOptionalMail("a@b"));
		assertFalse(ReportInput.isValidOptionalMail("a@b.c\r\nBcc: victim@example.org"));
		assertFalse(ReportInput.isValidOptionalMail("a@b.c?bcc=victim@example.org"));
		assertFalse(ReportInput.isValidOptionalMail("\"x\" <a@b.c>"));
		assertFalse(ReportInput.isValidOptionalMail("a@b.c\" onmouseover=\"x"));
		assertFalse(ReportInput.isValidOptionalMail("a,b@c.de"));
		assertFalse(ReportInput.isValidOptionalMail("a".repeat(250) + "@b.de"));
	}

	@Test
	void commentIsCapped() {
		assertTrue(ReportInput.isWithin(null, ReportInput.MAX_COMMENT_LENGTH));
		assertTrue(ReportInput.isWithin("x".repeat(2000), ReportInput.MAX_COMMENT_LENGTH));
		assertFalse(ReportInput.isWithin("x".repeat(2001), ReportInput.MAX_COMMENT_LENGTH));
	}

	@Test
	void replyLinkEncodesSubjectAndCopies() {
		assertNull(ReportInput.replyLink("", "Report:#1", List.of("a@b.de")));
		assertEquals("mailto:v@example.test?subject=Report%3A%23ab12&bcc=a%40b.de%2Cc%40d.de",
				ReportInput.replyLink("v@example.test", "Report:#ab12", List.of("a@b.de", "c@d.de")));
		assertEquals("mailto:v@example.test?subject=Report%20x",
				ReportInput.replyLink("v@example.test", "Report x", null));
	}
}
