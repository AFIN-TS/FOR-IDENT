package de.hswt.filehandler.excel;

import de.hswt.filehandler.api.Reader;
import de.hswt.filehandler.definition.ColumnDefinition;
import de.hswt.filehandler.definition.SheetDefinition;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public abstract class ExcelReader implements Reader<Workbook> {

	private static final String INVALID_ARGUMENT_ERROR_MESSAGE = "The arguments must not be null or invalid";

	private static final String ERROR_MESSAGE = "An error occured";

	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelReader.class);

	private Document configFileDocument;

	private Map<String, SheetDefinition> requiredSheets;

	private Map<String, SheetDefinition> optionalSheets;

	private Map<String, ColumnDefinition> sortedIdentifierColumns;

	private List<String> requiredColumnCaptions;

	private List<String> columnCaptions;

	protected abstract Path getConfigFile();

	protected abstract InputStream getConfigFileAsStream();

	/**
	 * Checks if this reader can read the excel file and returns a score value
	 * that quantifies how well the excel file fits the required identifiers of
	 * the reader.
	 *
	 * @param clazz
	 */
	@Override
	public double canHandle(Workbook workbook, Class<?> clazz) {
		Objects.requireNonNull(workbook, "Paramter workbook must not be null.");
		Objects.requireNonNull(clazz, "Paramter clazz must not be null.");

		double scoreValue = CAN_NOT_HANDLE;

		// Check if xml config file is valid
		if (getConfigFileDocument() == null) {
			return scoreValue;
		}

		// Check if Reader can read expected Content
		if (!clazz.isAssignableFrom(getContentClass())) {
			return scoreValue;
		}

		// Check if excel file type is readable
		if (!fileHasCorrectType(workbook)) {
			return scoreValue;
		}

		// Check if content of the file is readable
		if (!fileHasCorrectFormat(workbook)) {
			return scoreValue;
		}
		scoreValue = calculateScore(workbook);
		if (!requiredColumnCaptions.isEmpty() && !columnCaptions.containsAll(requiredColumnCaptions)) {
			return 0.0;
		}

		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return scoreValue;
	}

	/**
	 * Checks if the excel file type (XSSF or HSSF) is readable by this reader.
	 *
	 * * @param workbook 
	 * 				the excel workbook 
	 * @return boolean
	 * 				if Excel Workbook has correct type
	 */
	private boolean fileHasCorrectType(Workbook workbook) {

		Node rootElement = configFileDocument.getDocumentElement();
		NamedNodeMap attributes = rootElement.getAttributes();
		Node format = attributes.getNamedItem("format");

		if (format == null) {

			throw new IllegalArgumentException(
					"The file type in the config.xml of an excel reader must be declared");
		}

		if ("97".equals(format.getNodeValue().trim())) {
			return workbook instanceof HSSFWorkbook;
		} else if ("2007".equals(format.getNodeValue().trim())) {
			return workbook instanceof XSSFWorkbook;
		} else if ("97-2007".equals(format.getNodeValue().trim())) {
			return workbook instanceof HSSFWorkbook || workbook instanceof XSSFWorkbook;
		} else {
			throw new IllegalArgumentException("The file type in the config.xml of an excel "
					+ "reader must \"97\",\"2007\" or \"97-2007\"");
		}
	}

	/**
	 * Checks if the excel file has the required format that is specified in the
	 * config file of the reader.
	 *
	 * * @param workbook 
	 * 				the excel workbook
	 * @return boolean
	 * 				if Excel Workbook has correct type
	 */
	private boolean fileHasCorrectFormat(Workbook workbook) {

		if (workbook == null) {
			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		optionalSheets = determineOptionalSheets();
		requiredSheets = determineRequiredSheets();

		for (String id : requiredSheets.keySet()) {

			SheetDefinition sheetDefinition = requiredSheets.get(id);

			if (sheetDefinition.getNameInXmlFile() == null
					&& requiredSheetExists(workbook, sheetDefinition)) {
				continue;
			} else if (sheetDefinition.getNameInXmlFile() != null && requiredSheetExists(workbook,
					sheetDefinition.getNameInXmlFile(), sheetDefinition)) {
				continue;
			} else {
				return false;
			}
		}

		for (String id : optionalSheets.keySet()) {

			SheetDefinition sheetDefinition = optionalSheets.get(id);

			if (sheetDefinition.getNameInXmlFile() == null
					&& requiredSheetExists(workbook, sheetDefinition)) {
				continue;
			} else if (sheetDefinition.getNameInXmlFile() != null && requiredSheetExists(workbook,
					sheetDefinition.getNameInXmlFile(), sheetDefinition)) {
				continue;
			}
		}
		return true;
	}

	/**
	 * Checks if the excel file has a required sheet that is specified in the
	 * config file of the reader.
	 * <p>
	 * This method is only used if the required sheet has no name in the config
	 * file of the reader. Otherwise the second requiredSheetExists method is
	 * used.
	 *
	 * * @param workbook 
	 * 				the excel workbook
	 * * * @param sheetDefinition
	 * 				the required sheet definition
	 * 				the required sheet definition
	 * @return boolean
	 * 				Workbook has required Sheet	
	 */
	private boolean requiredSheetExists(Workbook workbook, SheetDefinition sheetDefinition) {

		if (workbook == null || sheetDefinition == null) {
			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		List<Sheet> existingSheets = getExistingSheets(workbook);

		for (Sheet sheet : existingSheets) {
			if (sheetHasCorrectIdentifiers(sheet, sheetDefinition)) {
				sheetDefinition.setNameInExcelFile(sheet.getSheetName());
				return true;
			}
			// reset sheet definition to remove references to sheets that only
			// partially matched the sheet definition
			sheetDefinition.reset();
		}
		return false;
	}

	/**
	 * Returns an ArrayList of all sheets the excel file contains.
	 *
	 * * @param workbook 
	 * 				the excel workbook
	 * @return List<Sheet>
	 *     			all sheets within the workbook
	 */
	private List<Sheet> getExistingSheets(Workbook workbook) {

		if (workbook == null) {
			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		List<Sheet> existingSheets = new ArrayList<>();
		int numberOfSheets = workbook.getNumberOfSheets();

		for (int i = 0; i < numberOfSheets; i++) {

			existingSheets.add(workbook.getSheetAt(i));
		}

		return existingSheets;
	}

	/**
	 * Checks if the excel file has a required sheet that is specified in the
	 * config file of the reader.
	 * <p>
	 * This method is only used if the required sheet has a name in the config
	 * file of the reader. Otherwise the second requiredSheetExists method is
	 * used.
	 *
	 * * @param workbook 
	 * 				the excel workbook
	 * @param requiredSheetName
	 * * @param sheetDefinition
	 * 				the required sheet definition
	 * @return
	 */
	private boolean requiredSheetExists(Workbook workbook, String requiredSheetName, SheetDefinition sheetDefinition) {

		if (workbook == null || requiredSheetName == null) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		Sheet sheet = workbook.getSheet(requiredSheetName);

		if (sheet == null) {
			return false;
		}

		if (sheetHasCorrectIdentifiers(sheet, sheetDefinition)) {
			sheetDefinition.setNameInExcelFile(requiredSheetName);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks if a sheet contains all the required identifiers that are
	 * specified in the config file of the reader.
	 *
	 * @param sheet
	 * * @param sheetDefinition
	 * 				the required sheet definition
	 * @return boolean
	 * 				sheet has the correct identifiers
	 */
	private boolean sheetHasCorrectIdentifiers(Sheet sheet, SheetDefinition sheetDefinition) {

		if (sheet == null || sheetDefinition == null) {
			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		int identifierRowIndex = sheetDefinition.getIdentifierRow();
		Row identifierRow = sheet.getRow(identifierRowIndex);

		if (identifierRow == null) {
			return false;
		}

		Map<String, Integer> groupRequirements = sheetDefinition.getGroupRequirements();

		// Map that contains the number of columns of one group that really
		// exist in the excel sheet
		Map<String, Integer> existingColumnsInGroups = new HashMap<>();
		// Initialize existingColumnsInGroups with all group IDs as keys and 0
		// as values
		for (String groupID : groupRequirements.keySet()) {
			existingColumnsInGroups.put(groupID, 0);
		}

		Map<String, ColumnDefinition> identifierColumns = sheetDefinition.getIdentifierColumns();

		// Contains column indices of those columns which had a required
		// identifier
		Map<Integer, Boolean> checkedColumns = new HashMap<>();

		// Check if all identifiers exist in the excel sheet
		for (String id : identifierColumns.keySet()) {

			ColumnDefinition requiredColumn = identifierColumns.get(id);

			if (identifierColumnExists(identifierRow, requiredColumn, checkedColumns)) {
				int oldColumnsInGroups = existingColumnsInGroups.get(requiredColumn.getGroupID());
				existingColumnsInGroups.put(requiredColumn.getGroupID(), oldColumnsInGroups + 1);
			} else {
				// reset column definition to remove references to columns that
				// only
				// partially matched the column definition
				requiredColumn.reset();
			}
		}

		for (String groupID : groupRequirements.keySet()) {

			int requiredColumnNumber = groupRequirements.get(groupID);
			int existingColumnNumber = existingColumnsInGroups.get(groupID);

			if (existingColumnNumber < requiredColumnNumber) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if a sheet contains one specific identifier that is specified in
	 * the config file of the reader.
	 *
	 * @param identifierRow
	 * @param requiredColumn
	 * @param checkedColumns
	 * @return
	 */
	private boolean identifierColumnExists(Row identifierRow, ColumnDefinition requiredColumn, Map<Integer, Boolean> checkedColumns) {

		if (requiredColumn == null || identifierRow == null || checkedColumns == null) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		// Map that contains the names of instances of all required multiple and
		// singular columns
		// that were already found in the sheet. Serves to check if a column
		// identifier exists
		// more than 1 time in the sheet
		Map<String, Boolean> validatedIdentifiers = new HashMap<>();

		Boolean multiple = requiredColumn.getMultiple();

		if (multiple) {
			return compareIfMultiple(identifierRow, requiredColumn, validatedIdentifiers,
					checkedColumns);
		} else {
			return compareIfSingular(identifierRow, requiredColumn, validatedIdentifiers,
					checkedColumns);
		}
	}

	/**
	 * Checks if a sheet contains one specific multiple identifier that is
	 * specified in the config file of the reader.
	 *
	 * @param identifierRow
	 * @param requiredColumn
	 * @param validatedIdentifiers
	 * @param checkedColumns
	 * @return
	 */
	private boolean compareIfMultiple(Row identifierRow, ColumnDefinition requiredColumn, Map<String, Boolean> validatedIdentifiers, Map<Integer, Boolean> checkedColumns) {

		if (requiredColumn == null || identifierRow == null || validatedIdentifiers == null
				|| checkedColumns == null) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		String requiredColumnName = requiredColumn.getName();
		int counterOfMultiples = 0;

		for (Cell cell : identifierRow) {

			// Check if the current column was already successfully checked for
			// a required identifier. If it was then skip this column.
			if (checkedColumns.get(cell.getColumnIndex()) != null) {
				continue;
			}

			// Check if the current column contains a string. If it does not
			// then
			// skip this column because it can not contain an identifier.
			if (cell.getCellType() != CellType.STRING
					&& cell.getCellType() != CellType.BLANK) {
				continue;
			}

			String cellIdentifierName = cell.getStringCellValue();

			boolean regexCheck = cellIdentifierName.matches(requiredColumnName + ".*");

			// Check if the excel identifier matches the XML identifier
			// and if it exists more than 1 time in the sheet
			if (regexCheck) {
				counterOfMultiples = counterOfMultiples + 1;
				requiredColumn.setColumnIndices(cellIdentifierName, cell.getColumnIndex());
				checkedColumns.put(cell.getColumnIndex(), true);
			}

			if (regexCheck && sameSuffixExistsMultipleTimes(cellIdentifierName,
					validatedIdentifiers, counterOfMultiples)) {
				throw new IllegalArgumentException(
						"Multiple identifier exists multiple times in sheet with the same suffix.");
			}
		}
		if (counterOfMultiples < 1) {

			requiredColumn.clearColumnIndices();
			return false;
		}

		return true;
	}

	/**
	 * Checks if a sheet contains one specific multiple identifier more than one
	 * time with the same suffix and throws an exception if this is the case.
	 *
	 * @param cellIdentifierName
	 * @param validatedIdentifiers
	 * @param counterOfMultiples
	 * @return
	 */
	private boolean sameSuffixExistsMultipleTimes(String cellIdentifierName, Map<String, Boolean> validatedIdentifiers, Integer counterOfMultiples) {

		if (cellIdentifierName == null || cellIdentifierName.isEmpty()
				|| validatedIdentifiers == null || counterOfMultiples == null) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		if (validatedIdentifiers.get(cellIdentifierName) == null) {
			validatedIdentifiers.put(cellIdentifierName, true);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Checks if a sheet contains one specific singular identifier that is
	 * specified in the config file of the reader.
	 *
	 * @param identifierRow
	 * @param requiredColumn
	 * @param validatedIdentifiers
	 * @param checkedColumns
	 * @return
	 */
	private boolean compareIfSingular(Row identifierRow, ColumnDefinition requiredColumn, Map<String, Boolean> validatedIdentifiers, Map<Integer, Boolean> checkedColumns) {

		if (requiredColumn == null || identifierRow == null || validatedIdentifiers == null
				|| checkedColumns == null) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		String requiredColumnName = requiredColumn.getName();

		for (Cell cell : identifierRow) {

			// Check if the current column was already successfully checked for
			// a required identifier. If it was then skip this column.
			if (checkedColumns.get(cell.getColumnIndex()) != null) {
				continue;
			}

			// Check if the current column contains a string. If it does not
			// then
			// skip this column because it can not contain an identifier.
			if (cell.getCellType() != CellType.STRING
					&& cell.getCellType() != CellType.BLANK) {
				continue;
			}

			String cellIdentifierName = cell.getStringCellValue();

			// Check if the current column identifier matches the required XML
			// identifier and if it exists more than 1 time in the sheet
			if (cellIdentifierName.equals(requiredColumnName)
					&& validatedIdentifiers.get(requiredColumnName) == null) {
				validatedIdentifiers.put(requiredColumnName, true);
				checkedColumns.put(cell.getColumnIndex(), true);
				requiredColumn.setColumnIndex(cell.getColumnIndex());
			} else if (cellIdentifierName.equals(requiredColumnName)
					&& validatedIdentifiers.get(requiredColumnName) != null) {
				throw new IllegalArgumentException(
						"Singular identifier exists multiple times in sheet.");
			}
		}
		return validatedIdentifiers.get(requiredColumnName) != null;
	}

	/**
	 * Creates a input stream out of the config file of the reader.
	 *
	 * @return
	 */
	private Document getConfigFileDocument() {

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			Path configFile = getConfigFile();
			if (configFile != null) {
				configFileDocument = builder.parse(configFile.toFile());
			} else {
				InputStream configStream = getConfigFileAsStream();
				if (configStream == null) {
					throw new NullPointerException("config file and config input stream are null");
				}
				configFileDocument = builder.parse(configStream);
			}

			return configFileDocument;
		} catch (IOException | ParserConfigurationException | SAXException e) {
			LOGGER.error(ERROR_MESSAGE, e);
			return null;
		}
	}

	/**
	 * Creates a map of the required sheets that are specified in the config
	 * file of the reader. The sheet ID from the config file serves as key and
	 * the corresponding SheetDefinition serves as value.
	 *
	 * @return
	 */
	private Map<String, SheetDefinition> determineRequiredSheets() {

		Node rootElement = configFileDocument.getDocumentElement();
		NodeList xmlSheets = getSpecificChildNode(rootElement, "sheets").getChildNodes();
		Map<String, SheetDefinition> sheets = new HashMap<>();

		for (int i = 0; i < xmlSheets.getLength(); i++) {

			Node xmlSheet = xmlSheets.item(i);
			if (xmlSheet.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			NamedNodeMap attributes = xmlSheet.getAttributes();
			String optional = attributes.getNamedItem("optional").getNodeValue();

			String sheetID = attributes.getNamedItem("id").getNodeValue();
			Boolean isOptional = Boolean.valueOf(optional);

			if (isOptional) {
				continue;
			}
			Node nameAttribute = attributes.getNamedItem("name");
			String sheetName = null;

			if (nameAttribute != null) {
				sheetName = nameAttribute.getNodeValue();
			}

			int identifierRow = Integer
					.parseInt(attributes.getNamedItem("identifierRow").getNodeValue());
			int contentRow = Integer.parseInt(attributes.getNamedItem("contentRow").getNodeValue());
			Map<String, ColumnDefinition> requiredColumns = getIdentifierColumns(xmlSheet);
			Map<String, Integer> groupRequirements = getGroupRequirements(xmlSheet);
			SheetDefinition sheetDefinition = new SheetDefinition(sheetID, sheetName, identifierRow,
					contentRow, requiredColumns, groupRequirements);

			sheets.put(sheetID, sheetDefinition);
		}
		return sheets;
	}

	private Map<String, SheetDefinition> determineOptionalSheets() {

		Node rootElement = configFileDocument.getDocumentElement();
		NodeList xmlSheets = getSpecificChildNode(rootElement, "sheets").getChildNodes();
		Map<String, SheetDefinition> sheets = new HashMap<>();

		for (int i = 0; i < xmlSheets.getLength(); i++) {

			Node xmlSheet = xmlSheets.item(i);

			if (xmlSheet.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			NamedNodeMap attributes = xmlSheet.getAttributes();

			String sheetID = attributes.getNamedItem("id").getNodeValue();
			String optional = attributes.getNamedItem("optional").getNodeValue();
			Boolean isOptional = Boolean.valueOf(optional);

			if (!isOptional) {
				continue;
			}
			Node nameAttribute = attributes.getNamedItem("name");
			String sheetName = null;

			if (nameAttribute != null) {
				sheetName = nameAttribute.getNodeValue();
			}

			int identifierRow = Integer
					.parseInt(attributes.getNamedItem("identifierRow").getNodeValue());
			int contentRow = Integer.parseInt(attributes.getNamedItem("contentRow").getNodeValue());
			Map<String, ColumnDefinition> requiredColumns = getIdentifierColumns(xmlSheet);
			Map<String, Integer> groupRequirements = getGroupRequirements(xmlSheet);
			SheetDefinition sheetDefinition = new SheetDefinition(sheetID, sheetName, identifierRow,
					contentRow, requiredColumns, groupRequirements);

			sheets.put(sheetID, sheetDefinition);
		}
		return sheets;
	}

	/**
	 * Returns a map of the columns a sheet requires that are specified in the
	 * config file of the reader. The column ID serves as key and the
	 * corresponding ColumnDefinition serves as value. The keys of the map are
	 * sorted with the prefix length of the corresponding ColumnDefinition
	 * descending. This is required so that the identifiers with long prefix are
	 * checked first for existence in the excel file to prevent accidental
	 * matching of short multiple identifiers.
	 *
	 * @param xmlSheet
	 * @return
	 */
	private Map<String, ColumnDefinition> getIdentifierColumns(Node xmlSheet) {

		if (xmlSheet == null) {
			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		Map<String, ColumnDefinition> identIfierColumns = new HashMap<>();
		ValueComparator bvc = new ValueComparator(identIfierColumns);
		requiredColumnCaptions = new ArrayList<>();

		// This map will contain only the sorted keys (IDs). All values will be
		// null due to
		// the missing 0 return value in the Value Comparator.
		SortedMap<String, ColumnDefinition> sortedKeys = new TreeMap<>(bvc);

		// This map will contain the sorted keys (IDs) and their corresponding
		// ColumnDefinition objects.
		sortedIdentifierColumns = new LinkedHashMap<>();

		NodeList groups = getSpecificChildNode(xmlSheet, "groups").getChildNodes();

		// Iterate through all groups of a sheet
		for (int i = 0; i < groups.getLength(); i++) {

			Node group = groups.item(i);

			if (group.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			NamedNodeMap groupAttributes = group.getAttributes();
			String groupID = groupAttributes.getNamedItem("id").getNodeValue();

			NodeList columns = getSpecificChildNode(group, "columns").getChildNodes();

			// Iterate through all columns of a group
			for (int j = 0; j < columns.getLength(); j++) {

				Node column = columns.item(j);

				if (column.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}

				NamedNodeMap columnAttributes = column.getAttributes();
				String columnID = columnAttributes.getNamedItem("id").getNodeValue();
				String columnName = columnAttributes.getNamedItem("name").getNodeValue();
				String columnMultiple = "false";
				Node multiple = columnAttributes.getNamedItem("multiple");
				if (multiple != null) {
					columnMultiple = multiple.getNodeValue();
				}

				Node required = columnAttributes.getNamedItem("required");

				ColumnDefinition columnDefinition = new ColumnDefinition(columnID, columnName,
						columnMultiple, groupID);

				identIfierColumns.put(columnID, columnDefinition);

				if (required != null) {
					boolean isRequired = Boolean.valueOf(required.getNodeValue());
					if (isRequired) {
						requiredColumnCaptions.add(columnDefinition.getName());
					}
				}
			}
		}

		sortedKeys.putAll(identIfierColumns);

		for (String id : sortedKeys.keySet()) {
			sortedIdentifierColumns.put(id, identIfierColumns.get(id));
		}

		return sortedIdentifierColumns;
	}

	/**
	 * Returns a map of the column groups and their requirements that are
	 * specified config file of the reader. The group ID serves as key and the
	 * corresponding required number of columns of this group serves as value.
	 *
	 * @param xmlSheet
	 * @return
	 */
	private Map<String, Integer> getGroupRequirements(Node xmlSheet) {

		if (xmlSheet == null) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		Map<String, Integer> groupRequirements = new HashMap<>();

		NodeList groups = getSpecificChildNode(xmlSheet, "groups").getChildNodes();

		for (int i = 0; i < groups.getLength(); i++) {

			Node group = groups.item(i);

			if (group.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			NamedNodeMap attributes = group.getAttributes();
			String groupID = attributes.getNamedItem("id").getNodeValue();
			String requiredColumnsString = attributes.getNamedItem("requiredColumns")
					.getNodeValue();
			int requiredColumnsInt = parseRequiredColumns(requiredColumnsString, group);

			groupRequirements.put(groupID, requiredColumnsInt);
		}

		return groupRequirements;
	}

	/**
	 * Parses the number of required columns of a group from string to integer.
	 *
	 * @param requiredColumnsString
	 * @return
	 */
	private int parseRequiredColumns(String requiredColumnsString, Node group) {

		if (requiredColumnsString == null || requiredColumnsString.isEmpty()) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		try {
			if ("all".equals(requiredColumnsString)) {
				return countColumnsInGroup(group);
			} else {
				return Integer.parseInt(requiredColumnsString);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					"The number of required columns of a group specified in the "
							+ "XML config file must either be an integer or \"all\"");
		}
	}

	private int countColumnsInGroup(Node group) {

		int count = 0;
		NodeList columns = getSpecificChildNode(group, "columns").getChildNodes();

		for (int i = 0; i < columns.getLength(); i++) {

			Node column = columns.item(i);

			if (column.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			count++;
		}

		return count;
	}

	/**
	 * Calculates a score that quantifies how well an excel file fits the
	 * required identifiers that are specified in the config file of the reader.
	 * Score values range from 0 (worst) to 1 (best).
	 *
	 * * @param workbook 
	 * 				the excel workbook
	 * @return
	 */
	private double calculateScore(Workbook workbook) {

		if (workbook == null) {
			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		int numberOfExistingRequiredColumns = 0;
		int numberOfExistingColumns = 0;

		int numberOfExistingOptionalColumns = 0;
		int numberOfOptionalColumns = 0;

		for (SheetDefinition requiredSheet : requiredSheets.values()) {
			numberOfExistingRequiredColumns += countExistingIdentiferColumns(requiredSheet);
			numberOfExistingColumns += countExistingColumns(workbook, requiredSheet);
		}
		for (SheetDefinition optionalSheet : optionalSheets.values()) {
			numberOfExistingOptionalColumns = numberOfExistingRequiredColumns
					+ countExistingIdentiferColumns(optionalSheet);
		}

		if (numberOfExistingRequiredColumns == 0) {
			return 0d;
		}

		// Only sheets that are required influence the score calculation since
		// the identifier row index of
		// an unrequired sheet is unknown
		double requiredScore = (double) numberOfExistingRequiredColumns / (double) numberOfExistingColumns;

		if (numberOfExistingOptionalColumns == 0) {
			return requiredScore;
		}

		double optionalScore = (double) numberOfExistingOptionalColumns / (double) numberOfOptionalColumns;

		if (Double.isNaN(optionalScore) || Double.isInfinite(optionalScore)) {
			return requiredScore;
		}

		return optionalScore;
	}

	/**
	 * Counts how many of the identifiers that exist in the excel file are
	 * really required by the reader.
	 *
	 * * @param sheetDefinition
	 * 				the required sheet definition
	 * @return
	 */
	private int countExistingIdentiferColumns(SheetDefinition sheetDefinition) {

		if (sheetDefinition == null) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		Map<String, ColumnDefinition> requiredColumns = sheetDefinition.getIdentifierColumns();

		// Getting the number of existing required columns ignoring the multiple
		// columns
		int numberOfExistingRequiredColumns = 0;

		// Counting the number of existing required columns. Not every
		// requiredColumn must actually
		// exist in excel sheet since some may be declared as optional in their
		// group in the
		// config file
		for (String columnId : requiredColumns.keySet()) {
			ColumnDefinition columnDefinition = requiredColumns.get(columnId);

			// Adding +1 for every instance of a existing multiple column. An
			// empty column index
			// map means that this column not found in the excel sheet and
			// therefore is not counted
			// to the number of existing required columns
			if (columnDefinition.getMultiple() && !columnDefinition.getColumnIndices().isEmpty()) {
				numberOfExistingRequiredColumns = numberOfExistingRequiredColumns
						+ columnDefinition.getColumnIndices().size();
			}
			// Adding +1 for every existing singular column. A column index of
			// -1 means that this
			// column was not found in the excel sheet and therefore is not
			// counted to the number
			// of existing required columns
			else if (!columnDefinition.getMultiple() && columnDefinition.getColumnIndex() != -1) {
				numberOfExistingRequiredColumns++;
			}
		}

		return numberOfExistingRequiredColumns;
	}

	/**
	 * Counts how many identifiers exist in the excel file.
	 *
	 * * @param workbook 
	 * 				the excel workbook
	 * * @param sheetDefinition
	 * 				the required sheet definition
	 * @return
	 */
	private int countExistingColumns(Workbook workbook, SheetDefinition sheetDefinition) {

		if (workbook == null || sheetDefinition == null) {
			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		columnCaptions = new ArrayList<>();
		Sheet sheet = workbook.getSheet(sheetDefinition.getNameInExcelFile());
		int identifierRowIndex = sheetDefinition.getIdentifierRow();
		Row identifierRow = sheet.getRow(identifierRowIndex);

		int numberOfExistingColumns = 0;

		for (Cell cell : identifierRow) {

			if (cell.getCellType() == CellType.STRING
					&& !cell.getStringCellValue().isEmpty()) {
				columnCaptions.add(cell.getStringCellValue());
				numberOfExistingColumns++;
			}
		}

		return numberOfExistingColumns;
	}

	/**
	 * Returns child node with a specific name.
	 *
	 * @param parentNode
	 * @param requiredChild
	 * @return
	 */
	private Node getSpecificChildNode(Node parentNode, String requiredChild) {

		if (parentNode == null || requiredChild == null) {

			throw new IllegalArgumentException(INVALID_ARGUMENT_ERROR_MESSAGE);
		}

		NodeList children = parentNode.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			Node child = children.item(i);

			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			String childName = child.getNodeName();

			if (childName.equals(requiredChild)) {

				return child;
			}
		}
		throw new IllegalArgumentException("specific XML child node not found");
	}

	/**
	 * Comparator is used for sorting the keys in getRequiredColumns.
	 */
	public class ValueComparator implements Comparator<String> {

		Map<String, ColumnDefinition> base;

		ValueComparator(Map<String, ColumnDefinition> base) {
			this.base = base;
		}

		@Override
		public int compare(String a, String b) {

			if (base.get(a).getName().length() >= base.get(b).getName().length()) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	}

	protected Workbook getNewWorkbook(Path path) {
		try (Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
			return workbook;
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	protected Sheet getSheetFromNewWorkbook(Path path, String sheetName) {
		return getNewWorkbook(path).getSheet(sheetName);
	}

	/**
	 * Getter method for the map created by determineRequiredSheets.
	 *
	 * @return
	 */
	protected Map<String, SheetDefinition> getRequiredSheets() {
		return requiredSheets;
	}

	protected Map<String, SheetDefinition> getOptionalSheets() {
		return optionalSheets;
	}

	public Map<String, ColumnDefinition> getSortedIdentifierColumns() {
		return sortedIdentifierColumns;
	}
}
