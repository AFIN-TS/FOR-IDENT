package de.hswt.fi.application;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The initial admin needs a password and a mail address from the environment (SP-20, S4 and S8). */
class ApplicationStartupListenerTest {

	@Test
	void passwordAndMailAreTakenFromTheEnvironment() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(ApplicationStartupListener.ADMIN_PASSWORD_PROPERTY, "secret-for-test")
				.withProperty(ApplicationStartupListener.ADMIN_MAIL_PROPERTY, " admin@example.test ");

		assertEquals("secret-for-test", ApplicationStartupListener.getAdminPassword(environment));
		assertEquals("admin@example.test", ApplicationStartupListener.getAdminMail(environment));
	}

	@Test
	void missingPasswordStopsTheInitialization() {
		assertThrows(IllegalStateException.class,
				() -> ApplicationStartupListener.getAdminPassword(new MockEnvironment()));
		assertThrows(IllegalStateException.class, () -> ApplicationStartupListener.getAdminPassword(
				new MockEnvironment().withProperty(ApplicationStartupListener.ADMIN_PASSWORD_PROPERTY, " ")));
	}

	@Test
	void missingOrInvalidMailStopsTheInitialization() {
		assertThrows(IllegalStateException.class,
				() -> ApplicationStartupListener.getAdminMail(new MockEnvironment()));
		assertThrows(IllegalStateException.class, () -> ApplicationStartupListener.getAdminMail(
				new MockEnvironment().withProperty(ApplicationStartupListener.ADMIN_MAIL_PROPERTY, "admin")));
	}
}
