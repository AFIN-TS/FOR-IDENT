package de.hswt.fi.processing.service.def;

import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.Peak;
import de.hswt.fi.processing.service.model.ProcessingJob;
import de.hswt.fi.processing.service.model.ProcessingSettings;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class PreProcessingService {

	public void updateFeatures(ProcessingJob job) {

        ProcessingSettings settings = job.getSettings();

        updateFeaturesNeutralMass(job.getFeatureSet(), settings);
        settings.setPrecursorPpm(settings.getRequestedPrecursorPpm());

        if (job.getSettings().getIntensityThreshold() != 0d) {
            filterIntensitiesBelowThreshold(job);
        }

        initRelativeIntensity(job);
    }

    private void filterIntensitiesBelowThreshold(ProcessingJob job) {
        job.getFeatureSet().getFeatures().forEach(f -> filterIntensitiesBelowThreshold(f, job.getSettings()
                .getIntensityThreshold()));
    }

    private void filterIntensitiesBelowThreshold(Feature feature, double threshold) {
        // Keep the original peaks, only the used peaks are filtered
        feature.setUsedPeaks(new ArrayList<>(feature.getPeaks()));
        List<Peak> belowThreshold = feature.getPeaks().stream().filter(p -> p.getIntensity() < threshold)
                .collect(Collectors.toList());
        feature.getUsedPeaks().removeAll(belowThreshold);
    }

    private void updateFeaturesNeutralMass(FeatureSet featureSet, ProcessingSettings settings) {
        for (Feature feature : featureSet.getFeatures()) {
            // The formula mass (exact, with its own ppm) is used only if the run uses the formulae of the file
            feature.setMassCalculated(settings.isUseFormulae() && feature.getFormulaDerivedMass() != null);
            Double neutralMass;
            if (feature.isMassCalculated() && feature.getFormulaDerivedMass() != null) {
                neutralMass = feature.getFormulaDerivedMass();
            } else {
                neutralMass = feature.getPrecursorMass() + settings.getIonisation().getIonisation();
            }
            feature.setNeutralMass(neutralMass);
        }
    }

    private void initRelativeIntensity(ProcessingJob job) {
        job.getFeatureSet().getFeatures().forEach(this::initRelativeIntensity);
    }

    private void initRelativeIntensity(Feature feature) {

        Optional<Peak> maxPeak = feature.getUsedPeaks().stream().max(Comparator.comparingDouble(Peak::getIntensity));

        Optional<Peak> thresholdPeak = feature.getUsedPeaks().stream().filter(p -> p.getIntensity() >= 100).findAny();

        if (thresholdPeak.isPresent() && maxPeak.isPresent()) {
            double max = maxPeak.get().getIntensity();
            feature.getUsedPeaks().forEach(p -> p.setRelativeIntensity(p.getIntensity() * 1000 / max));
            feature.setRelativeFactor(max / 1000.0);
        }
    }
}
