package de.hswt.fi.fileimport.service.excel;

import de.hswt.fi.common.PoiUtil;
import de.hswt.fi.common.RTCalculationUtil;
import de.hswt.fi.common.ValueFormatUtil;
import de.hswt.fi.model.RTICalibrationData;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Marco Luthardt
 *
 */
public abstract class AbstractCalibrationExcelReader extends ExcelReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCalibrationExcelReader.class);

	private static final int MAX_CALIBRATION_DATA_COUNT = 100;

	protected abstract URL getConfigUrl();

	protected abstract String getCalibrationSheetNameId();

	protected abstract String getCalibrationSubstanceId();

	protected abstract String getCalibrationRtId();

	protected abstract String getCalibrationSignalId();

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.filehandler.api.Reader#getContent(java.nio.file.Path)
	 */
	@Override
	public List<RTICalibrationData> getContent(Path path) {

		List<RTICalibrationData> results = new ArrayList<>();

		SheetDefinition sheetDefinition = getRequiredSheets().get(getCalibrationSheetNameId());

		if (sheetDefinition == null || sheetDefinition.getNameInExcelFile() == null) {
			return Collections.emptyList();
		}

		Sheet sheet = getSheetFromNewWorkbook(path, sheetDefinition.getNameInExcelFile());

		if (sheet == null) {
			return Collections.emptyList();
		}

		ColumnDefinition substanceColumn = sheetDefinition.getIdentifierColumns().get(getCalibrationSubstanceId());
		ColumnDefinition rtColumn = sheetDefinition.getIdentifierColumns().get(getCalibrationRtId());
		ColumnDefinition signalColumn = sheetDefinition.getIdentifierColumns().get(getCalibrationSignalId());

		if (substanceColumn == null || rtColumn == null || !rtColumn.getMultiple() || signalColumn == null) {
			return Collections.emptyList();
		}

		int maxLength = MAX_CALIBRATION_DATA_COUNT + sheetDefinition.getContentRow();

		for (int i = sheetDefinition.getContentRow(); i <= sheet.getLastRowNum() && i < maxLength; i++) {
			String name;
			Double signalValue;
			Row row = sheet.getRow(i);

			name = PoiUtil.getStringCellValue(row.getCell(substanceColumn.getColumnIndex()));
			signalValue = ValueFormatUtil.roundCalibrationSignal(PoiUtil.getDoubleCellValue(row.getCell(signalColumn.getColumnIndex())));

			if (name == null || name.isEmpty() || signalValue == null) {
				continue;
			}

			RTICalibrationData calibration = new RTICalibrationData(name, signalValue);

			List<Double> rtValues = extractRTValues(rtColumn, row);
			
			Double meanRt = RTCalculationUtil.getMeanRt(rtValues);
			if(meanRt != null) {
				calibration.setMeanRt(meanRt);
			}
			
			if (!rtValues.isEmpty()) {
				results.add(calibration);
			}
		}

		try {
			sheet.getWorkbook().close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return results;
	}

	private List<Double> extractRTValues(ColumnDefinition rtColumn, Row row) {

		List<Double> rtValues = new ArrayList<>();
		Map<String, Integer> indices = rtColumn.getColumnIndices();

		for (Integer values : indices.values()) {
			Double rt = PoiUtil.getDoubleCellValue(row.getCell(values));
			if (rt != null) {
				rtValues.add(rt);
			}
		}

		return rtValues;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.filehandler.api.Reader#getContentClass()
	 */
	@Override
	public Class<?> getContentClass() {
		return List.class;
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
}
