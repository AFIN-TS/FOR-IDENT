package de.hswt.fi.search.service.mass.search.def;

import de.hswt.fi.common.SearchUtil;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.Score;
import de.hswt.fi.search.service.mass.search.api.MassSearchService;
import de.hswt.fi.search.service.mass.search.model.*;
import de.hswt.fi.search.service.mass.search.model.properties.NumberSearchProperty;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class DefaultMassSearchService implements MassSearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMassSearchService.class);

	@Override
	public MassSearchResults executeJob(MassSearchJob job, List<CompoundSearchService> selectedSearchServices) {
		List<MassSearchResult> results = new ArrayList<>();

		int targetsCount = job.getFeatureSet().getFeatures().size();
		FileSearchResultSummary resultSummary = new FileSearchResultSummary();
		resultSummary.setFeaturesCount(targetsCount);
		resultSummary.setUnlocatedFeaturesCount(targetsCount);

		if (selectedSearchServices.isEmpty()) {
			return new MassSearchResults(results, resultSummary);
		}

		List<Feature> features = job.getFeatureSet().getFeatures();
		FileSearchSettings settings = job.getSettings();

		// One thread per CPU instead of one per feature: thousands of features exceeded the process limit of the server
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (Feature feature : features) {
			Double mass = feature.getNeutralMass();
			if (mass == null || mass.isNaN() || mass.isInfinite()) {
				continue;
			}

			double ppm = settings.getPpm();

			if (feature.isMassCalculated()) {
				ppm = settings.getFormulaDerivedMassesPpm();
			}

			executor.execute(new Evaluator(feature.getIdentifier(), ppm, mass,
					feature.getRetentionTime(), results, selectedSearchServices));
		}

		executor.shutdown();
		try {
			executor.awaitTermination(600, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("An error occured {}", e);
			Thread.currentThread().interrupt();
		}

		Set<String> locatedFeatureIds = new HashSet<>();
		results.forEach(r -> locatedFeatureIds.add(r.getTargetIdentifier()));
		resultSummary.setLocatedFeaturesCount(locatedFeatureIds.size());
		resultSummary.setUnlocatedFeaturesCount(targetsCount - locatedFeatureIds.size());
		resultSummary.setCandidatesCount(results.size());

		return new MassSearchResults(sortResults(results), resultSummary);
	}

	private List<MassSearchResult> sortResults(List<MassSearchResult> results) {
		Comparator<MassSearchResult> byTarget = Comparator.comparing(MassSearchResult::getTargetIdentifier);

		Comparator<MassSearchResult> byScore = Comparator.comparing(t -> t.getScore().getScoreValue());

		return results.stream().sorted(byTarget.thenComparing(byScore.reversed()))
				.collect(Collectors.toList());
	}

	private class Evaluator implements Runnable {

		String id;

		double ppm;

		double mass;

		Double retentionTime;

		List<MassSearchResult> results;

		List<CompoundSearchService> compoundSearchServices;

		private Evaluator(String id, double ppm, double mass, Double retentionTime,
						  List<MassSearchResult> results, List<CompoundSearchService> compoundSearchServices) {
			this.id = id;
			this.ppm = ppm;
			this.mass = mass;
			this.results = results;
			this.retentionTime = retentionTime;
			this.compoundSearchServices = compoundSearchServices;

		}

		@Override
		public void run() {
			NumberSearchProperty searchProperty = new NumberSearchProperty(mass, ppm);

			List<Entry> resultEntries = compoundSearchServices.stream()
					.map(searchService -> searchService.searchByAccurateMass(searchProperty))
					.flatMap(List::stream)
					.collect(Collectors.toList());

			List<MassSearchResult> currentResults = new ArrayList<>();

			for (Entry entry : resultEntries) {
				double deltaMass = 0.0;
				if (entry.getAccurateMass() != null) {
					deltaMass = mass - entry.getAccurateMass().getValue();
				}
				currentResults.add(new MassSearchResult(id, entry, mass, deltaMass, retentionTime));
			}

			if (!currentResults.isEmpty()) {
				double maxDelta = SearchUtil.getRangeFromPPM(mass, ppm);
				createScore(currentResults, maxDelta);

				synchronized (results) {
					results.addAll(currentResults);
				}
			}
		}

		private void createScore(List<MassSearchResult> results, double maxDelta) {
			double minDelta = Double.MAX_VALUE;
			for (MassSearchResult result : results) {
				double delta = Math.abs(result.getDeltaMass());
				if (delta < minDelta) {
					minDelta = delta;
				}
				double score = Math.max(1.0 - (delta) / (maxDelta), 0.0);
				result.setScore(new Score(score, 1.0, score));
			}
		}
	}
}
