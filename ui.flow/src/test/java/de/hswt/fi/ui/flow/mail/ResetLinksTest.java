package de.hswt.fi.ui.flow.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The reset link comes from the configured public address, never from the request (SP-20, S7). */
class ResetLinksTest {

	@Test
	void linkIsBuiltFromThePublicUrl() {
		assertEquals("https://for-ident.example.org/reset-password?token=abc",
				ResetLinks.fromPublicUrl("https://for-ident.example.org", "abc"));
		assertEquals("https://for-ident.example.org/reset-password?token=abc",
				ResetLinks.fromPublicUrl(" https://for-ident.example.org/ ", "abc"));
		assertEquals("http://localhost:8080/app/reset-password?token=abc",
				ResetLinks.fromPublicUrl("http://localhost:8080/app/", "abc"));
	}

	@Test
	void withoutPublicUrlThereIsNoLink() {
		assertNull(ResetLinks.fromPublicUrl(null, "abc"));
		assertNull(ResetLinks.fromPublicUrl("  ", "abc"));
	}

	@Test
	void invalidPublicUrlIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> ResetLinks.fromPublicUrl("for-ident.example.org", "abc"));
		assertThrows(IllegalArgumentException.class, () -> ResetLinks.fromPublicUrl("javascript:alert(1)", "abc"));
		assertThrows(IllegalArgumentException.class, () -> ResetLinks.fromPublicUrl("https://a.example/?x=1", "abc"));
	}
}
