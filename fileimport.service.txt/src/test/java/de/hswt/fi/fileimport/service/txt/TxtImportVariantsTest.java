package de.hswt.fi.fileimport.service.txt;

import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Variants of the text format (SP-15): SCIEX OS report templates with and without Formula line, peak lists in other
 * lines, decimal commas, broken entries. Synthetic data.
 */
public class TxtImportVariantsTest {

	@TempDir
	Path directory;

	private List<Feature> importText(String content) throws IOException {
		Path file = directory.resolve("export.txt");
		Files.writeString(file, content.replace("\n", "\r\n"), StandardCharsets.UTF_8);
		FeatureSet featureSet = new SciexTxtFeatureContentImporter().importFromFile(file);
		return featureSet.getFeatures();
	}

	@Test
	public void standardBlockWithFormula() throws IOException {
		List<Feature> features = importText("""

				NAME: 144.982166 / 18.12
				RETENTIONTIME: 18.12
				PRECURSORMZ: 144.9822
				Formula: C3HF4P
				50.1992 3869 57.9352 6644 62.9293 5319464
				//
				""");
		assertEquals(1, features.size());
		Feature feature = features.get(0);
		assertEquals("144.982166 / 18.12", feature.getIdentifier());
		assertEquals(18.12, feature.getRetentionTime());
		assertEquals(144.9822, feature.getPrecursorMass());
		assertEquals("C3HF4P", feature.getNeutralFormula());
		assertEquals(3, feature.getPeaks().size());
		assertEquals(62.9293, feature.getPeaks().get(2).getMz());
		assertEquals(5319464, feature.getPeaks().get(2).getIntensity());
	}

	@Test
	public void withoutFormulaLineAndOnePeakPerLine() throws IOException {
		// SCIEX OS template "noSF": no Formula line; one peak per line
		List<Feature> features = importText("""
				NAME: 110.0605 / 8.39
				RETENTIONTIME: 8.420
				PRECURSORMZ: 110.0606
				65.0349 4
				65.0358 13
				66.0317 4
				//
				""");
		assertEquals(1, features.size());
		assertNull(features.get(0).getNeutralFormula());
		assertEquals(3, features.get(0).getPeaks().size());
		assertEquals(110.0606, features.get(0).getPrecursorMass());
	}

	@Test
	public void peaksInFormulaOrPrecursorLine() throws IOException {
		// Report templates that write the peak list right after the label
		List<Feature> features = importText("""
				NAME: A
				RETENTIONTIME: 1.5
				PRECURSORMZ: 200.1
				Formula: 50.1 300 51.2 20
				//
				NAME: B
				RETENTIONTIME: 2.5
				PRECURSORMZ: 300.2 60.1 100 61.2 50
				//
				""");
		assertEquals(2, features.size());
		assertNull(features.get(0).getNeutralFormula());
		assertEquals(2, features.get(0).getPeaks().size());
		assertEquals(300.2, features.get(1).getPrecursorMass());
		assertEquals(2, features.get(1).getPeaks().size());
	}

	@Test
	public void decimalCommaTabsAndColonInName() throws IOException {
		List<Feature> features = importText("""
				NAME: Cpd 1: 23.806
				RETENTIONTIME: 23,806
				PRECURSORMZ: 221,0353
				Formula:\tC9H8O5
				65,0349\t4\t66,0317\t12
				//
				""");
		assertEquals(1, features.size());
		Feature feature = features.get(0);
		assertEquals("Cpd 1: 23.806", feature.getIdentifier());
		assertEquals(23.806, feature.getRetentionTime());
		assertEquals(221.0353, feature.getPrecursorMass());
		assertEquals("C9H8O5", feature.getNeutralFormula());
		assertEquals(2, feature.getPeaks().size());
		assertEquals(66.0317, feature.getPeaks().get(1).getMz());
	}

	@Test
	public void noFormulaTextsAndNoPeaks() throws IOException {
		List<Feature> features = importText("""
				NAME: X
				RETENTIONTIME: 1
				PRECURSORMZ: 100
				Formula: No data for Formula Finder Composition Name xcm
				N/A
				//
				NAME: Y
				RETENTIONTIME: 2
				PRECURSORMZ: 101
				Formula: No formula found
				//
				""");
		assertEquals(2, features.size());
		assertNull(features.get(0).getNeutralFormula());
		assertTrue(features.get(0).getPeaks().isEmpty());
		assertNull(features.get(1).getNeutralFormula());
	}

	@Test
	public void brokenEntryIsSkippedNotTheWholeFile() throws IOException {
		List<Feature> features = importText("""
				NAME: without precursor
				RETENTIONTIME: 1
				Formula: C2H6O
				//
				NAME: fine
				RETENTIONTIME: 2
				PRECURSORMZ: 47.0491
				Formula: C2H6O
				10.1 x 12.2 5 13.3
				//
				NAME: last block without end marker
				RETENTIONTIME: 3
				PRECURSORMZ: 60.1
				""");
		assertEquals(2, features.size());
		assertEquals("fine", features.get(0).getIdentifier());
		// "10.1 x" is not readable, "12.2 5" is; the single "13.3" has no intensity
		assertEquals(1, features.get(0).getPeaks().size());
		assertEquals("last block without end marker", features.get(1).getIdentifier());
	}
}
