package de.hswt.fi.fileimport.service.excel;

import de.hswt.fi.common.PoiUtil;
import de.hswt.fi.common.RTCalculationUtil;
import de.hswt.fi.common.ValueFormatUtil;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.filehandler.definition.ColumnDefinition;
import de.hswt.filehandler.definition.SheetDefinition;
import de.hswt.filehandler.excel.ExcelReader;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Marco Luthardt
 */
public abstract class AbstractExcelReader extends ExcelReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExcelReader.class);

	private static final int MAX_TARGET_DATA_COUNT = 7500;

	protected abstract URL getConfigUrl();

	protected abstract String getTargetsSheetNameId();

	protected abstract String getTargetsIdentifierId();

	protected abstract String getTargetsMassId();

	protected abstract String getTargetsFormulaId();

	protected abstract String getTargetsRtId();

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.filehandler.api.Reader#getContent(java.nio.file.Path)
	 */
	@Override
	public FeatureSet getContent(Path path) {
			return new FeatureSet(path, getTargetData(path));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.filehandler.api.Reader#getContentClass()
	 */
	@Override
	public Class<?> getContentClass() {
		return FeatureSet.class;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.filehandler.excel.ExcelReader#getConfigFile()
	 */
	@Override
	protected Path getConfigFile() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.filehandler.excel.ExcelReader#getConfigFileAsStream()
	 */
	@Override
	protected InputStream getConfigFileAsStream() {
		URL url = getConfigUrl();
		try {
			return url.openStream();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Parses the target (search) data.
	 *
	 * @param path
	 *            the path to the file to parse
	 * @return the parsed target data
	 */
	private List<Feature> getTargetData(Path path) {

		SheetDefinition sheetDefinition = getRequiredSheets().get(getTargetsSheetNameId());
		if (sheetDefinition == null) {
			return Collections.emptyList();
		}

		Sheet sheet = getSheetFromNewWorkbook(path, sheetDefinition.getNameInExcelFile());
		if (sheet == null) {
			return Collections.emptyList();
		}

		List<Feature> features = getExtractFeatureData(sheetDefinition, sheet);

		try {
			sheet.getWorkbook().close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return features;
	}

	private List<Feature> getExtractFeatureData(SheetDefinition sheetDefinition, Sheet sheet) {

		List<Feature> results = new ArrayList<>();

		ColumnDefinition identifierCol = sheetDefinition.getIdentifierColumns()
				.get(getTargetsIdentifierId());
		ColumnDefinition rtColumn = sheetDefinition.getIdentifierColumns().get(getTargetsRtId());
		ColumnDefinition massCol = sheetDefinition.getIdentifierColumns().get(getTargetsMassId());
		ColumnDefinition formulaCol = getTargetsFormulaId() != null ?
				sheetDefinition.getIdentifierColumns().get(getTargetsFormulaId()) : null;

		if (identifierCol == null || rtColumn == null || massCol == null) {
			return Collections.emptyList();
		}

		int maxLength = MAX_TARGET_DATA_COUNT + sheetDefinition.getContentRow();

		int id = 1;

		for (int i = sheetDefinition.getContentRow(); i <= sheet.getLastRowNum()
				&& i < maxLength; i++) {
			String identifier;
			Double precursorMass;
			String neutralFormula = null;

			Row row = sheet.getRow(i);

			if (PoiUtil.isRowEmpty(row)) {
				break;
			}

			identifier = getStringCellValue(identifierCol, row);
			if (identifier.isEmpty()) {
				identifier = "SI generated " + id++;
			}

			precursorMass = ValueFormatUtil.roundMass(PoiUtil.getDoubleCellValue(row.getCell(massCol.getColumnIndex())));

			if (formulaCol != null) {
				neutralFormula = getStringCellValue(formulaCol, row);
			}

			// If no precursor mass and no formula is given skip feature
			if (precursorMass == null && formulaCol == null) {
				continue;
			}

			results.add(new Feature.Builder(identifier, precursorMass)
					.withRetentionTime(calculateMeanRT(rtColumn, row))
					.withNeutralFormula(neutralFormula)
					.build());

		}

		return results;
	}

	private String getStringCellValue(ColumnDefinition identifierCol, Row row) {
		return PoiUtil.getStringCellValue(row.getCell(identifierCol.getColumnIndex()));
	}

	private Double calculateMeanRT(ColumnDefinition rtColumn, Row row) {
		Collection<Integer> indices = Collections.emptyList();

		if (!rtColumn.getColumnIndices().isEmpty()) {
			indices = rtColumn.getColumnIndices().values();
		} else if (rtColumn.getColumnIndex() >= 0) {
			indices = Collections.singleton(rtColumn.getColumnIndex());
		}

		List<Double> rtValues = extractRTValues(row, indices);

		return RTCalculationUtil.getMeanRt(rtValues);
	}

	private List<Double> extractRTValues(Row row, Collection<Integer> indices) {

		List<Double> rtValues = new ArrayList<>();

		for (Integer values : indices) {
			Double rt = PoiUtil.getDoubleCellValue(row.getCell(values));
			if (rt != null) {
				rtValues.add(rt);
			}
		}

		return rtValues;
	}

}
