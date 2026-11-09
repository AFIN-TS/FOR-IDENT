package de.hswt.fi.application.regression;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.Peak;
import de.hswt.fi.model.RTICalibrationData;
import de.hswt.fi.model.Score;
import de.hswt.fi.processing.service.model.ProcessCandidate;
import de.hswt.fi.processing.service.model.ProcessingResult;
import de.hswt.fi.search.service.index.model.IndexSearchResult;
import de.hswt.fi.search.service.mass.search.model.Entry;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts results into deterministic JSON snapshots: fixed key order, numbers rounded to
 * {@value #SIGNIFICANT_DIGITS} significant digits, sorted candidate lists and random feature
 * identifiers replaced by stable ones.
 */
final class Snapshots {

	private static final int SIGNIFICANT_DIGITS = 10;

	private static final int PEAKS_IN_SNAPSHOT = 3;

	// Features without identifier get "Target" + random UUID on import
	private static final Pattern RANDOM_TARGET = Pattern.compile(
			"Target[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

	/** Replaces random feature identifiers by stable ones, in order of first appearance. */
	static final class Identifiers {

		private final Map<String, String> stable = new HashMap<>();

		String of(String identifier) {
			if (identifier == null || !RANDOM_TARGET.matcher(identifier).matches()) {
				return identifier;
			}
			return stable.computeIfAbsent(identifier, key -> "Target#" + (stable.size() + 1));
		}
	}

	private Snapshots() {
	}

	static String toJson(Object snapshot) throws JsonProcessingException {
		return MAPPER.writeValueAsString(snapshot) + "\n";
	}

	static Map<String, Object> featureSet(FeatureSet featureSet, Identifiers ids) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("featureCount", featureSet.getFeatures().size());
		snapshot.put("calibration", featureSet.getRtiCalibrationData().stream()
				.map(Snapshots::calibration).collect(Collectors.toList()));
		snapshot.put("features", featureSet.getFeatures().stream()
				.map(feature -> feature(feature, ids)).collect(Collectors.toList()));
		return snapshot;
	}

	/**
	 * Total order of candidates. Feature identifiers are not unique in all files (e.g. two features
	 * "mz 172.13333" with different retention times), and the order of equal candidates depends on
	 * the parallel mass search, so retention time and mass deviation are used as tie-breakers.
	 */
	static Comparator<ProcessCandidate> candidateOrder(Identifiers ids) {
		return Comparator
				.comparing((ProcessCandidate c) -> String.valueOf(ids.of(c.getTargetIdentifier())))
				.thenComparing(c -> c.getEntry() != null ? c.getEntry().getPublicID() : "")
				.thenComparing(c -> c.getRetentionTime(), Comparator.nullsFirst(Comparator.naturalOrder()))
				.thenComparingDouble(c -> c.getMassSearchResult() != null ? c.getMassSearchResult().getDeltaMass() : 0);
	}

	static Map<String, Object> processing(ProcessingResult result, Identifiers ids) {
		List<ProcessCandidate> candidates = new ArrayList<>(result.getResults());
		candidates.sort(candidateOrder(ids));

		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("candidateCount", candidates.size());
		snapshot.put("bestMatches", candidates.stream().filter(ProcessCandidate::isBestMatch).count());
		snapshot.put("candidates", candidates.stream()
				.map(candidate -> candidate(candidate, ids)).collect(Collectors.toList()));
		return snapshot;
	}

	private static Map<String, Object> calibration(RTICalibrationData data) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("identifier", data.getIdentifier());
		snapshot.put("signal", number(data.getSignal()));
		snapshot.put("rti", number(data.getRti()));
		snapshot.put("meanRt", number(data.getMeanRt()));
		snapshot.put("valid", data.isValid());
		return snapshot;
	}

	private static Map<String, Object> feature(Feature feature, Identifiers ids) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("identifier", ids.of(feature.getIdentifier()));
		snapshot.put("precursorMass", number(feature.getPrecursorMass()));
		snapshot.put("neutralFormula", feature.getNeutralFormula());
		snapshot.put("formulaDerivedMass", number(feature.getFormulaDerivedMass()));
		snapshot.put("neutralMass", number(feature.getNeutralMass()));
		snapshot.put("massCalculated", feature.isMassCalculated());
		snapshot.put("retentionTime", number(feature.getRetentionTime()));
		snapshot.put("retentionTimeIndex", number(feature.getRetentionTimeIndex()));
		snapshot.put("retentionTimeSignal", number(feature.getRetentionTimeSignal()));
		snapshot.put("relativeFactor", number(feature.getRelativeFactor()));
		List<Peak> peaks = feature.getPeaks();
		snapshot.put("peakCount", peaks.size());
		snapshot.put("peakMzSum", number(peaks.stream().mapToDouble(Peak::getMz).sum()));
		snapshot.put("peakIntensitySum", number(peaks.stream().mapToDouble(Peak::getIntensity).sum()));
		snapshot.put("firstPeaks", peaks.stream().limit(PEAKS_IN_SNAPSHOT).map(Snapshots::peak)
				.collect(Collectors.toList()));
		return snapshot;
	}

	private static List<Object> peak(Peak peak) {
		List<Object> values = new ArrayList<>();
		values.add(number(peak.getMz()));
		values.add(number(peak.getIntensity()));
		values.add(number(peak.getRelativeIntensity()));
		return values;
	}

	private static Map<String, Object> candidate(ProcessCandidate candidate, Identifiers ids) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		Entry entry = candidate.getEntry();
		snapshot.put("target", ids.of(candidate.getTargetIdentifier()));
		snapshot.put("retentionTime", number(candidate.getRetentionTime()));
		snapshot.put("entry", entry != null ? entry.getPublicID() : null);
		snapshot.put("database", entry != null ? entry.getDatasourceName() : null);
		snapshot.put("name", entry != null && entry.getName() != null ? entry.getName().getValue() : null);
		snapshot.put("bestMatch", candidate.isBestMatch());
		snapshot.put("score", score(candidate.getScore()));
		if (candidate.getMassSearchResult() != null) {
			snapshot.put("deltaMass", number(candidate.getMassSearchResult().getDeltaMass()));
			snapshot.put("massScore", score(candidate.getMassSearchResult().getScore()));
		}
		IndexSearchResult index = candidate.getIndexSearchResult();
		if (index != null) {
			snapshot.put("rti", number(index.getRetentionTimeIndex()));
			snapshot.put("rtiScore", score(index.getScore()));
		}
		if (candidate.getMsMsCandidate() != null) {
			snapshot.put("msmsScore", score(candidate.getMsMsCandidate().getScore()));
			snapshot.put("fragmenterScore", number(candidate.getMsMsCandidate().getFragmenterScore()));
		}
		snapshot.put("massBankScore", score(candidate.getMassBankSimpleScore()));
		return snapshot;
	}

	private static Object score(Score score) {
		if (score == null) {
			return null;
		}
		List<Object> values = new ArrayList<>();
		values.add(number(score.getScoreValue()));
		values.add(number(score.getWeight()));
		values.add(number(score.getWeightedValue()));
		return values;
	}

	/** Rounds to a fixed number of significant digits; NaN and infinity become strings. */
	static Object number(Double value) {
		if (value == null) {
			return null;
		}
		if (value.isNaN() || value.isInfinite()) {
			return value.toString();
		}
		return new BigDecimal(value).round(new MathContext(SIGNIFICANT_DIGITS)).stripTrailingZeros();
	}
}
