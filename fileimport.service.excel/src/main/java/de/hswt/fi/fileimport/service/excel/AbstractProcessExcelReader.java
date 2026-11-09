package de.hswt.fi.fileimport.service.excel;

import de.hswt.fi.common.PoiUtil;
import de.hswt.fi.common.RTCalculationUtil;
import de.hswt.fi.common.ValueFormatUtil;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.Peak;
import de.hswt.fi.model.RTICalibrationData;
import de.hswt.filehandler.definition.ColumnDefinition;
import de.hswt.filehandler.definition.SheetDefinition;
import de.hswt.filehandler.excel.ExcelReader;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

/**
 *
 * @author Marco Luthardt
 *
 */
public abstract class AbstractProcessExcelReader extends ExcelReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessExcelReader.class);

	private static final int MAX_TARGET_DATA_COUNT = 5000;

	private static final int MAX_CALIBRATION_DATA_COUNT = 100;

	protected abstract URL getConfigUrl();

	protected abstract String getPeakListSheetNameId();

	protected abstract String getPeakListNamesId();

	protected abstract String getPeakListTargetNamesId();

	protected abstract String getPeakListMassId();

	protected abstract String getPeakListFormulaId();

	protected abstract String getPeakRtId();

	protected abstract String getCalibrationSheetNameId();

	protected abstract String getCalibrationSubstanceId();

	protected abstract String getCalibrationSheetRtId();

	protected abstract String getCalibrationLogDId();

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.filehandler.api.Reader#getContent(java.nio.file.Path)
	 */
	@Override
	public FeatureSet getContent(Path file) {
		FeatureSet featureSet = new FeatureSet(file, getPeaks(file));
		featureSet.setRtiCalibrationData(getCalibrationData(file));
		return featureSet;
	}

	private List<RTICalibrationData> getCalibrationData(Path path) {

		SheetDefinition sheetDefinition = getOptionalSheets().get(getCalibrationSheetNameId());

		if (sheetDefinition == null || sheetDefinition.getNameInExcelFile() == null) {
			return Collections.emptyList();
		}

		Sheet sheet = getSheetFromNewWorkbook(path, sheetDefinition.getNameInExcelFile());

		if (sheet == null) {
			return Collections.emptyList();
		}

		List<RTICalibrationData> features = extractRTICalculationData(sheetDefinition, sheet);

		try {
			sheet.getWorkbook().close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return features;
	}

	private List<RTICalibrationData> extractRTICalculationData(SheetDefinition sheetDefinition, Sheet sheet) {

		ColumnDefinition substanceCol = sheetDefinition.getIdentifierColumns().get(getCalibrationSubstanceId());
		ColumnDefinition rtColumn = sheetDefinition.getIdentifierColumns().get(getCalibrationSheetRtId());
		ColumnDefinition logdCol = sheetDefinition.getIdentifierColumns().get(getCalibrationLogDId());

		if (substanceCol == null || rtColumn == null || !rtColumn.getMultiple() || logdCol == null) {
			return Collections.emptyList();
		}

		List<RTICalibrationData> results = new ArrayList<>();

		int maxLength = MAX_CALIBRATION_DATA_COUNT + sheetDefinition.getContentRow();

		for (int i = sheetDefinition.getContentRow(); i <= sheet.getLastRowNum() && i < maxLength; i++) {
			String name;
			Double logD;
			Row row = sheet.getRow(i);

			name = PoiUtil.getStringCellValue(row.getCell(substanceCol.getColumnIndex()));
			logD = ValueFormatUtil.roundLogD(PoiUtil.getDoubleCellValue(row.getCell(logdCol.getColumnIndex())));

			if (name == null || name.isEmpty() || logD == null) {
				continue;
			}

			RTICalibrationData calibration = new RTICalibrationData(name, logD);

			Map<String, Integer> indices = rtColumn.getColumnIndices();
			List<Double> rtValues = extractRTValues(row, indices.values());

			Double meanRT = RTCalculationUtil.getMeanRt(rtValues);

			if (meanRT != null) {
				calibration.setMeanRt(meanRT);
			}

			if (!rtValues.isEmpty()) {
				results.add(calibration);
			}
		}

		return results;
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
			LOGGER.error("An error occured {}", e);
			return null;
		}
	}

	private List<Feature> getPeaks(Path path) {
		List<Feature> results = new ArrayList<>();

		SheetDefinition sheetDefinition = getRequiredSheets().get(getPeakListSheetNameId());

		if (sheetDefinition == null) {
			return Collections.emptyList();
		}

		Workbook workbook = getNewWorkbook(path);
		Sheet sheet = workbook.getSheet(sheetDefinition.getNameInExcelFile());

		if (sheet == null) {
			return results;
		}

		return extractPeakData(sheetDefinition, workbook, sheet);
	}

	private List<Feature> extractPeakData(SheetDefinition sheetDefinition, Workbook workbook, Sheet sheet) {

		List<Feature> results = new ArrayList<>();
		int maxLength = MAX_TARGET_DATA_COUNT + sheetDefinition.getContentRow();

		ColumnDefinition peakNamesCol = sheetDefinition.getIdentifierColumns().get(getPeakListNamesId());
		ColumnDefinition targetNamesCol = sheetDefinition.getIdentifierColumns().get(getPeakListTargetNamesId());
		ColumnDefinition massCol = sheetDefinition.getIdentifierColumns().get(getPeakListMassId());
		ColumnDefinition formulaCol = sheetDefinition.getIdentifierColumns().get(getPeakListFormulaId());

		for (int i = sheetDefinition.getContentRow(); i <= sheet.getLastRowNum() && i < maxLength; i++) {
			String name = null;
			String targetName = null;
			String formula;
			Double mass = null;
			Row row = sheet.getRow(i);

			if (row != null) {
				name = PoiUtil.getStringCellValue(row.getCell(peakNamesCol.getColumnIndex()));
				targetName = PoiUtil.getStringCellValue(row.getCell(targetNamesCol.getColumnIndex()));
				mass = PoiUtil.getDoubleCellValue(row.getCell(massCol.getColumnIndex()));
			}

			if (name == null || name.isEmpty() || targetName == null || targetName.isEmpty() || mass == null) {
				continue;
			}

			formula = PoiUtil.getStringCellValue(row.getCell(formulaCol.getColumnIndex()));

			Feature processingData = getProcessingData(workbook, name, targetName, mass, formula);

			ColumnDefinition rtColumn = sheetDefinition.getIdentifierColumns().get(getPeakRtId());

			if (processingData != null) {
				processingData.setRetentionTime(calculateMeanRT(rtColumn, row));
				results.add(processingData);
			}
		}

		return results;
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

	private Feature getProcessingData(Workbook workbook, String sheetName, String targetName, double precursorMass, String formula) {
		Sheet sheet;
		try {
			sheet = workbook.getSheet(sheetName);
		} catch (EncryptedDocumentException e) {
			LOGGER.error("An error occured {}", e);
			return null;
		}

		if (sheet == null) {
			return null;
		}

		int maxLength = MAX_TARGET_DATA_COUNT + 1;

		List<Peak> peaks = new ArrayList<>();

		for (int i = 0; i <= sheet.getLastRowNum() && i < maxLength; i++) {
			Row row = sheet.getRow(i);

			Double mz = PoiUtil.getDoubleCellValue(row.getCell(0));
			Double intensity = PoiUtil.getDoubleCellValue(row.getCell(2));

			if (mz != null && intensity != null) {
				peaks.add(new Peak(mz, intensity));
			}
		}

		return new Feature.Builder(targetName, precursorMass)
				.withNeutralFormula(formula)
				.withPeaks(peaks)
				.build();
	}
}
