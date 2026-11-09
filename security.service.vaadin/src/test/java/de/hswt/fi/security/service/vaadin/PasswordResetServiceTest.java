package de.hswt.fi.security.service.vaadin;

import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.api.repositories.RegisteredUserRepository;
import de.hswt.fi.security.service.model.RegisteredUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PasswordResetServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

	private RegisteredUserRepository repository;

	private SecurityService securityService;

	private RegisteredUser user;

	@BeforeEach
	public void setUp() {
		repository = mock(RegisteredUserRepository.class);
		securityService = mock(SecurityService.class);
		user = new RegisteredUser("user@example.org", "$2a$10$oldhash");
		user.setMail("user@example.org");
		user.setEnabled(true);
		when(repository.findByMail("user@example.org")).thenReturn(Optional.of(user));
		when(repository.findByUsername("user@example.org")).thenReturn(Optional.of(user));
	}

	private PasswordResetService service(Instant now) {
		return new PasswordResetService(repository, securityService, "test-secret", Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	public void validTokenIdentifiesTheUser() {
		String token = service(NOW).createToken(" User@Example.org ").orElseThrow();
		assertEquals(Optional.of(user), service(NOW.plusSeconds(60)).validate(token));
	}

	@Test
	public void tokenExpiresAfterOneHour() {
		String token = service(NOW).createToken("user@example.org").orElseThrow();
		assertTrue(service(NOW.plus(Duration.ofMinutes(59))).validate(token).isPresent());
		assertFalse(service(NOW.plus(Duration.ofMinutes(61))).validate(token).isPresent());
	}

	@Test
	public void tokenIsInvalidOnceThePasswordChanged() {
		String token = service(NOW).createToken("user@example.org").orElseThrow();
		user.setPassword("$2a$10$newhash");
		assertFalse(service(NOW).validate(token).isPresent());
	}

	@Test
	public void manipulatedOrForeignTokensAreRejected() {
		String token = service(NOW).createToken("user@example.org").orElseThrow();
		String[] parts = token.split("\\.");
		// Later expiry with the old signature
		assertFalse(service(NOW).validate(parts[0] + "." + (Long.parseLong(parts[1]) + 3600) + "." + parts[2]).isPresent());
		// Token signed with another key
		String foreign = new PasswordResetService(repository, securityService, "other-secret",
				Clock.fixed(NOW, ZoneOffset.UTC)).createToken("user@example.org").orElseThrow();
		assertFalse(service(NOW).validate(foreign).isPresent());
		assertFalse(service(NOW).validate("garbage").isPresent());
		assertFalse(service(NOW).validate("a.b.c").isPresent());
		assertFalse(service(NOW).validate(null).isPresent());
	}

	@Test
	public void noTokenForUnknownOrDisabledAccounts() {
		assertFalse(service(NOW).createToken("nobody@example.org").isPresent());
		String token = service(NOW).createToken("user@example.org").orElseThrow();
		user.setEnabled(false);
		assertFalse(service(NOW).createToken("user@example.org").isPresent());
		assertFalse(service(NOW).validate(token).isPresent());
	}

	@Test
	public void resetPasswordOnlyWithValidToken() {
		when(securityService.resetPasswordTo("user@example.org", "new-password")).thenReturn(true);
		String token = service(NOW).createToken("user@example.org").orElseThrow();
		assertFalse(service(NOW.plus(Duration.ofHours(2))).resetPassword(token, "new-password"));
		verify(securityService, never()).resetPasswordTo(anyString(), anyString());
		assertTrue(service(NOW).resetPassword(token, "new-password"));
		verify(securityService).resetPasswordTo("user@example.org", "new-password");
	}
}
