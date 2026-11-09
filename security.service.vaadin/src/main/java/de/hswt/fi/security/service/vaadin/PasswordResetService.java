package de.hswt.fi.security.service.vaadin;

import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.api.repositories.RegisteredUserRepository;
import de.hswt.fi.security.service.model.RegisteredUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Password reset by link (SP-11). Replaces the reset of the Vaadin 8 UI, which set a new password at once for any
 * entered e-mail address.
 * <p>
 * The token contains the user name and the expiry time, signed with HMAC-SHA256 over both and the current password
 * hash. Nothing is stored: the token expires after {@link #VALIDITY}, and it can be used only once because the
 * password hash changes with the new password. The key comes from {@code de.hswt.fi.security.reset-token-secret};
 * without it a random key is created at startup (open links then become invalid on a restart).
 */
@Service
public class PasswordResetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);

	public static final Duration VALIDITY = Duration.ofHours(1);

	private static final String HMAC = "HmacSHA256";

	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private final RegisteredUserRepository userRepository;

	private final SecurityService securityService;

	private final byte[] key;

	private final Clock clock;

	@Autowired
	public PasswordResetService(RegisteredUserRepository userRepository, SecurityService securityService,
								@Value("${de.hswt.fi.security.reset-token-secret:}") String secret) {
		this(userRepository, securityService, secret, Clock.systemUTC());
	}

	PasswordResetService(RegisteredUserRepository userRepository, SecurityService securityService, String secret,
						 Clock clock) {
		this.userRepository = userRepository;
		this.securityService = securityService;
		this.clock = clock;
		if (secret == null || secret.isBlank()) {
			key = new byte[32];
			new SecureRandom().nextBytes(key);
		} else {
			key = secret.getBytes(StandardCharsets.UTF_8);
		}
	}

	/**
	 * Creates a reset token for the enabled account with the given e-mail address; empty if there is none.
	 */
	public Optional<String> createToken(String mail) {
		if (mail == null || mail.isBlank()) {
			return Optional.empty();
		}
		return userRepository.findByMail(mail.trim().toLowerCase(Locale.ROOT))
				.filter(RegisteredUser::isEnabled)
				.map(user -> {
					long expiry = clock.instant().plus(VALIDITY).getEpochSecond();
					String username = ENCODER.encodeToString(user.getUsername().getBytes(StandardCharsets.UTF_8));
					return username + "." + expiry + "." + ENCODER.encodeToString(sign(user, expiry));
				});
	}

	/**
	 * The account the token belongs to, if the token is valid, not expired and not used yet.
	 */
	public Optional<RegisteredUser> validate(String token) {
		if (token == null) {
			return Optional.empty();
		}
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			return Optional.empty();
		}
		try {
			String username = new String(DECODER.decode(parts[0]), StandardCharsets.UTF_8);
			long expiry = Long.parseLong(parts[1]);
			byte[] signature = DECODER.decode(parts[2]);
			if (clock.instant().getEpochSecond() > expiry) {
				return Optional.empty();
			}
			return userRepository.findByUsername(username)
					.filter(RegisteredUser::isEnabled)
					.filter(user -> MessageDigest.isEqual(sign(user, expiry), signature));
		} catch (IllegalArgumentException e) {
			LOGGER.debug("Invalid password reset token: {}", e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Sets the new password if the token is valid; afterwards the token is no longer valid.
	 */
	public boolean resetPassword(String token, String newPassword) {
		Optional<RegisteredUser> user = validate(token);
		if (user.isEmpty() || newPassword == null || newPassword.isEmpty()) {
			return false;
		}
		boolean success = securityService.resetPasswordTo(user.get().getMail(), newPassword);
		LOGGER.info("Password reset by link for user id {}: {}", user.get().getId(), success ? "done" : "failed");
		return success;
	}

	private byte[] sign(RegisteredUser user, long expiry) {
		try {
			Mac mac = Mac.getInstance(HMAC);
			mac.init(new SecretKeySpec(key, HMAC));
			String data = user.getUsername() + "\n" + expiry + "\n" + user.getPassword();
			return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("HMAC-SHA256 not available", e);
		}
	}
}
