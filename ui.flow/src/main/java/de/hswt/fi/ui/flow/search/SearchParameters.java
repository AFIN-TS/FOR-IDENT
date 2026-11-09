package de.hswt.fi.ui.flow.search;

import de.hswt.fi.common.RangeParser;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Parsing and description of search parameters (TextFieldUtils and ReportUtil of the Vaadin 8 UI).
 */
public final class SearchParameters {

	private SearchParameters() {
	}

	/**
	 * Sets the monoisotopic mass: a single value (searched with the ppm tolerance) or a range ("188-189", ">500",
	 * "<100"); empty clears it. Returns false for an invalid input.
	 */
	public static boolean setMass(SearchParameter parameter, String input, double ppm) {
		parameter.setAccurateMass(null);
		parameter.setAccurateMassRangeMin(Double.MIN_VALUE);
		parameter.setAccurateMassRangeMax(Double.MAX_VALUE);
		parameter.setPpm(0.0);
		String value = input == null ? "" : input.trim();
		if (value.isEmpty()) {
			return true;
		}
		RangeParser range = new RangeParser();
		if (range.parse(value)) {
			parameter.setAccurateMassRangeMin(range.getMin());
			parameter.setAccurateMassRangeMax(range.getMax());
			return true;
		}
		Double number = parseNumber(value);
		if (number == null) {
			return false;
		}
		parameter.setAccurateMass(number);
		parameter.setPpm(ppm);
		return true;
	}

	/** Sets logP: a single value (searched with the delta) or a range; empty clears it. */
	public static boolean setLogP(SearchParameter parameter, String input, double delta) {
		parameter.setLogP(null);
		parameter.setLogPRangeMin(Double.MIN_VALUE);
		parameter.setLogPRangeMax(Double.MAX_VALUE);
		parameter.setLogPDelta(0.0);
		String value = input == null ? "" : input.trim();
		if (value.isEmpty()) {
			return true;
		}
		RangeParser range = new RangeParser();
		if (range.parse(value)) {
			parameter.setLogPRangeMin(range.getMin());
			parameter.setLogPRangeMax(range.getMax());
			return true;
		}
		Double number = parseNumber(value);
		if (number == null) {
			return false;
		}
		parameter.setLogP(number);
		parameter.setLogPDelta(delta);
		return true;
	}

	/** A number with "." or "," as decimal separator. */
	static Double parseNumber(String value) {
		try {
			return Double.valueOf(value.replace(',', '.'));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** True if no field that restricts the search is set (the Vaadin 8 UI ignored ranges here). */
	public static boolean isEmpty(SearchParameter parameter) {
		return parameter.getIndexChar() == ' '
				&& isBlank(parameter.getName()) && isBlank(parameter.getCas())
				&& isBlank(parameter.getElementalFormula()) && isBlank(parameter.getIupac())
				&& isBlank(parameter.getSmiles()) && isBlank(parameter.getPublicID())
				&& isBlank(parameter.getInchiKey())
				&& parameter.getAccurateMass() == null && !isRange(parameter.getAccurateMassRangeMin(), parameter.getAccurateMassRangeMax())
				&& parameter.getLogP() == null && !isRange(parameter.getLogPRangeMin(), parameter.getLogPRangeMax())
				&& parameter.getLogD() == null
				&& (parameter.getHalogens() == null || parameter.getHalogens().isEmpty());
	}

	static boolean isRange(Double min, Double max) {
		return min != null && max != null
				&& (Double.compare(min, Double.MIN_VALUE) != 0 || Double.compare(max, Double.MAX_VALUE) != 0);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	/**
	 * Short description of the set parameters, e.g. "Name: phenazone; Mass: 189.1022 ± 5 ppm (+H)", for the history
	 * and the report. {@code caption} translates the keys {@code ui.search.*}.
	 */
	public static String describe(SearchParameter parameter, Function<String, String> caption) {
		List<String> parts = new ArrayList<>();
		if (parameter.getIndexChar() != ' ') {
			parts.add(caption.apply("ui.search.history.index") + ": "
					+ Character.toUpperCase(parameter.getIndexChar()));
		}
		add(parts, caption.apply("ui.search.name"), parameter.getName());
		add(parts, caption.apply("ui.search.cas"), parameter.getCas());
		add(parts, caption.apply("ui.search.formula"), parameter.getElementalFormula());
		add(parts, caption.apply("ui.search.inchiKey"), parameter.getInchiKey());
		add(parts, caption.apply("ui.search.iupac"), parameter.getIupac());
		add(parts, caption.apply("ui.search.smiles"), parameter.getSmiles());
		add(parts, caption.apply("ui.search.publicId"), parameter.getPublicID());
		String ionisation = parameter.getIonisation() == null ? "" : " (" + parameter.getIonisation() + ")";
		if (parameter.getAccurateMass() != null) {
			parts.add(caption.apply("ui.search.mass") + ": " + format(parameter.getAccurateMass()) + " ± "
					+ format(parameter.getPpm()) + " ppm" + ionisation);
		} else if (isRange(parameter.getAccurateMassRangeMin(), parameter.getAccurateMassRangeMax())) {
			parts.add(caption.apply("ui.search.mass") + ": " + range(parameter.getAccurateMassRangeMin(),
					parameter.getAccurateMassRangeMax()) + ionisation);
		}
		if (parameter.getLogP() != null) {
			parts.add(caption.apply("ui.search.logP") + ": " + format(parameter.getLogP()) + " ± "
					+ format(parameter.getLogPDelta()));
		} else if (isRange(parameter.getLogPRangeMin(), parameter.getLogPRangeMax())) {
			parts.add(caption.apply("ui.search.logP") + ": " + range(parameter.getLogPRangeMin(), parameter.getLogPRangeMax()));
		}
		Set<String> halogens = parameter.getHalogens();
		if (halogens != null && !halogens.isEmpty()) {
			parts.add(caption.apply("ui.search.halogens") + ": " + String.join(", ", halogens.stream().sorted().toList()));
		}
		return String.join("; ", parts);
	}

	private static void add(List<String> parts, String caption, String value) {
		if (!isBlank(value)) {
			parts.add(caption + ": " + value.trim());
		}
	}

	private static String range(Double min, Double max) {
		// ">x" is stored by RangeParser with an upper bound near Double.MAX_VALUE
		if (max > 1e300) {
			return "> " + format(min);
		}
		return format(min) + " - " + format(max);
	}

	static String format(Double value) {
		if (value == null) {
			return "";
		}
		String text = String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "");
		return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
	}
}
