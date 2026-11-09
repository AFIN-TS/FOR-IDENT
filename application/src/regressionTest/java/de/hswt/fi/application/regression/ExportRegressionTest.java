package de.hswt.fi.application.regression;

import de.hswt.fi.beans.BeanComponentMapper;
import de.hswt.fi.export.service.excel.ExcelCreator;
import de.hswt.fi.export.service.excel.ExcelFileDefinition;
import de.hswt.fi.export.service.excel.ExcelSheetDefinition;
import de.hswt.fi.fileimport.service.api.FeatureContentImporter;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.processing.service.model.ProcessCandidate;
import de.hswt.fi.processing.service.model.ProcessingJob;
import de.hswt.fi.processing.service.model.ProcessingResult;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Exports processing results to Excel with all selectable columns (the default of the download
 * dialog) and compares the cell texts of all sheets with the golden files in golden/export.
 * Embedded structure images are not compared.
 */
public class ExportRegressionTest extends AbstractRegressionTest {

	private static final List<String> EXPORTED_INPUTS = Arrays.asList("excel-data-pos-rti", "txt-01-suspect", "cef-example");

	@Autowired
	private FeatureContentImporter importer;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private List<CompoundSearchService> compoundSearchServices;

	@Autowired
	private BeanComponentMapper mapper;

	@Test
	public void excelExportMatchesGoldenFiles() throws Exception {
		for (RegressionInput input : RegressionInput.ALL) {
			if (!EXPORTED_INPUTS.contains(input.id)) {
				continue;
			}
			FeatureSet featureSet = ImportRegressionTest.importFile(importer, input).get();
			ProcessingJob job = RegressionProcessing.createJob(context, featureSet, input, compoundSearchServices);
			ProcessingResult result = RegressionProcessing.process(context, job);
			String text = cellTexts(export(result));
			checkGolden("export/" + input.id + ".txt", text);
			// Repeated exports of the same result must be identical (detects nondeterministic export)
			for (int i = 2; i <= EXPORT_REPEAT; i++) {
				checkEqual("export-repeat/" + input.id + "-" + i + ".txt", text, cellTexts(export(result)));
			}
		}
	}

	private static final int EXPORT_REPEAT = Integer.getInteger("regression.exportRepeat", 1);

	private Workbook export(ProcessingResult result) {
		// Deterministic order (the order of the processing result depends on the parallel mass search)
		List<ProcessCandidate> candidates = new ArrayList<>(result.getResults());
		candidates.sort(Snapshots.candidateOrder(new Snapshots.Identifiers()));

		ExcelSheetDefinition<ProcessCandidate> sheet = new ExcelSheetDefinition<>("results");
		sheet.setData(candidates);
		sheet.setColumnPropertyIds(candidates.isEmpty() ? Collections.emptyList()
				: mapper.getSelectorColumns(candidates.get(0)));

		ExcelFileDefinition definition = new ExcelFileDefinition();
		definition.setFilename("regression");
		definition.setSheets(Collections.singletonList(sheet));
		return context.getBean(ExcelCreator.class).createWorkbook(definition);
	}

	/** All cell texts as shown by Excel (tab separated), so formatting changes are detected too. */
	private static String cellTexts(Workbook workbook) {
		DataFormatter formatter = new DataFormatter();
		StringBuilder text = new StringBuilder();
		for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
			Sheet sheet = workbook.getSheetAt(s);
			text.append("# sheet ").append(sheet.getSheetName()).append('\n');
			for (Row row : sheet) {
				List<String> cells = new ArrayList<>();
				for (int c = 0; c < row.getLastCellNum(); c++) {
					Cell cell = row.getCell(c);
					cells.add(cell == null ? "" : formatter.formatCellValue(cell).replace('\n', ' '));
				}
				text.append(String.join("\t", cells)).append('\n');
			}
		}
		return text.toString();
	}
}
