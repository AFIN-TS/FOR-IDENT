package de.hswt.fi.application.regression;

import de.hswt.fi.fileimport.service.api.FeatureContentImporter;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.processing.service.model.ProcessingJob;
import de.hswt.fi.processing.service.model.ProcessingResult;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;

/**
 * Runs the complete processing (mass screening, RTI screening, MS/MS with MetFrag, MassBank)
 * against all four compound databases, like the UI does with its default settings, and compares
 * the scored candidates with the golden files in golden/processing.
 */
public class ProcessingRegressionTest extends AbstractRegressionTest {

	@Autowired
	private FeatureContentImporter importer;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private List<CompoundSearchService> compoundSearchServices;

	@Test
	public void processingResultsMatchGoldenFiles() throws Exception {
		for (RegressionInput input : RegressionInput.ALL) {
			if (!input.process) {
				continue;
			}
			Optional<FeatureSet> featureSet = ImportRegressionTest.importFile(importer, input);
			if (!featureSet.isPresent()) {
				checkGolden("processing/" + input.id + ".json", "not importable\n");
				continue;
			}
			ProcessingJob job = RegressionProcessing.createJob(context, featureSet.get(), input, compoundSearchServices);
			ProcessingResult result = RegressionProcessing.process(context, job);
			checkGolden("processing/" + input.id + ".json",
					Snapshots.toJson(Snapshots.processing(result, new Snapshots.Identifiers())));
		}
	}
}
