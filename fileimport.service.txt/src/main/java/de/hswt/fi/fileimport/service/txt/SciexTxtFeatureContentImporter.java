package de.hswt.fi.fileimport.service.txt;

import de.hswt.fi.common.FileUtil;
import de.hswt.fi.fileimport.service.api.FileBasedFeatureContentImporter;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.Peak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text format with one block per feature, as written by the SCIEX OS / MasterView report templates, by Compound
 * Discoverer scripts and similar tools (MSP-like):
 *
 * <pre>
 * NAME: 144.982166 / 18.12
 * RETENTIONTIME: 18.12
 * PRECURSORMZ: 144.9822
 * Formula: C3HF4P
 * 50.1992 3869 57.9352 6644 ...
 * //
 * </pre>
 *
 * <p>The lines are recognised by their keyword (SP-15; until then strictly by position): the Formula line is optional
 * (SCIEX template without formula), other keywords are ignored, and the peaks (pairs of m/z and intensity) may stand
 * in one line or in several, separated by blanks or tabs, also with a decimal comma. Peaks written on the Formula or
 * PRECURSORMZ line (some report templates put the peak list there) are read as peaks. A block without name,
 * retention time or precursor m/z is skipped instead of failing the whole file. Formula texts of the instrument
 * software such as "No data for Formula Finder ...", "No formula found" or "Error" mean: no formula.
 */
@Component
@Scope("prototype")
public class SciexTxtFeatureContentImporter implements FileBasedFeatureContentImporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(SciexTxtFeatureContentImporter.class);

	private static final String END_OF_BLOCK = "//";

	/** "KEY: value"; the key starts with a letter, so peak lines never match. */
	private static final Pattern KEY_VALUE = Pattern.compile("^\\s*([A-Za-z][A-Za-z _#]*?)\\s*:(.*)$");

	private static final Pattern NUMBERS_ONLY = Pattern.compile("^[\\s\\d.,;eE+-]+$");

	@Override
	public FeatureSet importFromFile(Path contentPath) {
		List<String> lines = FileUtil.readAllLines(contentPath);
		List<Feature> features = new ArrayList<>();
		if (lines == null) {
			return new FeatureSet(contentPath, features);
		}

		List<String> block = new ArrayList<>();
		int blockNumber = 0;
		for (String line : lines) {
			if (line.trim().equals(END_OF_BLOCK)) {
				blockNumber++;
				addFeature(block, blockNumber, features);
				block.clear();
			} else if (!line.trim().isEmpty()) {
				block.add(line);
			}
		}
		// A last block without the end marker
		if (!block.isEmpty()) {
			addFeature(block, blockNumber + 1, features);
		}
		return new FeatureSet(contentPath, features);
	}

	@Override
	public boolean canHandle(Path path) {
		return path.getFileName().toString().endsWith(".txt");
	}

	private void addFeature(List<String> block, int blockNumber, List<Feature> features) {
		Feature feature = parseFeature(block, blockNumber);
		if (feature != null) {
			features.add(feature);
		}
	}

	/** The feature of a block, or null if name, retention time or precursor m/z are missing or not readable. */
	Feature parseFeature(List<String> block, int blockNumber) {
		String name = null;
		Double retentionTime = null;
		Double precursorMass = null;
		String formula = null;
		List<Peak> peaks = new ArrayList<>();
		boolean noPeaks = false;

		for (String line : block) {
			Matcher keyValue = KEY_VALUE.matcher(line);
			if (!keyValue.matches()) {
				// Peak line; "N/A" means no peaks
				if (line.contains("N/A")) {
					noPeaks = true;
				} else {
					addPeaks(line, peaks, blockNumber);
				}
				continue;
			}
			String key = keyValue.group(1).trim().toUpperCase(Locale.ROOT);
			String value = keyValue.group(2).trim();
			switch (key) {
				case "NAME" -> name = value;
				case "RETENTIONTIME" -> retentionTime = parseNumber(value);
				case "PRECURSORMZ" -> {
					// The mass, possibly followed by the peak list
					String[] tokens = value.split("\\s+", 2);
					precursorMass = parseNumber(tokens[0]);
					if (tokens.length > 1) {
						addPeaks(tokens[1], peaks, blockNumber);
					}
				}
				case "FORMULA" -> {
					if (looksLikePeaks(value)) {
						addPeaks(value, peaks, blockNumber);
					} else if (value.contains("N/A")) {
						noPeaks = true;
					} else if (!isNoFormula(value)) {
						formula = value;
					}
				}
				default -> {
					// Other keywords of MSP-like files (e.g. "Num Peaks", "PRECURSORTYPE") are not needed
				}
			}
		}

		if (name == null || name.isEmpty() || retentionTime == null || precursorMass == null) {
			LOGGER.warn("entry {} skipped: name, retention time or precursor m/z missing or not readable", blockNumber);
			return null;
		}
		return new Feature.Builder(name, precursorMass)
				.withRetentionTime(retentionTime)
				.withNeutralFormula(formula)
				.withPeaks(noPeaks && peaks.isEmpty() ? new ArrayList<>() : peaks)
				.build();
	}

	/** Formula texts of the instrument software that mean "no formula" (as before). */
	static boolean isNoFormula(String value) {
		return value.contains("No data") || value.contains("Error") || value.contains("No formula");
	}

	/** True for a value that consists of at least one pair of numbers, e.g. a peak list in the Formula line. */
	static boolean looksLikePeaks(String value) {
		return !value.isEmpty() && NUMBERS_ONLY.matcher(value).matches() && value.trim().split("[\\s;]+").length >= 2;
	}

	/** Adds the pairs of m/z and intensity of a line; an incomplete last pair or unreadable numbers are skipped. */
	static void addPeaks(String line, List<Peak> peaks, int blockNumber) {
		String[] tokens = line.trim().split("[\\s;]+");
		if (tokens.length == 1 && tokens[0].isEmpty()) {
			return;
		}
		if (tokens.length % 2 != 0) {
			LOGGER.warn("entry {}: odd number of peak values, last value ignored", blockNumber);
		}
		for (int i = 0; i + 1 < tokens.length; i += 2) {
			Double mz = parseNumber(tokens[i]);
			Double intensity = parseNumber(tokens[i + 1]);
			if (mz == null || intensity == null) {
				LOGGER.warn("entry {}: peak '{} {}' not readable, skipped", blockNumber, tokens[i], tokens[i + 1]);
				continue;
			}
			peaks.add(new Peak(mz, intensity));
		}
	}

	/** Number with decimal point or, if there is no point, decimal comma; null if not readable. */
	static Double parseNumber(String value) {
		if (value == null) {
			return null;
		}
		String number = value.trim();
		if (number.indexOf('.') < 0) {
			number = number.replace(',', '.');
		}
		try {
			return Double.parseDouble(number);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
