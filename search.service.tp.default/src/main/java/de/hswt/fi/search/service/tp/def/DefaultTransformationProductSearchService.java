package de.hswt.fi.search.service.tp.def;

import de.hswt.fi.common.SearchUtil;
import de.hswt.fi.model.Feature;
import de.hswt.fi.search.service.tp.api.TransformationProductSearchService;
import de.hswt.fi.search.service.tp.def.repository.PathwayCompoundRepositoryCustom;
import de.hswt.fi.search.service.tp.def.repository.PathwayRepository;
import de.hswt.fi.search.service.tp.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Component
@Scope("prototype")
// Needs the pathway database (SP-16)
@ConditionalOnProperty(name = "spring.datasource.pathway.enabled", havingValue = "true")
public class DefaultTransformationProductSearchService implements TransformationProductSearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTransformationProductSearchService.class);

	private final PathwayRepository pathwayRepository;

	private final PathwayCompoundRepositoryCustom pathwayCompoundRepository;

	@Autowired
	public DefaultTransformationProductSearchService(PathwayRepository pathwayRepository,
													 PathwayCompoundRepositoryCustom pathwayCompoundRepository) {
		this.pathwayRepository = pathwayRepository;
		this.pathwayCompoundRepository = pathwayCompoundRepository;
	}

	@Override
	public TransformationProductResult executeJob(TransformationProductJob job) {

		List<PathwayCandidate> inChiKeyPathwayCandidates = getPathwayCandidates(job.getInChiKeys());
		LOGGER.debug("found {} candidates via inChiKey", inChiKeyPathwayCandidates.size());

		List<FeatureWrapper> massCandidates = getMassCandidates(
				job.getFeatureSet().getFeatures(), job.getSettings().getPpm());
		LOGGER.debug("found {} candidates via mass range", massCandidates.size());

		Set<FeatureWrapper> assignedFeatures = assignFeaturesToPathways(inChiKeyPathwayCandidates, massCandidates);
		constructPathwayGraphs(inChiKeyPathwayCandidates);
		LOGGER.debug("candidates via inChiKey with explaining features: {}",
				inChiKeyPathwayCandidates.size());

		// Process unknown feature pathways
		massCandidates.removeAll(assignedFeatures);

		List<PathwayCandidate> featurePathwayCandidates = getFeaturePathwayCandidates(massCandidates);
		assignFeaturesToPathways(featurePathwayCandidates, massCandidates);
		constructPathwayGraphs(featurePathwayCandidates);

		LOGGER.debug("pathway candidates with only unknown features: {}",
				featurePathwayCandidates.size());

		// create result summary and result wrapper
		Set<String> inChiKeyPathwayIds = new HashSet<>();
		inChiKeyPathwayCandidates.forEach(c -> inChiKeyPathwayIds.addAll(c.getPathways().keySet()));
		Set<String> featurePathwayIds = new HashSet<>();
		featurePathwayCandidates.forEach(c -> featurePathwayIds.addAll(c.getPathways().keySet()));

		TransformationProductResultSummary resultSummary = new TransformationProductResultSummary(
				inChiKeyPathwayCandidates.size(), inChiKeyPathwayIds.size(),
				featurePathwayCandidates.size(), featurePathwayIds.size());

		return new TransformationProductResult(inChiKeyPathwayCandidates, featurePathwayCandidates, resultSummary);
	}

	/**
	 * Assign compounds of pathways from feature wrappers with only unknown
	 * features to a set of given pathways.
	 *
	 * @param pathwayCandidates
	 *            the pathways assigning the compounds to
	 * @param unkownFeatures
	 *            feature wrapper with pathways with unknown features compounds
	 *
	 * @return a set of feature wrappers, which were assigned to the given
	 *         pathways
	 */
	private Set<FeatureWrapper> assignFeaturesToPathways(List<PathwayCandidate> pathwayCandidates, List<FeatureWrapper> unkownFeatures) {
		Set<FeatureWrapper> assignedFeatures = new LinkedHashSet<>();

		for (PathwayCandidate candidate : pathwayCandidates) {
			List<FeatureWrapper> addedFeatures = getFeaturesOfPathways(candidate.getPathways(), unkownFeatures);
			if (!addedFeatures.isEmpty()) {
				addedFeatures.forEach(f -> candidate.addExplainedFeature(f.getFeature()));
				assignedFeatures.addAll(addedFeatures);
			}
		}

		return assignedFeatures;
	}

	/**
	 * Get the pathways specified in the feature wrappers fully from the db.
	 *
	 * @param massCandidates
	 *            feature wrappers, to get the full pathways for
	 *
	 * @return the pathways from the db
	 */
	private List<PathwayCandidate> getFeaturePathwayCandidates(List<FeatureWrapper> massCandidates) {
		List<PathwayCandidate> featurePathways = new ArrayList<>();
		for (FeatureWrapper feature : massCandidates) {
			PathwayCandidate pathway = new PathwayCandidate("");
			featurePathways.add(pathway);
			for (MatchingPathway matchingPathway : feature.getMatchingPathways()) {
				pathwayRepository.findById(matchingPathway.getId()).ifPresent(pathway::addPathway);
			}
		}

		return featurePathways;
	}

	/**
	 * Method to get all pathways witch contains a compound with one of the
	 * given InChiKeys.
	 *
	 * Each pathway is identified by it's root compound.
	 *
	 * @param inChiKeys
	 *            list of InChiKeys to search for pathways
	 *
	 * @return a list of pathways candidates
	 */
	private List<PathwayCandidate> getPathwayCandidates(List<String> inChiKeys) {
		ExecutorService executor = Executors.newFixedThreadPool(100);

		List<PathwayCandidate> pathwayCandidates = new ArrayList<>();
		List<FutureTask<PathwayCandidate>> compoundTasks = new ArrayList<>();

		for (String inChiKey : inChiKeys) {
			FutureTask<PathwayCandidate> task = new FutureTask<>(
					() -> executePathwaySearch(inChiKey));
			compoundTasks.add(task);
			executor.execute(task);
		}

		for (FutureTask<PathwayCandidate> task : compoundTasks) {
			PathwayCandidate candidate = null;
			try {
				candidate = task.get();
			} catch (InterruptedException e) {
				LOGGER.info("Unable to get result pathway for InChi key.", e.getCause());
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				LOGGER.info("Unable to get result pathway for InChi key.", e.getCause());
			}
			if (candidate != null) {
				pathwayCandidates.add(candidate);
			}
		}

		executor.shutdown();

		return pathwayCandidates;
	}

	private PathwayCandidate executePathwaySearch(String inChiKey) {
		if (inChiKey.isEmpty()) {
			return null;
		}

		List<Pathway> pathways = pathwayRepository.findByCompounds_InChiKey(inChiKey);
		if (pathways.isEmpty()) {
			return null;
		}

		PathwayCandidate candidate = new PathwayCandidate(inChiKey);
		pathways.forEach(candidate::addPathway);

		return candidate;
	}

	private List<FeatureWrapper> getMassCandidates(Collection<Feature> features, double ppm) {
		ExecutorService executor = Executors.newFixedThreadPool(100);
		List<FeatureWrapper> candidates = new ArrayList<>();
		List<FutureTask<FeatureWrapper>> massTasks = new ArrayList<>();

		for (Feature feature : features) {
			FutureTask<FeatureWrapper> task = new FutureTask<>(() -> executePathwaySearch(feature, ppm));
			massTasks.add(task);
			executor.execute(task);
		}

		for (FutureTask<FeatureWrapper> task : massTasks) {
			FeatureWrapper wrapper = null;
			try {
				wrapper = task.get();
			} catch (InterruptedException e) {
				LOGGER.info("Unable to get result pathway for mass range.", e.getCause());
				Thread.currentThread().interrupt();
			}
			catch (ExecutionException e) {
				LOGGER.info("Unable to get result pathway for mass range.", e.getCause());
			}
			if (wrapper != null) {
				candidates.add(wrapper);
			}
		}

		executor.shutdown();

		return candidates;
	}

	private FeatureWrapper executePathwaySearch(Feature feature, double ppm) {
		double mass = feature.getNeutralMass();
		double delta = SearchUtil.getRangeFromPPM(mass, ppm);

		List<MatchingPathway> candidates =
				pathwayCompoundRepository.aggregateCompoundsInMassRange(mass - delta, mass + delta);

		if (candidates.isEmpty()) {
			return null;
		}

		FeatureWrapper result = new FeatureWrapper(feature);
		result.setMatchingPathways(candidates);

		return result;
	}

	private List<FeatureWrapper> getFeaturesOfPathways(Map<String, Pathway> resultPathways, List<FeatureWrapper> featureWrappers) {
		List<FeatureWrapper> resultContainedFeatures = new ArrayList<>();
		Set<String> resultPathwayIds = resultPathways.keySet();

		for (FeatureWrapper featureWrapper : featureWrappers) {
			for (MatchingPathway pathway : featureWrapper.getMatchingPathways()) {
				if (resultPathwayIds.contains(pathway.getId())) {
					resultContainedFeatures.add(featureWrapper);
					addFeatureToPathway(resultPathways.get(pathway.getId()), pathway, featureWrapper);
				}
			}
		}

		return resultContainedFeatures;
	}

	private void addFeatureToPathway(Pathway pathway, MatchingPathway matchingPathway, FeatureWrapper feature) {
		try {
			for (Compound compound : pathway.getCompounds()) {
				if (matchingPathway.getInChiKeys().contains(compound.getInChiKey())) {
					compound.addMatchingFeature(feature.getFeature());
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	private void constructPathwayGraphs(List<PathwayCandidate> pathwayCandidates) {
		for (PathwayCandidate pathwayCandidate : pathwayCandidates) {
			pathwayCandidate.getPathways().values().forEach(this::constructPathway);
		}
	}

	private void constructPathway(Pathway pathway) {
		for (Transformation transformation : pathway.getTransformations()) {
			for (Compound compound : pathway.getCompounds()) {
				if (compound.getTransformationIds().contains(transformation.getId())) {
					compound.addTransformation(transformation);
					transformation.setCompound(compound);
				} else if (transformation.getTransformationProductId().equals(compound.getId())) {
					transformation.setTransformationProduct(compound);
				}
				if (compound.getRoot()) {
					pathway.setRoot(compound);
				}
			}
		}
	}

	private class FeatureWrapper {

		private Feature feature;

		private List<MatchingPathway> matchingPathways;

		FeatureWrapper(Feature feature) {
			matchingPathways = new ArrayList<>();
			this.feature = feature;
		}

		Feature getFeature() {
			return feature;
		}

		List<MatchingPathway> getMatchingPathways() {
			return matchingPathways;
		}

		void setMatchingPathways(List<MatchingPathway> matchingPathways) {
			this.matchingPathways = matchingPathways;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			FeatureWrapper that = (FeatureWrapper) o;
			return Objects.equals(feature, that.feature) &&
					Objects.equals(matchingPathways, that.matchingPathways);
		}

		@Override
		public int hashCode() {
			return Objects.hash(feature, matchingPathways);
		}
	}
}
