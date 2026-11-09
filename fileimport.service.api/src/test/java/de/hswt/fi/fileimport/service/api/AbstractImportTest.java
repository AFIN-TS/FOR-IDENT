package de.hswt.fi.fileimport.service.api;

import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.Peak;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
abstract public class AbstractImportTest {

	public abstract FileBasedFeatureContentImporter getFileImporter();

	public abstract FeatureSet createReferenceData();

	public abstract String getFileName();

	@Test
	public void contentImporterFound() {
		assertNotNull(getFileImporter());
	}

	@Test
	public void templateFileExists() throws URISyntaxException {
		assertTrue(getResourcePath().toFile().exists());
	}

	@Test
	public void featureAmountMatch() throws URISyntaxException {
		assertEquals(createReferenceData().getFeatures().size(), importContent().getFeatures().size());
	}

	@Test
	public void featureIdentifiersMatch() throws URISyntaxException {

		List<Feature> referenceFeatures = createReferenceData().getFeatures();
		List<Feature> testFeatures = importContent().getFeatures();

		System.out.println(referenceFeatures);
		System.out.println(testFeatures);

		referenceFeatures.forEach(feature ->
				assertTrue(testFeatures.stream().anyMatch(contentFeature ->
						contentFeature.getIdentifier().equals(feature.getIdentifier()))));
	}

	@Test
	public void featurePropertiesMatch() throws URISyntaxException {

		FeatureSet content = importContent();

		if (content == null) {
			fail();
			return;
		}

		List<Feature> referenceFeatures = createReferenceData().getFeatures();
		List<Feature> testFeatures = content.getFeatures();

		assertEquals(referenceFeatures.size(), testFeatures.size());

		referenceFeatures.forEach(feature -> feature.getPeaks().sort(Comparator.comparingDouble(Peak::getMz)));
		testFeatures.forEach(feature -> feature.getPeaks().sort(Comparator.comparingDouble(Peak::getMz)));

		testFeatures.forEach(System.out::println);
		
		for (Feature referenceFeature : referenceFeatures) {

			boolean foundEquivalent = false;

			for (Feature testFeature : testFeatures) {
				
				if (!referenceFeature.getIdentifier().equals(testFeature.getIdentifier())) {
					continue;
				}
				if (!compare(referenceFeature.getPrecursorMass(), testFeature.getPrecursorMass())) {
					continue;
				}

				if (!compare(referenceFeature.getRetentionTime(), testFeature.getRetentionTime())) {
					continue;
				}
				if (!compare(referenceFeature.getNeutralFormula(),
						testFeature.getNeutralFormula())) {
					continue;

				}
				if (referenceFeature.getPeaks().equals(testFeature.getPeaks())) {
					foundEquivalent = true;
				} else {
					System.out.println(referenceFeature.getPeaks());
					System.out.println(testFeature.getPeaks());
				}
				
			}
			if(!foundEquivalent) {
				System.out.println(" ----- " + referenceFeature);
			}
			assertTrue(foundEquivalent);
		}

	}

	private FeatureSet importContent() throws URISyntaxException {
		return getFileImporter().importFromFile(getResourcePath());
	}

	private Path getResourcePath() throws URISyntaxException {
		return Paths.get(this.getClass().getClassLoader().getResource(getFileName()).toURI());
	}

	private boolean compare(Double a, Double b) {
		if (a == null && b == null) {
			return true;
		} else if (a == null || b == null) {
			return false;
		} else {
			return Double.compare(a, b) == 0;
		}
	}

	private boolean compare(String a, String b) {
		if ((a == null && (b != null && b.isEmpty()))
				|| ((a != null && a.isEmpty()) && b == null)) {
			return true;
		} else {
			return StringUtils.equals(a, b);
		}
	}

	protected void addReferenceFeature(String identifier, double precursorMass, List<Feature> referenceFeatures) {
		Feature feature = new Feature.Builder(identifier, precursorMass).build();
		referenceFeatures.add(feature);
	}

	protected void addReferenceFeature(String identifier, double precursorMass, double retentionTime,
									   List<Feature> referenceFeatures) {
		Feature feature = new Feature.Builder(identifier, precursorMass)
				.withRetentionTime(retentionTime)
				.build();
		referenceFeatures.add(feature);
	}

	protected void addReferenceFeature(String identifier, double precursorMass, double retentionTime, String formula,
									   List<Feature> referenceFeatures) {
		Feature feature = new Feature.Builder(identifier, precursorMass)
				.withRetentionTime(retentionTime)
				.withNeutralFormula(formula)
				.build();

		referenceFeatures.add(feature);
	}

	protected void addReferenceFeature(String identifier, double precursorMass, double retentionTime,
									   List<Peak> peaks, List<Feature> referenceFeatures) {
		Feature feature = new Feature.Builder(identifier, precursorMass)
				.withRetentionTime(retentionTime)
				.withPeaks(peaks)
				.build();

		referenceFeatures.add(feature);
	}

	protected void addReferenceFeature(String identifier, double precursorMass, double retentionTime, String formula,
									   List<Peak> peaks, List<Feature> referenceFeatures) {
		Feature feature = new Feature.Builder(identifier, precursorMass)
				.withRetentionTime(retentionTime)
				.withNeutralFormula(formula)
				.withPeaks(peaks)
				.build();

		referenceFeatures.add(feature);
	}

	// mzIntensityValues must be tuples of mz and intensity values
	// mzIntensityValues = mz, intensity, mz, intensity, mz, intensity ...
	protected List<Peak> createPeakList(double... mzIntensityValues) {

		List<Peak> peaks = new ArrayList<>();

		for(int i = 0; i < mzIntensityValues.length -1; i += 2) {
			peaks.add(new Peak(mzIntensityValues[i], mzIntensityValues[i+1]));
		}

		return peaks;
	}

}
