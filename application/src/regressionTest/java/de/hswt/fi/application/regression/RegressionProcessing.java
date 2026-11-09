package de.hswt.fi.application.regression;

import de.hswt.fi.common.StationaryPhase;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.processing.service.api.ProcessingService;
import de.hswt.fi.processing.service.model.ProcessingJob;
import de.hswt.fi.processing.service.model.ProcessingResult;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs a processing job the way the UI does with its default settings, against all compound
 * databases.
 */
final class RegressionProcessing {

	private RegressionProcessing() {
	}

	static ProcessingJob createJob(ApplicationContext context, FeatureSet featureSet, RegressionInput input,
			List<CompoundSearchService> compoundSearchServices) {
		ProcessingService processingService = context.getBean(ProcessingService.class);
		ProcessingJob job = processingService.getProcessingJob(featureSet);
		// UI defaults: pH 3, 5 ppm precursor and fragments; ionisation and stationary phase must be chosen
		job.getSettings().setIonisation(input.ionisation);
		job.getSettings().setStationaryPhase(StationaryPhase.C18_POLAR_ENDCAPPED);
		job.setRequestedProcessUnits(processingService.getAvailableProcessingUnits(job));
		List<CompoundSearchService> services = new ArrayList<>(compoundSearchServices);
		services.sort(Comparator.comparing(CompoundSearchService::getDatasourceName));
		job.setSelectedSearchServices(services);
		return job;
	}

	static ProcessingResult process(ApplicationContext context, ProcessingJob job) {
		// ProcessingService is prototype scoped, the UI also uses a new instance per job
		return context.getBean(ProcessingService.class).executeJob(job, null);
	}
}
