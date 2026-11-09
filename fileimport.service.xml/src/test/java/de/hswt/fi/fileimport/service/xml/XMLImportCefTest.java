package de.hswt.fi.fileimport.service.xml;

import de.hswt.fi.fileimport.service.api.AbstractImportTest;
import de.hswt.fi.fileimport.service.api.FileBasedFeatureContentImporter;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;

import java.util.ArrayList;
import java.util.List;

public class XMLImportCefTest extends AbstractImportTest {

	private static final String TEST_FILE_NAME = "exampleCef.cef";

	@Override
	public FileBasedFeatureContentImporter getFileImporter() {
		return new CefFeatureContentImporter();
	}

	@Override
	public FeatureSet createReferenceData() {

		List<Feature> referenceFeatures = new ArrayList<>();

		addReferenceFeature("mz 342.14685", 342.14685, 3.558,
				createPeakList(
						56.9648, 21883.81,
						61.0075, 165084.31,
						70.0127, 27859.50,
						82.9967, 112645.14,
						51.9844, 43.15,
						75.6442, 19.29,
						87.0038, 355.86,
						87.0293, 11.45,
						35.7562, 25.68,
						56.0506, 181.28,
						69.0316, 148.39,
						96.0796, 45.41,
						42.8893, 28.57,
						51.9392, 47.93,
						57.2935, 23.01,
						84.0823, 117.16), referenceFeatures);

		addReferenceFeature("mz 172.13333", 172.13333, 4.099,
				createPeakList(
						56.9646, 21813.49,
						61.0075, 176078.44,
						67.9961, 13001.59,
						68.9938, 311012.81,
						55.0173, 103.50,
						67.0542, 106.89,
						70.0122, 30.12,
						71.9287, 47.52,
						43.0184, 24.22,
						54.9468, 16.63,
						55.0050, 12.01,
						39.0173, 35.02,
						41.0378, 273.19,
						44.0497, 51.60,
						43.0527, 44.35), referenceFeatures);

		addReferenceFeature("mz 160.13294", 160.13294, 4.758,
				createPeakList(
						56.9645, 23601.30,
						61.0074, 177187.28,
						67.9959, 13937.86,
						68.9937, 324938.28,
						43.0525, 38.87,
						55.0183, 62.78,
						55.0535, 1037.43,
						40.9697, 53.10,
						42.0320, 40.23,
						39.0216, 91.54,
						41.0546, 10.03,
						43.0161, 21.76,
						41.0373, 235.565,
						43.016850000000005, 151.885
						), referenceFeatures);

		return new FeatureSet(TEST_FILE_NAME, referenceFeatures);
	}

	@Override
	public String getFileName() {
		return TEST_FILE_NAME;
	}
}
