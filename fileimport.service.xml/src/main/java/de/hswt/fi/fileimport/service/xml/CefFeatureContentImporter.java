package de.hswt.fi.fileimport.service.xml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.hswt.fi.common.FileUtil;
import de.hswt.fi.common.SearchUtil;
import de.hswt.fi.fileimport.service.api.FileBasedFeatureContentImporter;
import de.hswt.fi.fileimport.service.xml.model.CefFile;
import de.hswt.fi.fileimport.service.xml.model.Compound;
import de.hswt.fi.fileimport.service.xml.model.Spectrum;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.Peak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class CefFeatureContentImporter implements FileBasedFeatureContentImporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(CefFeatureContentImporter.class);

	private static final int PPM_PEAKS = 10;

	// Jackson 2 with the settings of the XML converter of Spring Boot 2.7 that was used before (SP-10: Spring Boot 4
	// only provides Jackson 3)
	private final XmlMapper xmlMapper = XmlMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
			.build();

	@Override
	public FeatureSet importFromFile(Path contentPath) {

		CefFile cefFile;
		try {
			cefFile = xmlMapper.readValue(contentPath.toFile(), CefFile.class);
		} catch (IOException e) {
			LOGGER.error("Can not parse given content as XML {}", e);
			return null;
		}

		List<Feature> features = new ArrayList<>();
		for (Compound compound : cefFile.getCompounds()) {

			LOGGER.debug("Compound :{}", compound);

			Feature feature = getFeature(compound);
			if (feature != null) {
				features.add(feature);
			}
		}

		return new FeatureSet(contentPath, features);
	}

	@Override
	public boolean canHandle(Path path) {
		return path.endsWith(".cef") || FileUtil.readLines(path, 2).stream().filter(s -> s.contains("CEF")).count() == 1;
	}

	private Feature getFeature(Compound compound) {
		Double precursorMass;

		Optional<Spectrum> spectrum = compound.getSpectrum().stream().filter(s -> s.getType().contains("MS2")).findFirst();

		// Extract precursor Mass
		if (spectrum.isPresent() && spectrum.get().getMzOfInterest() != null) {
			precursorMass = spectrum.get().getMzOfInterest().getMz();

			// Case MolecularFeature
		} else if (compound.getLocation().getM() != null) {
			precursorMass = compound.getLocation().getM();
		} else {
			return null;
		}

		Feature.Builder featureBuilder = new Feature.Builder("mz " + precursorMass, precursorMass);

		if (compound.getLocation() != null && compound.getLocation().getRt() != null) {
			featureBuilder.withRetentionTime(compound.getLocation().getRt());
		}

		// If Formula given (Target approach) use exact formula Mass
		if (compoundHasValidFormula(compound)) {
			String formula = compound.getResults().getMolecule().getFormula();
			formula = formula.replaceAll("\\s+", "");
			featureBuilder.withNeutralFormula(formula);
		}

		// Extract MS2 fragments
		if (spectrum.isPresent() && precursorMass != null) {
			List<Peak> peaks = getPeaks(compound.getSpectrum(), precursorMass);
			featureBuilder.withPeaks(peaks);
		}

		return featureBuilder.build();

	}

	private boolean compoundHasValidFormula(Compound compound) {
		return compound.getResults() != null && compound.getResults().getMolecule() != null
				&& compound.getResults().getMolecule().getFormula() != null && !compound.getResults().getMolecule().getFormula().isEmpty();
	}

	private List<Peak> getPeaks(List<Spectrum> spectra, double precursorMass) {
		List<List<Peak>> spectraPeaks = new ArrayList<>();
		for (Spectrum spectrum : spectra) {
			if (spectrum.getPeaks() != null) {
				List<Peak> peaks = new ArrayList<>();
				spectrum.getPeaks().forEach(p -> peaks.add(new Peak(p.getX(), p.getY(), 0.0)));
				spectraPeaks.add(peaks);
			}
		}
		return mergePeaks(spectraPeaks, precursorMass);
	}

	private List<Peak> mergePeaks(List<List<Peak>> peaks, double precursorMass) {

		List<Peak> resultPeaks = new ArrayList<>();

		List<Peak> allPeaks = peaks.stream()
				.flatMap(List::stream)
				.filter(p -> p.getMz() <= precursorMass)
				.sorted(Comparator.comparingDouble(Peak::getMz))
				.collect(Collectors.toList());

		List<List<Peak>> clusterList = new ArrayList<>();

		for(Peak currentPeak : allPeaks) {
			double currentDelta = SearchUtil.getRangeFromPPM(currentPeak.getMz(), PPM_PEAKS);
			List<Peak> clusteredPeaks =
					allPeaks.stream()
					.filter(p -> p.getMz() >= currentPeak.getMz() && p.getMz() < (currentPeak.getMz() + currentDelta))
					.collect(Collectors.toList());

			clusterList.add(clusteredPeaks);
		}

		clusterList.sort(Comparator.comparingInt(value -> -value.size()));

		for (List<Peak> cluster : clusterList) {
			if (allPeaks.containsAll(cluster)) {
				resultPeaks.add(createMergedPeak(cluster));
				allPeaks.removeAll(cluster);
			}
		}

		return resultPeaks;
	}

	private Peak createMergedPeak(List<Peak> peaks) {
		double sumIntensity = peaks.stream().mapToDouble(Peak::getIntensity).sum();
		double sumRelIntensity = peaks.stream().mapToDouble(Peak::getRelativeIntensity).sum();
		double sumMz = peaks.stream().mapToDouble(Peak::getMz).sum();
		return new Peak(sumMz / peaks.size(), sumIntensity / peaks.size(), sumRelIntensity / peaks.size());
	}

}
