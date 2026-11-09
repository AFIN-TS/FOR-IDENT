package de.hswt.fi.export.service.excel;

import de.hswt.fi.beans.BeanColumnDefinition;
import de.hswt.fi.beans.BeanComponentMapper;
import de.hswt.fi.calculation.service.api.CalculationService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkbookCreator {
	private static final Logger LOGGER = LoggerFactory.getLogger(WorkbookCreator.class);
	private CalculationService calculationService;
	private BeanComponentMapper mapper;
	private boolean includeSource;
	private Workbook workbook;
	private Sheet sheet;
	private int nextRowIndex;
	private int resultIndex;
	private int columnCount;
	private int structureColumnIndex;
	private Set<String> postfixFilter;
	private Map<String, CellStyle> numberCellFormats;
	private Map<BeanColumnDefinition, Integer> columnCounts;
	private ExcelSheetDefinition currentSheetDefinition;
	private int columnOffset;

	WorkbookCreator(BeanComponentMapper mapper, CalculationService calculationService) {
		if (mapper != null && calculationService != null) {
			this.mapper = mapper;
			this.calculationService = calculationService;
		} else {
			throw new NullPointerException("Parameter must not be null.");
		}
	}

	Workbook createWorkbook(ExcelFileDefinition definition) {
		if (definition == null) {
			throw new NullPointerException("Parameter definition is null.");
		}

		this.initialize();

		for (ExcelSheetDefinition sheetDefinition : definition.getSheets()) {
			this.currentSheetDefinition = sheetDefinition;
			this.createSheet();
		}

		if (definition.isAddSources() && definition.getSourceFile() != null && definition.getSourceFile().toFile().exists()) {
			new ExcelSheetImporter().addSheetsFromFileToWorkbook(definition.getSourceFile().toFile(), this.workbook);
		}

		return this.workbook;
	}

	private void createSheet() {
		if (this.currentSheetDefinition.getData() != null && !this.currentSheetDefinition.getData().isEmpty()) {
			if (!this.currentSheetDefinition.isIncludeSource()) {
				this.postfixFilter.add("source");
			} else {
				this.postfixFilter.remove("source");
			}

			this.sheet = this.workbook.createSheet(this.checkSheetName(this.currentSheetDefinition.getName()));
			Object targetBean = this.currentSheetDefinition.getData().iterator().next();
			List<String> propertyIds = this.currentSheetDefinition.getColumnPropertyIds();
			if (propertyIds == null) {
				propertyIds = this.mapper.getSelectorColumns(this.currentSheetDefinition.getData().iterator().next());
			}

			Map<String, BeanColumnDefinition> columnDefinitions = this.mapper.getBeanColumnDefinitions(targetBean, propertyIds);
			if (columnDefinitions.size() != propertyIds.size()) {
				throw new TypeNotPresentException("Unavailable column definition requested.", null);
			}

			Map<String, String> captions = this.mapper.getNestedPropertyCaptions(columnDefinitions.values(), this.postfixFilter);
			this.columnOffset = this.currentSheetDefinition.isCreateIndex() ? 1 : 0;
			// find index of structure column, if any
			this.structureColumnIndex = -1;
			int i = -1;

			for (String key : captions.keySet()) {
				i++;
				if (key.endsWith("structure")) {
					this.structureColumnIndex = i + this.columnOffset;
					break;
				}
			}

			this.nextRowIndex = 0;
			this.columnCount = this.columnOffset + captions.size();
			this.resultIndex = 1;
			if (this.currentSheetDefinition.getColumnDirection().equals(ExcelSheetDefinition.ColumnDirection.VERTICAL)) {
				this.createTitleRow(captions.values());
			} else {
				this.createTitleColumn(captions.values());
			}

			columnDefinitions.values().forEach(d -> {
				Integer var10000 = this.columnCounts.put(d, this.getColumnCount(d));
			});
			this.executeSheetCreation(columnDefinitions);
			this.resizeColumns();
		}
	}

	private String checkSheetName(String sheetName) {
		// XSSFWorkbook sheet names can only be 31 characters and are truncated if longer.
		// Therefore the actual sheet name is truncated already here to  prevent failures
		// in comparison within checkSheetName method!
		if (sheetName.length() > 31) {
			sheetName = sheetName.substring(0, 31);
		}

		WorkbookUtil.validateSheetName(sheetName);

		for (int i = 0; i < this.workbook.getNumberOfSheets(); i++) {
			if (this.workbook.getSheetName(i).equals(sheetName)) {
				return this.checkSheetName("1_" + sheetName);
			}
		}

		return sheetName;
	}

	// The rows are created sequentially. The parallel creation of the 2022 version (thread pool) lost
	// values at random: the lazy collections of the beans (e.g. additional names of an entry) were loaded
	// concurrently outside of a transaction (SP-03b).
	private void executeSheetCreation(Map<String, BeanColumnDefinition> columnDefinitions) {
		for (Object bean : this.currentSheetDefinition.getData()) {
			RowCreator rowCreator = new RowCreator(
				bean,
				this.mapper,
				this.calculationService,
				this.columnCounts,
				columnDefinitions,
				this.postfixFilter,
				this.columnCount - this.columnOffset
			);
			List<List<RowCreator.CellValue>> rows = rowCreator.getCells();

			if (rows != null && !rows.isEmpty()) {
				if (this.currentSheetDefinition.getColumnDirection().equals(ExcelSheetDefinition.ColumnDirection.VERTICAL)) {
					this.insertRows(rows);
				} else {
					this.insertColumns(rows);
				}
			}

			this.resultIndex++;
		}
	}

	private void initialize() {
		this.columnCounts = new HashMap<>();
		this.workbook = new XSSFWorkbook();
		this.initNumberCellFormats();
		this.postfixFilter = new HashSet<>();
		this.postfixFilter.add("lastModified");
		this.postfixFilter.add("additional");
		this.postfixFilter.add("scoreValue");
		this.postfixFilter.add("weight");
	}

	private void initNumberCellFormats() {
		DataFormat dataFormat = this.workbook.createDataFormat();
		this.numberCellFormats = new HashMap<>();
		this.numberCellFormats.put("%.1f", this.getCellStyle(dataFormat, "0.0"));
		this.numberCellFormats.put("%.2f", this.getCellStyle(dataFormat, "0.00"));
		this.numberCellFormats.put("%.3f", this.getCellStyle(dataFormat, "0.000"));
		this.numberCellFormats.put("%.4f", this.getCellStyle(dataFormat, "0.0000"));
		this.numberCellFormats.put("%.5f", this.getCellStyle(dataFormat, "0.00000"));
		this.numberCellFormats.put("%.6f", this.getCellStyle(dataFormat, "0.000000"));
	}

	private CellStyle getCellStyle(DataFormat dataFormat, String style) {
		CellStyle cellStyle = this.workbook.createCellStyle();
		cellStyle.setDataFormat(dataFormat.getFormat(style));
		return cellStyle;
	}

	private void initRow() {
		Row row = this.sheet.createRow(this.nextRowIndex++);
		if (this.currentSheetDefinition.isCreateIndex()) {
			row.createCell(0).setCellValue(this.resultIndex);
		}
	}

	private int getColumnCount(BeanColumnDefinition column) {
		int count = 0;

		for (BeanColumnDefinition childColumn : column.getChildren()) {
			if (this.postfixFilter.stream().noneMatch(f -> childColumn.getPropertyId().endsWith(f))) {
				count += this.getColumnCount(childColumn);
			}
		}

		return Math.max(count, 1);
	}

	private void createTitleRow(Collection<String> captions) {
		Row row = this.sheet.createRow(this.nextRowIndex++);
		int cellId = 0;
		if (this.currentSheetDefinition.isCreateIndex()) {
			Cell cell = row.createCell(cellId++);
			cell.setCellValue("ID");
		}

		for (String caption : captions) {
			Cell cell = row.createCell(cellId++);
			cell.setCellValue(caption);
		}
	}

	private void createTitleColumn(Collection<String> captions) {
		for (String caption : captions) {
			this.initRow();
			this.resultIndex++;
			Row row = this.sheet.getRow(this.nextRowIndex - 1);
			Cell cell = row.createCell(this.columnOffset);
			cell.setCellValue(caption);
		}
	}

	private void insertRows(List<List<RowCreator.CellValue>> rows) {
		int startRowIndex = this.nextRowIndex;
		rows.get(0).forEach(r -> this.initRow());
		int columnIndex = this.columnOffset;

		for (List<RowCreator.CellValue> column : rows) {
			int rowIndex = startRowIndex;

			for (RowCreator.CellValue cellValue : column) {
				if (cellValue.getValue().isPresent()) {
					if (columnIndex == this.structureColumnIndex) {
						this.writeStructure((byte[])cellValue.getValue().get(), rowIndex++);
					} else {
						Cell cell = this.sheet.getRow(rowIndex++).createCell(columnIndex);
						this.writeCell(cell, cellValue);
					}
				}
			}

			columnIndex++;
		}
	}

	private void insertColumns(List<List<RowCreator.CellValue>> rows) {
		int startColumnIndex = 1 + this.columnOffset;
		int rowIndex = 0;

		for (List<RowCreator.CellValue> column : rows) {
			int columnIndex = startColumnIndex;

			for (RowCreator.CellValue cellValue : column) {
				if (cellValue.getValue().isPresent()) {
					Cell cell = this.sheet.getRow(rowIndex).createCell(columnIndex++);
					this.writeCell(cell, cellValue);
				}
			}

			rowIndex++;
		}
	}

	private void writeCell(Cell cell, RowCreator.CellValue cellValue) {
		if (cellValue.getValue().isPresent()) {
			Object value = cellValue.getValue().get();
			if (Number.class.isAssignableFrom(cellValue.getType())) {
				if (Double.class.isAssignableFrom(value.getClass())) {
					cell.setCellValue((Double)value);
					cellValue.getFormat().ifPresent(format -> cell.setCellStyle(this.numberCellFormats.get(format)));
				}
			} else if (Date.class.isAssignableFrom(cellValue.getType()) && Date.class.isAssignableFrom(value.getClass())) {
				cell.setCellValue((Date)value);
			} else {
				cell.setCellValue(value.toString());
			}
		}
	}

	private void writeStructure(byte[] imageData, int rowIndex) {
		Drawing drawing = this.sheet.createDrawingPatriarch();
		Row row = this.sheet.getRow(rowIndex);
		row.setHeightInPoints(200.0F);
		int pictureIdx = this.workbook.addPicture(imageData, 6);
		ClientAnchor anchor = this.workbook.getCreationHelper().createClientAnchor();
		anchor.setCol1(this.structureColumnIndex);
		anchor.setRow1(rowIndex);
		anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
		Picture pict = drawing.createPicture(anchor, pictureIdx);
		pict.resize(1.0, 1.0);
	}

	// Sequential as well: POI sheets are not thread safe, and the parallel version only waited 10 seconds,
	// so the workbook could be written while columns were still being resized (SP-03b)
	private void resizeColumns() {
		for (int i = this.columnOffset; i < this.columnCount; i++) {
			this.resizeColumn(i);
		}
	}

	private void resizeColumn(int columnIndex) {
		int maxNumCharacters = 0;
		if (columnIndex == this.structureColumnIndex) {
			this.sheet.setColumnWidth(this.structureColumnIndex, 9472);
		} else {
			for (int row = 0; row <= this.sheet.getLastRowNum(); row++) {
				DataFormatter formatter = new DataFormatter();
				int numberOfCharacters = formatter.formatCellValue(this.sheet.getRow(row).getCell(columnIndex)).length();
				if (numberOfCharacters > maxNumCharacters) {
					maxNumCharacters = numberOfCharacters;
				}
			}

			int width = Math.min((int)(maxNumCharacters * (maxNumCharacters < 50 ? 1.3 : 1.1)), 100) * 256;
			this.sheet.setColumnWidth(columnIndex, width);
		}
	}

	public boolean includeSource() {
		return this.includeSource;
	}

	public void setIncludeSource(boolean includeSource) {
		this.includeSource = includeSource;
	}
}
