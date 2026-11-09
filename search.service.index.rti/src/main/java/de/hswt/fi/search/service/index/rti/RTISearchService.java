package de.hswt.fi.search.service.index.rti;

import de.hswt.fi.common.Ionisation;
import de.hswt.fi.common.StationaryPhase;
import de.hswt.fi.common.spring.Profiles;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.Score;
import de.hswt.fi.search.service.index.api.IndexSearchService;
import de.hswt.fi.search.service.index.model.IndexJob;
import de.hswt.fi.search.service.index.model.IndexSearchResult;
import de.hswt.fi.search.service.index.model.IndexSearchResults;
import de.hswt.fi.search.service.index.model.IndexSettings;
import de.hswt.fi.search.service.index.model.MoleculePhDependency;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.properties.NumberSearchProperty;
import de.hswt.fi.search.service.mass.search.model.properties.NumberValueProperty;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Profile({Profiles.LC, Profiles.DEVELOPMENT_LC, Profiles.TEST})
@Component
@Scope("prototype")
public class RTISearchService implements IndexSearchService {

	private static final int TIMEOUT = 10000;
	private static final Logger LOGGER = LoggerFactory.getLogger(RTISearchService.class);
	private static final double LOGD_MAX_DELTA = 0.1;
	private static final double LOGD_MIN_VALUE = 0.35;
	private static final double LOGD_MAX_VALUE = 4.0;
	private static final double SCORE_MAX_LOGD_DELTA = 5.0;
	private List<IndexSearchResult> candidates;

	@Override
	public IndexSearchResults executeJob(IndexJob job, List<CompoundSearchService> searchServices) {
		LOGGER.debug("enter getResults with processing data {}", job);

		candidates = new ArrayList<>();

		IndexSettings settings = job.getSettings();

		double ionisation = settings.getIonisation().getIonisation();

		if (!searchServices.isEmpty()) {
			// One thread per CPU instead of one per feature: thousands of features exceeded the process limit of the server
			ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

			LOGGER.debug("starting RTI search for {} targets",
					job.getFeatureSet().getFeatures().size());

			for (Feature feature : job.getFeatureSet().getFeatures()) {

				double ppm = settings.getPpm();

				if (feature.isMassCalculated()) {
					ppm = settings.getFormulaDerivedMassesPpm();
				}

				executor.execute(new Evaluator(ppm,
						feature.isMassCalculated() ? 0.0 : ionisation, settings.getPh(),
						settings.getStationaryPhase().toString(), feature, searchServices));
			}

			LOGGER.debug("search for all targets data started");
			executor.shutdown();
			try {
				LOGGER.debug("waiting for RTI search to finish");
				executor.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				LOGGER.debug("waiting search to finish failed: {}", e.getMessage());
				Thread.currentThread().interrupt();
			}
		} else {
			LOGGER.debug("No search service available, RTI search not performed");
		}

		Comparator<IndexSearchResult> byTarget = Comparator.comparing(IndexSearchResult::getTargetIdentifier);

		Comparator<IndexSearchResult> byScore = Comparator.comparing(t -> t.getScore().getScoreValue());

		candidates = candidates.stream().sorted(byTarget.thenComparing(byScore.reversed()))
				.collect(Collectors.toList());

		LOGGER.debug("RTI search finished, return {} entries", candidates.size());

		Set<String> locatedFeatureIds = new HashSet<>();
		candidates.forEach(r -> locatedFeatureIds.add(r.getTargetIdentifier()));

		return new IndexSearchResults<>(candidates);
	}

    @Override
    public IndexSearchResult getDummyResult() {
        return IndexSearchResult.notAvailable();
    }

    /**
	 * The Class Evaluator is the container for each thread (runnable).
	 */
	private class Evaluator implements Runnable {

		private final List<CompoundSearchService> searchServices;

		private double ppm;

		private double ionisation;

		private double ph;

		private String stationaryPhase;

		private Feature feature;

		/**
		 * Instantiates a new evaluator.
		 *
		 * @param ppm             the ppm
		 * @param ionisation      the ionisation
		 * @param ph              the ph
		 * @param stationaryPhase the stationary phase
		 * @param feature         the target feature
		 * @param searchServices
		 */
		private Evaluator(double ppm, double ionisation, double ph, String stationaryPhase,
						  Feature feature, List<CompoundSearchService> searchServices) {
			this.ppm = ppm;
			this.feature = feature;
			this.ionisation = ionisation;
			this.ph = ph;
			this.stationaryPhase = stationaryPhase;
			this.searchServices = searchServices;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			double searchValue = feature.getNeutralMass();

			ArrayList<IndexSearchResult> tempEntries = new ArrayList<>();
			List<Entry> searchResult = searchServices.stream()
					.map(searchService -> searchService.searchByAccurateMassAndPh(new NumberSearchProperty(searchValue, ppm), ph))
					.flatMap(List::stream)
					.collect(Collectors.toList());

			for (Entry entry : searchResult) {
				MoleculePhDependency dependency = getMoleculePhDependency(entry, ph);
				Double adjustedLogD = getAdjustedLogD(feature.getRetentionTimeSignal(), dependency, stationaryPhase);

				tempEntries.add(new IndexSearchResult(feature.getIdentifier(), entry,
						feature.getRetentionTime(), ph, feature.getRetentionTimeSignal(), searchValue,
						feature.isMassCalculated(), adjustedLogD, getLogD(entry, ph), ppm,
						feature.getRetentionTimeIndex(), ionisation, stationaryPhase, dependency));
			}

			calculateScore(tempEntries, ph);
			findBestMatch(tempEntries);

			synchronized (candidates) {
				candidates.addAll(tempEntries);
			}
		}

		/**
		 * Gets the molecule pH dependency which is based on the logD values
		 * from the DB.
		 *
		 * @param entry the entry from the DB
		 * @param ph    the pH target level
		 * @return the molecule pH dependency, or null if no logD values are
		 * present
		 */
		private MoleculePhDependency getMoleculePhDependency(Entry entry, double ph) {
			Double min = getMinValue(entry.getLogdValues());
			Double max = getMaxValue(entry.getLogdValues());
			if (min == null || max == null) {
				return null;
			}

			double diff = max - min;

			NumberValueProperty property = getLogDProperty(entry, ph);

			if (property == null) {
				return null;
			}

			// loadable molecule
			if (diff > LOGD_MAX_DELTA) {
				if (property.getCharge() > 0) {
					return MoleculePhDependency.POSITIVE_LOADABLE;
				} else if (property.getCharge() < 0) {
					return MoleculePhDependency.NEGATIVE_LOADABLE;
				}
			}

			Double value = property.getValue();

			if (value == null) {
				return null;
			}

			if (value < LOGD_MIN_VALUE) {
				return MoleculePhDependency.NEUTRAL_LOWER;
			} else if (value > LOGD_MAX_VALUE) {
				return MoleculePhDependency.NEUTRAL_UPPER;
			} else {
				return MoleculePhDependency.NEUTRAL;
			}
		}

		/**
		 * Gets the min value of a number value set.
		 *
		 * @param values the values set
		 * @return the min value of the set, or null if the set is empty
		 */
		private Double getMinValue(Set<NumberValueProperty> values) {
			if (values.isEmpty()) {
				return null;
			}
			Double min = null;

			for (NumberValueProperty value : values) {
				if (min == null || value.getValue() < min) {
					min = value.getValue();
				}
			}
			return min;
		}

		/**
		 * Gets the max value of a number value set.
		 *
		 * @param values the values set
		 * @return the max value of the set, or null if the set is empty
		 */
		private Double getMaxValue(Set<NumberValueProperty> values) {
			if (values.isEmpty()) {
				return null;
			}
			Double max = null;
			for (NumberValueProperty value : values) {
				if (max == null || value.getValue() > max) {
					max = value.getValue();
				}
			}
			return max;
		}

		private void calculateScore(ArrayList<IndexSearchResult> entries, double ph) {
			if (entries == null || entries.isEmpty()) {
				return;
			}

			for (IndexSearchResult entry : entries) {
				Score scoreSummery;
				if (entry.isAvailable()) {
					double logD = getLogD(entry.getEntry(), ph);
					double diff = Math.abs(entry.getAdjustedLogD() - logD);
					double score = diff < SCORE_MAX_LOGD_DELTA ? (1.0 - diff / SCORE_MAX_LOGD_DELTA)
							: 0.0;
					scoreSummery = new Score(score, 1.0, score);
				} else {
					scoreSummery = new Score(Double.NaN, 1.0, Double.NaN);
				}
				entry.setScore(scoreSummery);
			}
		}

		/**
		 * Gets the logD value at the given pH level.
		 *
		 * @param entry the entry from the DB
		 * @param ph    the pH target level
		 * @return the logD value it it exist, otherwise null
		 */
		private NumberValueProperty getLogDProperty(Entry entry, double ph) {
			if (entry == null || entry.getLogdValues() == null) {
				return null;
			}
			for (NumberValueProperty property : entry.getLogdValues()) {
				if (property.getPh().equals(ph)) {
					return property;
				}
			}
			return null;
		}

		/**
		 * Adjusts a logD value (the predicted logD from the RTI) if the logD value
		 * from the DB is in a given range and the Molecule Dependency. The range is
		 * given by the group of Thomas Letztel at the TU Munich.
		 *
		 * @param targetLogD      the target logD to adjust
		 * @param dependency      the molecule dependency
		 * @param stationaryPhase the stationary phase
		 * @return the adjusted logD value
		 */
		private Double getAdjustedLogD(Double targetLogD, MoleculePhDependency dependency, String stationaryPhase) {
			if (targetLogD == null || targetLogD.isNaN()) {
				return null;
			}

			switch (dependency) {
				case NEUTRAL:
					return targetLogD;
				case NEUTRAL_LOWER:
					return targetLogD - 1.0;
				case NEUTRAL_UPPER:
					return targetLogD + 1.0;
				case POSITIVE_LOADABLE:
				case NEGATIVE_LOADABLE:
					if (stationaryPhase.equals(StationaryPhase.C18_POLAR_ENDCAPPED.toString())
							|| stationaryPhase.equals(StationaryPhase.C18_POLAR_EMBEDDED.toString())) {
						return targetLogD;
					} else {
						return targetLogD - 1.0;
					}
				default:
					return targetLogD;
			}
		}

		private Double getLogD(Entry entry, double ph) {
			for (NumberValueProperty logD : entry.getLogdValues()) {
				if (logD.getPh() != null && logD.getPh().equals(ph)) {
					return logD.getValue();
				}
			}

			return getMeanValue(entry.getLogdValues());
		}

		/**
		 * Each result set is weighted on the base of the logD value from the DB at
		 * the given pH level.
		 *
		 * @param entries the result entries
		 */
		private void findBestMatch(ArrayList<IndexSearchResult> entries) {

			if (entries == null || entries.isEmpty()) {
				return;
			}

			Optional<IndexSearchResult> optional = entries.stream()
					.max(Comparator.comparingDouble(entry -> entry.getScore().getScoreValue()));

			if (optional.isPresent()) {
				IndexSearchResult rtiSearchResult = optional.get();
				rtiSearchResult.setFirst(true);
			}

			optional = entries.stream().min(Comparator.comparingDouble(e -> e.getScore().getScoreValue()));

			if (optional.isPresent()) {
				IndexSearchResult r = optional.get();
				r.setLast(true);
			}
		}

		/**
		 * Gets the mean value.
		 *
		 * @param set the set
		 * @return the mean value
		 */
		private Double getMeanValue(Set<NumberValueProperty> set) {
			if (set == null || set.isEmpty()) {
				return 0.0;
			}
			double sum = 0.0;
			int quantity = 0;
			for (NumberValueProperty value : set) {
				if (value.getValue() != null) {
					sum += value.getValue();
					quantity++;
				}
			}
			return quantity == 0 ? null : sum / quantity;
		}
	}
}
