package de.hswt.fi.service.importer.csv;

import de.hswt.fi.fileimport.service.api.FileBasedFeatureContentImporter;
import de.hswt.fi.model.Feature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The CSV formats (SP-15: there were no tests): Bruker, Thermo Compound Discoverer 2.x and Thermo TraceFinder. They
 * are recognised by their header line and read by column position. Synthetic data.
 */
public class CsvImportTest {

	@TempDir
	Path directory;

	private Path write(String name, String content) throws IOException {
		Path file = directory.resolve(name);
		Files.writeString(file, content, StandardCharsets.UTF_8);
		return file;
	}

	@Test
	public void bruker() throws IOException {
		Path file = write("bruker.csv", """
				#, RT [min], Area, Cmpd.SF, Max. m/z, Prec. m/z
				1,4.53,120000,C5 H9 N O,100.0758,100.0758
				2,7.22,95000,C9 H15 N O,154.1226,154.1226
				""");
		BrukerCsvContentImporter importer = new BrukerCsvContentImporter();
		assertTrue(importer.canHandle(file));
		List<Feature> features = importer.importFromFile(file).getFeatures();
		assertEquals(2, features.size());
		assertFeature(features.get(0), "1", 100.0758, 4.53, "C5H9NO");
		assertFeature(features.get(1), "2", 154.1226, 7.22, "C9H15NO");
	}

	@Test
	public void compoundDiscoverer2() throws IOException {
		// Tab-separated with quotes; the first column (e.g. "Checked") comes before "Name"
		Path file = write("cd.csv", """
				"Checked"\t"Name"\t"Predicted Formula"\t"Molecular Weight"\t"RT [min]"\t"Area (Max.)"
				"False"\t"Phenazone"\t"C11H12N2O"\t"188.0950"\t"5.85"\t"1000"
				"False"\t"Unknown"\t"C10H9N3O"\t"187.0746"\t"5.76"\t"900"
				""");
		ThermoCompoundDiscovererCsvContentImporter importer = new ThermoCompoundDiscovererCsvContentImporter();
		assertTrue(importer.canHandle(file));
		List<Feature> features = importer.importFromFile(file).getFeatures();
		assertEquals(2, features.size());
		// Names are generated, the mass is the neutral molecular weight
		assertFeature(features.get(0), "SI generated 0", 188.0950, 5.85, "C11H12N2O");
		assertFeature(features.get(1), "SI generated 1", 187.0746, 5.76, "C10H9N3O");
	}

	@Test
	public void traceFinder() throws IOException {
		Path file = write("tracefinder.csv", """
				Sample Name,Database Name,MZ,IP,FI,LS,Compound Name,Formula,Adduct,Extracted Mass,Observed Mass,Mass Error,Peak Area,Expected RT,Observed RT
				S1,DB,1,1,1,1,"Phenazone, metabolite",C11H12N2O,M+H,189.1022,189.1020,-1.1,5000,5.80,5.85
				S1,DB,1,1,1,1,Atrazine,C8H14ClN5,M+H,216.1010,216.1012,0.9,4000,9.10,9.12
				""");
		ThermoTraceFinderCsvContentImporter importer = new ThermoTraceFinderCsvContentImporter();
		assertTrue(importer.canHandle(file));
		List<Feature> features = importer.importFromFile(file).getFeatures();
		assertEquals(2, features.size());
		// m/z from "Extracted Mass", RT from "Observed RT"; a comma inside quotes does not split
		assertFeature(features.get(0), "SI generated 0", 189.1022, 5.85, "C11H12N2O");
		assertFeature(features.get(1), "SI generated 1", 216.1010, 9.12, "C8H14ClN5");
	}

	@Test
	public void otherCsvFilesAreNotClaimed() throws IOException {
		Path file = write("other.csv", "Name,Mass,RT\nA,100,1\n");
		for (FileBasedFeatureContentImporter importer : List.of(new BrukerCsvContentImporter(),
				new ThermoCompoundDiscovererCsvContentImporter(), new ThermoTraceFinderCsvContentImporter())) {
			assertFalse(importer.canHandle(file), importer.getClass().getSimpleName());
		}
	}

	private static void assertFeature(Feature feature, String name, double mass, double rt, String formula) {
		assertEquals(name, feature.getIdentifier());
		assertEquals(mass, feature.getPrecursorMass(), 1e-9);
		assertEquals(rt, feature.getRetentionTime(), 1e-9);
		assertEquals(formula, feature.getNeutralFormula());
	}
}
