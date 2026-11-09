package de.hswt.fi.common;

/**
 * The Class SearchUtil is used to calculate the search range for masses out of
 * the ppm value. It also provides the proton mass for the ion species +H/-H.
 * 
 * @author Marco Luthardt
 */
public final class SearchUtil {

	private SearchUtil() {
		throw new IllegalStateException("Static utility class should not be initialized");
	}

	/**
	 * Mass of the proton in u (CODATA 2018), added for [M+H]+ and removed for [M-H]-. Up to 2.x/3.0.0 the mass of the
	 * hydrogen atom (1.0078) was used, which shifted the neutral mass by 0.52 mDa (topic T6).
	 */
	static final double PROTON_MASS = 1.007276466621;

	/**
	 * Returns the +/- range value based on the mass and ppm. The value must be
	 * added and subtracted to get the search range.
	 *
	 * @param mass
	 *            the mass to search for
	 * @param ppm
	 *            the ppm value
	 * @return the range from ppm
	 */
	public static synchronized double getRangeFromPPM(double mass, double ppm) {
		return Math.abs(mass * ppm / 1000000.0);
	}

	/**
	 * Returns the mass value of the ionisation (mass of +H, -H or 0).
	 *
	 * @param ionisation
	 *            the ionisation id or constant
	 * @return the ionisation mass value, which is +H, -H or 0, based on the
	 *         given ionisation constant
	 */
	public static synchronized double getIonisation(Ionisation ionisation) {

		if (ionisation == null) {
			return 0.0;
		}
		switch (ionisation) {
		case POSITIVE_IONISATION:
			return -PROTON_MASS;
		case NEGATIVE_IONISATION:
			return PROTON_MASS;
		default:
			return 0.0;
		}
	}
}
