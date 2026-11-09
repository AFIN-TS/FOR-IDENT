package de.hswt.fi.processing.service.def;

import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.Score;
import de.hswt.fi.msms.service.api.MsMsService;
import de.hswt.fi.msms.service.model.MsMsCandidate;
import de.hswt.fi.msms.service.model.MsMsData;
import de.hswt.fi.msms.service.model.MsMsJob;
import de.hswt.fi.msms.service.model.MsMsSettings;
import de.hswt.fi.processing.service.api.ProcessingService;
import de.hswt.fi.processing.service.model.*;
import de.hswt.fi.processing.service.model.ProcessingUnitState.UnitState;
import de.hswt.fi.search.service.index.api.IndexSearchService;
import de.hswt.fi.search.service.index.model.IndexJob;
import de.hswt.fi.search.service.index.model.IndexSearchResult;
import de.hswt.fi.search.service.index.model.IndexSearchResults;
import de.hswt.fi.search.service.index.model.IndexSettings;
import de.hswt.fi.search.service.mass.search.api.MassSearchService;
import de.hswt.fi.search.service.mass.search.model.FileSearchSettings;
import de.hswt.fi.search.service.mass.search.model.MassSearchJob;
import de.hswt.fi.search.service.mass.search.model.MassSearchResult;
import de.hswt.fi.search.service.mass.search.model.MassSearchResults;
import de.hswt.fi.search.service.tp.api.TransformationProductSearchService;
import de.hswt.fi.search.service.tp.model.PathwayCandidate;
import de.hswt.fi.search.service.tp.model.TransformationProductJob;
import de.hswt.fi.search.service.tp.model.TransformationProductResult;
import de.hswt.fi.search.service.tp.model.TransformationProductSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class DefaultProcessingService implements ProcessingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProcessingService.class);
	private PreProcessingService preProcessService;
	private MassSearchService massSearchService;
	private IndexSearchService indexSearchService;
	private MsMsService msmsService;
	private TransformationProductSearchService tpService;
	private double scoreSumMax = 0.0;
	private Consumer<ProcessingUnitState> stateUpdateCallback;
	private ProcessResultSummary resultSummary;
	private ProcessingSettings settings;

	// Transformation products need the pathway database (MongoDB), it can be switched off
	@Value("${spring.datasource.pathway.enabled}")
	private boolean tpEnabled;

	@Override
	public ProcessingResult executeJob(ProcessingJob job, Consumer<ProcessingUnitState> stateUpdateCallback) {
		this.stateUpdateCallback = stateUpdateCallback;
		this.settings = job.getSettings();

		List<ProcessResultWrapper> results = null;

		scoreSumMax = 0.0;

		resultSummary = new ProcessResultSummary();

		job.setRequestedProcessUnits(new ArrayList<>(getProcessingUnitOrder(job.getSettings())));

		preProcessService.updateFeatures(job);

		for (ProcessingUnit unit : job.getRequestedProcessUnits()) {
			results = executeUnit(unit, job, results);
		}

		if (results == null) {
			return new ProcessingResult(new ArrayList<>(), resultSummary);
		}

		calculateScore(results, job.getSettings());

		List<ProcessCandidate> candidates = new ArrayList<>();

		results.forEach(result -> candidates.addAll(result.getCandidates().stream().sorted((c1, c2) -> Double
				.compare(c2.getScore().getScoreValue(), c1.getScore().getScoreValue()))
				.collect(Collectors.toList())));

		return new ProcessingResult(candidates, resultSummary);
	}

	@Override
	public List<ProcessingUnit> getAvailableProcessingUnits(ProcessingJob job) {
		List<ProcessingUnit> units = new ArrayList<>();

		if (isMassScreeningPossible(job)) {
			units.add(ProcessingUnit.MASS_SCREENING);
		} else {
			// no basic processing is possible
			return units;
		}

		if (isRtiScreeningPossible(job)) {
			units.add(ProcessingUnit.RTI_SCREENING);
		}

		if (isMsMsPossible(job)) {
			units.add(ProcessingUnit.MSMS);
		}

		if (isTpPossible()) {
			units.add(ProcessingUnit.TP);
		}

		units.add(ProcessingUnit.MASSBANK_SIMPLE);

		return units;
	}

	@Override
	public Set<ProcessingUnit> getProcessingUnitOrder(ProcessingSettings settings) {
		Set<ProcessingUnit> order = new LinkedHashSet<>();

		if (settings.getScoreSettings().getMassScreeningState().isExecute()) {
			order.add(ProcessingUnit.MASS_SCREENING);
		} else {
			return order;
		}

		if (settings.getScoreSettings().getRtiScreeningState().isExecute()) {
			order.add(ProcessingUnit.RTI_SCREENING);
		}

		if (settings.getScoreSettings().getMsmsState().isExecute()) {
			order.add(ProcessingUnit.MSMS);
		}

		if (tpEnabled && settings.getScoreSettings().getTpState().isExecute()) {
			order.add(ProcessingUnit.TP);
		}

		if (settings.getScoreSettings().getMassBankSimpleState().isExecute()) {
			order.add(ProcessingUnit.MASSBANK_SIMPLE);
		}

		return order;
	}

	private List<ProcessResultWrapper> executeUnit(ProcessingUnit unit, ProcessingJob job, List<ProcessResultWrapper> results) {

		LOGGER.debug("Executing Unit {}", unit);
		switch (unit) {
			case MASS_SCREENING:
				return executeMassScreening(job);
			case RTI_SCREENING:
				return executeRti(job, results);
			case MSMS:
				return executeMsMs(job, results);
			case TP:
				return executeTp(job, results);
			case MASSBANK_SIMPLE:
				return executeMassBankSimple(job, results);
			default:
				throw new InvalidParameterException("Unknown process unit");
		}
	}

	private void updateState(ProcessingUnitState processUnitState, UnitState unitState) {
		processUnitState.setUnitState(unitState);
		if (stateUpdateCallback == null) {
			return;
		}
		stateUpdateCallback.accept(processUnitState);
	}

	private List<ProcessResultWrapper> executeMassScreening(ProcessingJob job) {
		updateState(job.getSettings().getScoreSettings().getMassScreeningState(),
				UnitState.PROCESSING);

		List<ProcessResultWrapper> processResults = executeMassSearch(job);

		updateState(job.getSettings().getScoreSettings().getMassScreeningState(),
				UnitState.FINISHED);

		return processResults;
	}

	private List<ProcessResultWrapper> executeMassSearch(ProcessingJob job) {

		List<ProcessResultWrapper> processResults = new ArrayList<>();

		scoreSumMax += job.getSettings().getScoreSettings().getMassScreeningState()
				.getScoreWeight();

		FileSearchSettings fileSearchSettings = new FileSearchSettings();
		fileSearchSettings.setPpm(job.getSettings().getPrecursorPpm());
		fileSearchSettings.setIonisation(job.getSettings().getIonisation());

		MassSearchJob massSearchJob = new MassSearchJob(fileSearchSettings, job.getFeatureSet());

		MassSearchResults jobResult = massSearchService.executeJob(massSearchJob, job.getSelectedSearchServices());

		List<MassSearchResult> results = jobResult.getResults();
		resultSummary.addResultSummary(jobResult.getResultSummary());

		Map<String, List<ProcessCandidate>> candidates = new HashMap<>();
		for (MassSearchResult result : results) {
			if (!candidates.containsKey(result.getTargetIdentifier())) {
				candidates.put(result.getTargetIdentifier(), new ArrayList<>());
			}
			candidates.get(result.getTargetIdentifier()).add(new ProcessCandidate(result));
		}

		candidates.keySet().forEach(
				key -> processResults.add(new ProcessResultWrapper(key, candidates.get(key))));

		return processResults;
	}

	private List<ProcessResultWrapper> executeTp(ProcessingJob job, List<ProcessResultWrapper> results) {
		updateState(job.getSettings().getScoreSettings().getTpState(), UnitState.PROCESSING);

		List<ProcessCandidate> candidates = new ArrayList<>();
		for (ProcessResultWrapper result : results) {
			candidates.addAll(result.getCandidates());
		}

		Map<String, ProcessCandidate> known = new HashMap<>();
		for (ProcessCandidate candidate : candidates) {
			String inChiKey = null;
			if (candidate.getEntry() != null) {
				inChiKey = candidate.getEntry().getInchiKey().getValue();
			}
			if (inChiKey != null) {
				known.put(inChiKey, candidate);
			}
		}

		TransformationProductSettings tpSettings = new TransformationProductSettings();
		tpSettings.setPpm(job.getSettings().getPrecursorPpm());

		TransformationProductJob tpJob = new TransformationProductJob(tpSettings,
				new ArrayList<>(known.keySet()), job.getFeatureSet());

		TransformationProductResult result = tpService.executeJob(tpJob);

		for (PathwayCandidate pathwayCandidate : result.getInChiKeyPathwayCandidates()) {
			known.get(pathwayCandidate.getInChiKey()).setPathwayCandidate(pathwayCandidate);
		}

		updateState(job.getSettings().getScoreSettings().getTpState(), UnitState.FINISHED);

		return results;
	}

	private List<ProcessResultWrapper> executeRti(ProcessingJob job, List<ProcessResultWrapper> results) {

		updateState(job.getSettings().getScoreSettings().getRtiScreeningState(),
				UnitState.PROCESSING);

		scoreSumMax += job.getSettings().getScoreSettings().getRtiScreeningState().getScoreWeight();

		IndexSettings indexSettings = new IndexSettings();
		indexSettings.setIonisation(job.getSettings().getIonisation());
		indexSettings.setPpm(job.getSettings().getPrecursorPpm());
		indexSettings.setPh(job.getSettings().getPh());
		indexSettings.setStationaryPhase(job.getSettings().getStationaryPhase());

		IndexJob indexJob = new IndexJob(indexSettings, job.getFeatureSet());

		IndexSearchResults jobResult = indexSearchService.executeJob(indexJob, job.getSelectedSearchServices());
		List<IndexSearchResult> rtiResults = jobResult.getResults();

		for (ProcessResultWrapper result : results) {
			parseRtiResult(result, rtiResults);
		}

		updateState(job.getSettings().getScoreSettings().getRtiScreeningState(),
				UnitState.FINISHED);

		return results;
	}

	private List<ProcessResultWrapper> executeMsMs(ProcessingJob job, List<ProcessResultWrapper> results) {
		updateState(job.getSettings().getScoreSettings().getMsmsState(), UnitState.PROCESSING);

		scoreSumMax += job.getSettings().getScoreSettings().getMsmsState().getScoreWeight();

		List<MsMsData> processingData = new ArrayList<>();
		for (ProcessResultWrapper result : results) {

			Optional<Feature> matchedFeature = job.getFeatureSet().getFeatures().stream()
					.filter(feature -> feature.getIdentifier().equals(result.getID())).findFirst();

			if (!matchedFeature.isPresent()) {
				continue;
			}

			MsMsData data = new MsMsData(matchedFeature.get(), getCandidateSmiles(result));
			processingData.add(data);
		}

		MsMsSettings msMsSettings = new MsMsSettings();
		msMsSettings.setPpm(job.getSettings().getPrecursorPpm());
		msMsSettings.setPpmFragments(job.getSettings().getPpmFragments());
		msMsSettings.setIonisation(job.getSettings().getIonisation());
		msMsSettings.setUseLocalCandidates(true);

		MsMsJob msMsJob = new MsMsJob(msMsSettings, processingData);

		Map<String, List<MsMsCandidate>> msMsResults = msmsService.process(msMsJob);

		for (ProcessResultWrapper result : results) {
			if (!msMsResults.containsKey(result.getID())) {
				continue;
			}
			parseMsMsResult(result, msMsResults.get(result.getID()));
		}

		updateState(job.getSettings().getScoreSettings().getMsmsState(), UnitState.FINISHED);

		return results;
	}

	private List<ProcessResultWrapper> executeMassBankSimple(ProcessingJob job, List<ProcessResultWrapper> results) {
		updateState(job.getSettings().getScoreSettings().getMassBankSimpleState(),
				UnitState.PROCESSING);

		scoreSumMax += job.getSettings().getScoreSettings().getMassBankSimpleState()
				.getScoreWeight();

		for (ProcessResultWrapper result : results) {
			for (ProcessCandidate candidate : result.getCandidates()) {
				candidate.setMassBankSimpleScore(
						new Score((candidate.getEntry().getMassBankIds()!= null && !candidate.getEntry().getMassBankIds().isEmpty()) ? 1.0 : 0.0, 0.0, 0.0));
			}
		}

		updateState(job.getSettings().getScoreSettings().getMassBankSimpleState(),
				UnitState.FINISHED);
		return results;
	}

	private void calculateScore(List<ProcessResultWrapper> results, ProcessingSettings settings) {
		for (ProcessResultWrapper result : results) {
			calculateScore(result, settings);
		}
	}

	private void calculateScore(ProcessResultWrapper result, ProcessingSettings settings) {

		double scoreMax = 0.0;
		double reducedScore = 0.0;

		for (int i = 0; i < result.getCandidates().size(); i++) {

			ProcessCandidate candidate = result.getCandidates().get(i);

			// Disable MetFrag Score for only single candidates and set best match true
			if (settings.getScoreSettings().getMsmsState().isEnabled() && result.getCandidates().size() == 1) {
				candidate.getMsMsCandidate().setScore(new Score(Double.NaN, settings.getScoreSettings().getMsmsState
						().getScoreWeight(), Double.NaN));
				candidate.setScore(new Score(Double.NaN, Double.NaN, Double.NaN));
				candidate.setBestMatch(true);
				return;
			}

			double summarizedScore = calculateCandidateScore(candidate, settings);
			Score score = new Score(summarizedScore / scoreSumMax, scoreSumMax, summarizedScore);
			candidate.setScore(score);

			if (summarizedScore > scoreMax) {
				scoreMax = summarizedScore;
				reducedScore = summarizedScore / scoreSumMax;
			}
		}

		if (reducedScore > 0) {
			final double score = reducedScore;
			result.getCandidates().stream().filter(c -> c.getScore().getScoreValue() == score)
					.forEach(c -> c.setBestMatch(true));
		}
	}

	private double calculateCandidateScore(ProcessCandidate candidate, ProcessingSettings settings) {

		double summarizedScore = 0.0;

		if (candidate.getMassSearchResult() != null) {
			recalculateScore(candidate.getMassSearchResult().getScore(),
					settings.getScoreSettings().getMassScreeningState().getScoreWeight());
			summarizedScore += candidate.getMassSearchResult().getScore().getWeightedValue();
		}

		if (candidate.getIndexSearchResult() != null) {
			recalculateScore(candidate.getIndexSearchResult().getScore(),
					settings.getScoreSettings().getRtiScreeningState().getScoreWeight());
			summarizedScore += candidate.getIndexSearchResult().getScore().getWeightedValue();
		}

		if (candidate.getMsMsCandidate() != null) {

			if (candidate.getMsMsCandidate().getFeature() == null) {
				candidate.getMsMsCandidate().getScore().setScoreValue(Double.NaN);
				candidate.getMsMsCandidate().getScore().setWeightedValue(Double.NaN);
			}

			recalculateScore(candidate.getMsMsCandidate().getScore(),
					settings.getScoreSettings().getMsmsState().getScoreWeight());
			summarizedScore += candidate.getMsMsCandidate().getScore().getWeightedValue();
		}

		if (candidate.getMassBankSimpleScore() != null) {
			recalculateScore(candidate.getMassBankSimpleScore(),
					settings.getScoreSettings().getMassBankSimpleState().getScoreWeight());
			summarizedScore += candidate.getMassBankSimpleScore().getWeightedValue();
		}

		return summarizedScore;
	}

	private void recalculateScore(Score score, double weight) {
		score.setWeight(weight);
		double value = !Double.isNaN(score.getScoreValue()) ? score.getScoreValue() : Double.NaN;
		score.setWeightedValue(value * score.getWeight());
	}

	private List<String> getCandidateSmiles(ProcessResultWrapper result) {
		List<String> smiles = new ArrayList<>();

		for (ProcessCandidate candidate : result.getCandidates()) {
			if (candidate.getEntry().getSmiles() != null) {
				smiles.add(candidate.getEntry().getSmiles().getValue());
			}
		}

		return smiles;
	}

	private void parseRtiResult(ProcessResultWrapper processResult, List<IndexSearchResult> indexSearchResults) {

		for (IndexSearchResult indexSearchResult : indexSearchResults) {
			for (ProcessCandidate processCandidate : processResult.getCandidates()) {
				if (processCandidate.getId().equals(indexSearchResult.getID())) {
					processCandidate.setIndexSearchResult(indexSearchResult);
				}
			}
		}

		// For process candidates with no msmsCandidate found, an null msmsCandidate is created to display scoring
		for (ProcessCandidate processCandidate : processResult.getCandidates()) {
			if (processCandidate.getIndexSearchResult() == null) {
				IndexSearchResult dummyResult = indexSearchService.getDummyResult();
				dummyResult.setTargetIdentifier(processCandidate.getMassSearchResult().getTargetIdentifier());
				dummyResult.setEntry(processCandidate.getMassSearchResult().getEntry());
				dummyResult.setScore(new Score(Double.NaN, 1.0d, Double.NaN));
				processCandidate.setIndexSearchResult(dummyResult);
			}
		}
	}

	private void parseMsMsResult(ProcessResultWrapper processResult, List<MsMsCandidate> candidates) {
		for (MsMsCandidate msmsCandidate : candidates) {
			for (ProcessCandidate processCandidate : processResult.getCandidates()) {
				if (processCandidate.getEntry().getSmiles().getValue()
						.equals(msmsCandidate.getIdentifier())) {
					processCandidate.setMsMsCandidate(msmsCandidate);
					break;
				}
			}
		}

		// For process candidates with no msmsCandidate found, an null msmsCandidate is created to display scoring
		for (ProcessCandidate processCandidate : processResult.getCandidates()) {
			if (processCandidate.getMsMsCandidate() == null) {
				processCandidate.setMsMsCandidate(new MsMsCandidate(null, new Score(Double.NaN,
						settings.getScoreSettings().getMsmsState().getScoreWeight(), 0), null, null,
						null, null, null, null, 0, null, null, null));
			}
		}
	}

	private boolean isMassScreeningPossible(ProcessingJob job) {
		return !job.getFeatureSet().getFeatures().isEmpty();
	}

	private boolean isRtiScreeningPossible(ProcessingJob job) {
		return !job.getFeatureSet().getFeatures().isEmpty() && !job.getFeatureSet()
				.getRtiCalibrationData().isEmpty();
	}

	private boolean isTpPossible() {
		return tpEnabled;
	}

	private boolean isMsMsPossible(ProcessingJob job) {
		return job.getFeatureSet().getFeatures().stream()
				.anyMatch(f -> !f.getPeaks().isEmpty()) && job.getSettings().getScoreSettings()
				.getMsmsState().isEnabled();
	}

	@Override
	public ProcessingJob getProcessingJob(FeatureSet featureSet) {
		ProcessingJob job = new ProcessingJob(featureSet);
		updateAvailableDataStates(job);
		calculateInitialScores(job);
		return job;
	}

	private void updateAvailableDataStates(ProcessingJob job) {

		if (job.getFeatureSet().getFeatures() != null || !job.getFeatureSet().getFeatures()
				.isEmpty()) {
			job.getSettings().getScoreSettings().getMassScreeningState().setDataAvailable(true);
		} else {
			return;
		}

		if (job.getFeatureSet().getRtiCalibrationData() != null && !job.getFeatureSet()
				.getRtiCalibrationData().isEmpty()) {
			job.getSettings().getScoreSettings().getRtiScreeningState().setDataAvailable(true);
		}

		job.getSettings().getScoreSettings().getMsmsState().setDataAvailable(
				job.getFeatureSet().getFeatures().stream()
						.anyMatch(f -> !f.getPeaks().isEmpty()));

		if (job.getSettings().getScoreSettings().getTpState() != null) {
			job.getSettings().getScoreSettings().getTpState().setDataAvailable(true);
		}

		job.getSettings().getScoreSettings().getMassBankSimpleState().setDataAvailable(true);
	}

	private void calculateInitialScores(ProcessingJob job) {

		ScoreSettings scoreSettings = job.getSettings().getScoreSettings();

		List<ProcessingUnitState> availableProcesses = scoreSettings.getProcessingUnitStates()
				.stream()
				.filter(ProcessingUnitState::isScoreable)
				.filter(ProcessingUnitState::isDataAvailable)
				.collect(Collectors.toList());

		if (availableProcesses.isEmpty()) {
			return;
		}

		double scoreWeight = 1d / availableProcesses.size();

		availableProcesses.forEach(processingUnitState -> processingUnitState.setScoreWeight(scoreWeight));
	}

	@Autowired
	public void setPreProcessService(PreProcessingService preProcessService) {
		this.preProcessService = preProcessService;
	}

	@Autowired
	public void setMassSearchService(MassSearchService massSearchService) {
		this.massSearchService = massSearchService;
	}

	@Autowired
	public void setIndexSearchService(IndexSearchService indexSearchService) {
		this.indexSearchService = indexSearchService;
	}

	@Autowired
	public void setMsmsService(MsMsService msmsService) {
		this.msmsService = msmsService;
	}

	// Only present with the pathway database (FI_PATHWAY_ENABLED, SP-16)
	@Autowired(required = false)
	public void setTpService(TransformationProductSearchService tpService) {
		this.tpService = tpService;
	}

	private static class ProcessResultWrapper {

		private String id;
		private List<ProcessCandidate> candidates;

		ProcessResultWrapper(String id, List<ProcessCandidate> candidates) {
			this.id = id;
			this.candidates = candidates;
		}

		String getID() {
			return id;
		}

		List<ProcessCandidate> getCandidates() {
			return candidates;
		}
	}
}
