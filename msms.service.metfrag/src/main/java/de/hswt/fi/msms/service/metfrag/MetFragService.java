package de.hswt.fi.msms.service.metfrag;

import de.hswt.fi.model.Feature;
import de.hswt.fi.model.Peak;
import de.hswt.fi.model.Score;
import de.hswt.fi.msms.service.api.MsMsService;
import de.hswt.fi.msms.service.model.*;
import de.ipbhalle.metfraglib.exceptions.RelativeIntensityNotDefinedException;
import de.ipbhalle.metfraglib.interfaces.ICandidate;
import de.ipbhalle.metfraglib.interfaces.IMatch;
import de.ipbhalle.metfraglib.list.CandidateList;
import de.ipbhalle.metfraglib.list.SortedTandemMassPeakList;
import de.ipbhalle.metfraglib.peak.TandemMassPeak;
import de.ipbhalle.metfraglib.settings.MetFragGlobalSettings;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Component
@Scope("prototype")
public class MetFragService implements MsMsService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MetFragService.class);

	@Override
	public Map<String, List<MsMsCandidate>> process(MsMsJob job) {
		Map<String, List<MsMsCandidate>> results = new HashMap<>();
		if (job == null) {
			return results;
		}

		// One thread per CPU instead of one per feature: thousands of features exceeded the process limit of the server
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		List<FutureTask<Pair>> tasks = new ArrayList<>();

		for (MsMsData data : job.getMsMsData()) {
			FutureTask<Pair> task = new FutureTask<>(() -> processLocal(data, job.getSettings()));
			tasks.add(task);
			executor.execute(task);
		}
		executor.shutdown();

		for (FutureTask<Pair> task : tasks) {
			Pair pair;
			try {
				pair = task.get();
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage());
				Thread.currentThread().interrupt();
				return results;
			} catch (ExecutionException e) {
				LOGGER.error(e.getMessage());
				return results;
			}
			results.put(pair.key, pair.candidates);
		}

		// MetFrag Score is only meaningful to prioritize between multiple candidates,
		// therefore reset scores for single candidates to zero
		resetScoresForSingleCandidates(results);

		return results;
	}

	private void resetScoresForSingleCandidates(Map<String, List<MsMsCandidate>> results) {

		// Find single candidate and set score to null
		results.keySet().stream().filter(key -> results.get(key).size() == 1).forEach(key -> results.get(key).get(0)
				.getScore().setScoreValue(0d));

	}

	private Pair processLocal(MsMsData data, MsMsSettings settings) {
		List<MsMsCandidate> candidates = new ArrayList<>();

		MetFragGlobalSettings globalSettings = new MetFragGlobalSettings();

		CandidateList scoredCandidateList;

		initSettingsLocal(data, settings, globalSettings);
		CombinedMetFragProcess metFragProcess = new CombinedMetFragProcess(globalSettings);

		try {
			metFragProcess.retrieveCompounds(data.getCandidatesSmiles());
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return null;
		}
		try {
			metFragProcess.run();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		scoredCandidateList = metFragProcess.getCandidateList();

		for (int i = 0; i < scoredCandidateList.getNumberElements(); i++) {
			MsMsCandidate candidate = parseCandidate(data.getFeature(),
					scoredCandidateList.getElement(i));
			if (candidate != null) {
				candidates.add(candidate);
			}
		}

		return new Pair(data.getFeature().getIdentifier(), candidates);
	}

	private MsMsCandidate parseCandidate(Feature data, ICandidate candidate) {
		List<MsMsCandidateFragment> candidateFragments = new ArrayList<>();
		// MetFrag 2.6 releases the precursor molecule after scoring; formula and SMILES of the fragments
		// need it again (as in the result writers of MetFrag)
		try {
			candidate.initialisePrecursorCandidate();
		} catch (Exception e) {
			LOGGER.error("Cannot initialise candidate {}", candidate.getIdentifier(), e);
			return null;
		}
		for (int i = 0; i < candidate.getMatchList().getNumberElements(); i++) {
			IMatch match = candidate.getMatchList().getElement(i);
			try {

				double relativeIntensity = match.getMatchedPeak().getIntensity() * data.getRelativeFactor();

				MsMsCandidateFragment candidateFragment = new MsMsCandidateFragmentBuilder().
						withMass(match.getMatchedPeak().getMass())
						.withFormula(
						match.getModifiedFormulaStringOfBestMatchedFragment(candidate.getPrecursorMolecule()))
						.withSmiles(match.getBestMatchedFragment().getSmiles(candidate.getPrecursorMolecule()))
						.withPeak(new Peak(match.getMatchedPeak().getMass(), relativeIntensity, match.getMatchedPeak().getIntensity()))
						.withImage(null)
						.createMsMsCandidateFragment();

				candidateFragments.add(candidateFragment);


			} catch (RelativeIntensityNotDefinedException e) {
				LOGGER.error(e.getMessage());
			}
		}

		double score = (double) candidate.getProperty("Score");
		return new MsMsCandidate(data, new Score(score, 1.0, score),
				(String) candidate.getProperty("Identifier"),
				(String) candidate.getProperty("Identifier"),
				(String) candidate.getProperty("MolecularFormula"),
				(String) candidate.getProperty("InChI"),
				(String) candidate.getProperty("InChIKey2"),
				(String) candidate.getProperty("InChIKey1"),
				(double) candidate.getProperty("FragmenterScore"),
				(String) candidate.getProperty("FragmenterScore_Values"),
				data.getUsedPeaks(),
				candidateFragments);
	}

	// TODO Maybe this need to be the precurosr mass
	private void initSettingsLocal(MsMsData data, MsMsSettings settings, MetFragGlobalSettings globalSettings) {
		Feature feature = data.getFeature();

		SortedTandemMassPeakList peakList = new SortedTandemMassPeakList(feature.getNeutralMass());

		for (Peak peak : feature.getUsedPeaks()) {
			peakList.addElement(new TandemMassPeak(peak.getMz(), peak.getIntensity(),
					peak.getRelativeIntensity()));
		}

		globalSettings.set("PeakList", peakList);

		globalSettings.set("MoleculeInMemory", getAtomContainer(data.getCandidatesSmiles()));

		globalSettings.set("NeutralPrecursorMolecularFormula", feature.getNeutralFormula());
		globalSettings.set("FragmentPeakMatchAbsoluteMassDeviation", 0.001);
		globalSettings.set("FragmentPeakMatchRelativeMassDeviation",
				1.0 * settings.getPpmFragments());

		globalSettings.set("NeutralPrecursorMass", feature.getNeutralMass());
		globalSettings.set("DatabaseSearchRelativeMassDeviation", 1.0 * settings.getPpm());

		// Precursor ion type as nominal mass of the adduct (MetFrag Constants.ADDUCT_NOMINAL_MASSES):
		// 1 = [M+H]+, -1 = [M-H]-, 0 = [M]. Formerly 2 and 1 were used, read as list positions, but MetFrag
		// (2.3 and 2.6) reads them as [M+D]+ and [M+H]+; the charge was always positive (SP-07)
		int ionMode = 0;
		boolean positive = true;
		switch (settings.getIonisation()) {
		case NEGATIVE_IONISATION:
			ionMode = -1;
			positive = false;
			break;
		case POSITIVE_IONISATION:
			ionMode = 1;
			break;
		default:
			break;
		}
		globalSettings.set("PrecursorIonMode", ionMode);
		globalSettings.set("IsPositiveIonMode", positive);

		globalSettings.set("MetFragScoreTypes", new String[] { "FragmenterScore" });
		globalSettings.set("MetFragScoreWeights", new Double[] { 1.0 });
		globalSettings.set("MaximumTreeDepth", new Byte("2"));
		globalSettings.set("MetFragPreProcessingCandidateFilter", new String[] {});
		globalSettings.set("MetFragPostProcessingCandidateFilter", new String[] {});
		globalSettings.set("MetFragPreProcessingCandidateFilter",
				new String[] { "UnconnectedCompoundFilter" });
		globalSettings.set("MetFragPostProcessingCandidateFilter",
				new String[] { "InChIKeyFilter" });
	}

	private IAtomContainer[] getAtomContainer(List<String> smiles) {
		IAtomContainer[] container = new IAtomContainer[smiles.size()];
		IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
		SmilesParser smilesParser = new SmilesParser(builder);
		InChIGeneratorFactory inChIGeneratorFactory;

		try {
			inChIGeneratorFactory = InChIGeneratorFactory.getInstance();
		} catch (CDKException e) {
			LOGGER.error(e.getMessage());
			return container;
		}

		try {
			for (int i = 0; i < smiles.size(); i++) {
				container[i] = inChIGeneratorFactory.getInChIToStructure(inChIGeneratorFactory
						.getInChIGenerator(smilesParser.parseSmiles(smiles.get(i))).getInchi(),
						builder).getAtomContainer();
			}
		} catch (CDKException e) {
			LOGGER.error(e.getMessage());
		}

		return container;
	}

	private class Pair {
		private String key;
		private List<MsMsCandidate> candidates;

		Pair(String key, List<MsMsCandidate> candidates) {
			this.key = key;
			this.candidates = candidates;
		}

	}

}
