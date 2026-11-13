package de.hswt.fi.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Generated passwords: requested length, letters and digits only, no repeats (SP-20, S8). */
class PasswordGeneratorTest {

	@Test
	void generatesLettersAndDigitsOfTheRequestedLength() {
		PasswordGenerator generator = new PasswordGenerator();
		Set<String> passwords = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			String password = generator.generatePassword(16);
			assertEquals(16, password.length());
			assertTrue(password.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')
					|| (c >= 'a' && c <= 'z')), password);
			passwords.add(password);
		}
		assertEquals(200, passwords.size());
	}
}
