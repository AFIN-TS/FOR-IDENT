package de.hswt.fi.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ion species +H/-H use the proton mass, not the mass of the hydrogen atom (topic T6).
 */
class SearchUtilTest {

	/** Monoisotopic mass of phenazone C11H12N2O. */
	private static final double PHENAZONE = 188.094963;

	@Test
	void ionisationUsesProtonMass() {
		assertEquals(-1.007276, SearchUtil.getIonisation(Ionisation.POSITIVE_IONISATION), 1e-6);
		assertEquals(1.007276, SearchUtil.getIonisation(Ionisation.NEGATIVE_IONISATION), 1e-6);
		assertEquals(0.0, SearchUtil.getIonisation(Ionisation.NEUTRAL_IONISATION));
		assertEquals(0.0, SearchUtil.getIonisation(null));
		assertEquals(SearchUtil.getIonisation(Ionisation.POSITIVE_IONISATION),
				Ionisation.POSITIVE_IONISATION.getIonisation());
		assertEquals(SearchUtil.getIonisation(Ionisation.NEGATIVE_IONISATION),
				Ionisation.NEGATIVE_IONISATION.getIonisation());
	}

	@Test
	void phenazoneProtonatedIsFoundWithOnePpm() {
		// Example from T6: [M+H]+ 189.1022 with 1 ppm gave no hit with the hydrogen atom mass
		double measured = 189.1022;
		double neutral = measured + SearchUtil.getIonisation(Ionisation.POSITIVE_IONISATION);
		assertTrue(Math.abs(neutral - PHENAZONE) <= SearchUtil.getRangeFromPPM(measured, 1.0));
	}
}
