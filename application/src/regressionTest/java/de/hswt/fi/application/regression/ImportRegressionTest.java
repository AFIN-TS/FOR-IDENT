package de.hswt.fi.application.regression;

import de.hswt.fi.fileimport.service.api.FeatureContentImporter;
import de.hswt.fi.model.FeatureSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Imports every sample file and compares the imported features (and RTI calibration) with the
 * golden files in golden/import.
 */
public class ImportRegressionTest extends AbstractRegressionTest {

	@Autowired
	private FeatureContentImporter importer;

	@Test
	public void importedFeaturesMatchGoldenFiles() throws Exception {
		for (RegressionInput input : RegressionInput.ALL) {
			Optional<FeatureSet> featureSet = importFile(importer, input);
			String content = featureSet.isPresent()
					? Snapshots.toJson(Snapshots.featureSet(featureSet.get(), new Snapshots.Identifiers()))
					: "not importable\n";
			checkGolden("import/" + input.id + ".json", content);
		}
	}

	/**
	 * Imports the input file and fails if the import changed it. Before SP-03b the Excel readers
	 * opened .xlsx files writable, and closing the POI workbook wrote them back to disk.
	 */
	static Optional<FeatureSet> importFile(FeatureContentImporter importer, RegressionInput input) throws Exception {
		Path file = input.path(ROOT_DIR);
		Path calibration = input.calibrationPath(ROOT_DIR);
		String before = checksum(file) + (calibration == null ? "" : checksum(calibration));
		Optional<FeatureSet> featureSet = calibration == null
				? importer.importFromFile(file)
				: importer.importFromFileWithCalibrationData(file, calibration);
		String after = checksum(file) + (calibration == null ? "" : checksum(calibration));
		if (!before.equals(after)) {
			throw new IllegalStateException("The import changed the input file " + input.file);
		}
		return featureSet;
	}

	private static String checksum(Path file) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
		return HexFormat.of().withUpperCase().formatHex(digest);
	}
}
