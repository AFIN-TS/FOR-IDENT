package de.hswt.fi.common;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Class POIUtil is used to read values from excel file cells. Therefore it
 * uses the Apache POI lib. It is used to get easy access to String and Double
 * values, by providing plantident values if the access fails.
 */
public final class PoiUtil {

	private PoiUtil() {
		// prevent instantiation
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(PoiUtil.class);

	/**
	 * Gets the string value of a cell. Returns simply the string value when the
	 * cell is a string cell. Performs a string transformation if the cell is
	 * numeric cell.
	 *
	 * @param cell
	 *            the excel cell
	 * @return the string value of the cell, an empty string if the cell is null
	 *         or the type is not a string or numeric
	 */
	public static String getStringCellValue(Cell cell) {
		LOGGER.trace("Entering getStringCellValue(), with cell {}", cell);

		if (cell == null) {
			LOGGER.trace("The given cell is null. Returning empty string.");
			return "";
		}

		String value;
		switch (cell.getCellType()) {
		case NUMERIC:
			value = Double.toString(cell.getNumericCellValue());
			LOGGER.trace("The given cell is a numberic cell.");
			break;
		case STRING:
			LOGGER.trace("The given cell is a string cell.");
			return Encode.forHtml((cell.getStringCellValue()));
		default: return "";
		}

		LOGGER.trace("Leave getStringCellValue(), returning {}", value);
		return value;
	}

	/**
	 * Gets the double value of a cell. Returns simply the double value when the
	 * cell is a numeric cell. Performs a transformation if the cell is string
	 * cell.
	 *
	 * @param cell
	 *            the excel cell
	 * @return the string value of the cell, an null if the cell is null or the
	 *         type is not a string or numeric
	 */
	public static Double getDoubleCellValue(Cell cell) {
		LOGGER.trace("Entering getDoubleCellValue(), with cell {}", cell);

		if (cell == null) {
			LOGGER.trace("The given cell is null. Returning null.");
			return null;
		}

		Double value = null;
		switch (cell.getCellType()) {
		case NUMERIC:
			LOGGER.trace("The given cell is a numberic cell.");
			value = cell.getNumericCellValue();
			break;
		case STRING:
			LOGGER.trace("The given cell is a string cell.");
			try {
				value = Double.parseDouble(Encode.forHtml((cell.getStringCellValue())));
			} catch (NumberFormatException ex) {
				LOGGER.trace("Value parsing (string to double) failed with value {}.",
						Encode.forHtml(cell.getStringCellValue()));
			}
			break;
		default: return null;
		}

		LOGGER.trace("Leave getDoubleCellValue(), returning {}", value);
		return value;
	}

	/**
	 * Checks if is row empty.
	 *
	 * @param row
	 *            the row
	 * @return true, if is row empty
	 */
	public static boolean isRowEmpty(Row row) {
		if (row == null) {
			return true;
		}
		for (int c = row.getFirstCellNum(); c <= row.getLastCellNum(); c++) {
			Cell cell = row.getCell(c);
			if (cell != null && cell.getCellType() != CellType.BLANK) {
				return false;
			}
		}
		return true;
	}
}
