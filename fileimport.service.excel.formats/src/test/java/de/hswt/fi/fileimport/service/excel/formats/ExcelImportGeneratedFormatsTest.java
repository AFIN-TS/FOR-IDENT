package de.hswt.fi.fileimport.service.excel.formats;

import de.hswt.fi.fileimport.service.excel.ExcelFeatureContentImporter;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.filehandler.excel.ExcelReader;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Excel formats without a sample file in the repository (SP-15): Thermo TraceFinder 3.2 and the generic
 * template ("Prototyp Letzel", sheet "targets"). The workbooks are generated with the columns of the format
 * configurations; synthetic data.
 */
public class ExcelImportGeneratedFormatsTest {

	@TempDir
	Path directory;

	@Test
	public void traceFinder32() throws IOException {
		Path file = directory.resolve("tracefinder.xlsx");
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Results");
			row(sheet, 0, "Match Result Name", "Formula", "m/z (Apex)", "RT (Measured)", "Area");
			row(sheet, 1, "Phenazone", "C11H12N2O", 189.1022, 5.85, 1000.0);
			row(sheet, 2, "Atrazine", "C8H14ClN5", 216.101, 9.12, 900.0);
			save(workbook, file);
		}
		List<Feature> features = importFile(file, new ThermoTraceFinder_3_2_ExcelReader());
		assertEquals(2, features.size());
		assertFeature(features.get(0), "Phenazone", 189.1022, 5.85, "C11H12N2O");
		assertFeature(features.get(1), "Atrazine", 216.101, 9.12, "C8H14ClN5");
	}

	@Test
	public void genericTemplate() throws IOException {
		Path file = directory.resolve("template.xls");
		try (Workbook workbook = new HSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("targets");
			// Header, two rows for notes, data from the fourth row (as in the template)
			row(sheet, 0, "identifier", "accurate mass", "elemental formula", "rt1", "rt2");
			row(sheet, 3, "Unknown Y", 164.095, "C10H12O2", 23.5, 23.7);
			row(sheet, 4, "Unknown Z", 188.0950, "", 5.8, 5.9);
			save(workbook, file);
		}
		List<Feature> features = importFile(file, new PrototypLetzelRTIExcelReader());
		assertEquals(2, features.size());
		assertEquals("Unknown Y", features.get(0).getIdentifier());
		assertEquals(164.095, features.get(0).getPrecursorMass(), 1e-9);
		assertEquals("C10H12O2", features.get(0).getNeutralFormula());
		// The mean of the retention times
		assertEquals(23.6, features.get(0).getRetentionTime(), 1e-9);
		assertEquals("Unknown Z", features.get(1).getIdentifier());
	}

	private List<Feature> importFile(Path file, ExcelReader reader) {
		ExcelFeatureContentImporter importer = new ExcelFeatureContentImporter(List.of(reader),
				List.of(new CalibrationExcelReaderLC()));
		FeatureSet featureSet = importer.importFromFile(file);
		return featureSet.getFeatures();
	}

	private static void row(Sheet sheet, int index, Object... values) {
		Row row = sheet.createRow(index);
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof Double number) {
				row.createCell(i).setCellValue(number);
			} else {
				row.createCell(i).setCellValue(String.valueOf(values[i]));
			}
		}
	}

	private static void save(Workbook workbook, Path file) throws IOException {
		try (OutputStream out = Files.newOutputStream(file)) {
			workbook.write(out);
		}
	}

	private static void assertFeature(Feature feature, String name, double mass, double rt, String formula) {
		assertEquals(name, feature.getIdentifier());
		assertEquals(mass, feature.getPrecursorMass(), 1e-9);
		assertEquals(rt, feature.getRetentionTime(), 1e-9);
		assertEquals(formula, feature.getNeutralFormula());
	}
}
