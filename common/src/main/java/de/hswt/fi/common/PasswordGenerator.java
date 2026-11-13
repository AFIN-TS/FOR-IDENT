package de.hswt.fi.common;

import org.apache.commons.text.RandomStringGenerator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.function.IntUnaryOperator;

/**
 * Random passwords of letters and digits (admin user dialog). Drawn from a {@link SecureRandom}; without
 * {@code usingRandom} commons-text would use a non-cryptographic generator (SP-20, review finding S8).
 */
@Component
@Scope("prototype")
public class PasswordGenerator {

	private final RandomStringGenerator randomStringGenerator;

	public PasswordGenerator() {
		SecureRandom secureRandom = new SecureRandom();
		IntUnaryOperator random = secureRandom::nextInt;
		randomStringGenerator = new RandomStringGenerator.Builder()
				.withinRange('0', 'z')
				.filteredBy(Character::isLetter, Character::isDigit)
				.usingRandom(random)
				.build();
	}

	public String generatePassword(int length) {
		return randomStringGenerator.generate(length);
	}
}
