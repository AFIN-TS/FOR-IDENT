package de.hswt.fi.export.service.excel;

import de.hswt.fi.beans.BeanColumnDefinition;
import de.hswt.fi.beans.BeanComponentMapper;
import de.hswt.fi.calculation.service.api.CalculationService;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

class RowCreator {

	private static final String STRUCTURE_POSTFIX = "structure";
	private Object bean;
	private CalculationService calculationService;
	private BeanComponentMapper mapper;
	private int columnCount;
	private Map<BeanColumnDefinition, Integer> columnCounts;
	private Map<String, BeanColumnDefinition> columns;
	private int rowIndex;
	private int nextRowIndex;
	private NumberFormat format;
	private Collection<String> postfixFilter;
	private List<List<CellValue>> rows;

	RowCreator(Object bean, BeanComponentMapper mapper,
			   CalculationService calculationService, Map<BeanColumnDefinition, Integer> columnCounts,
			   Map<String, BeanColumnDefinition> columns, Collection<String> postfixFilter,
			   int columnCount) {
		Objects.requireNonNull(bean, "Parameter bean is null.");
		Objects.requireNonNull(mapper, "Parameter mapper is null.");
		Objects.requireNonNull(calculationService, "Parameter calculationService is null.");
		Objects.requireNonNull(columns, "Parameter columns is null.");

		this.bean = bean;
		this.mapper = mapper;
		this.calculationService = calculationService;
		this.columns = columns;
		this.columnCounts = columnCounts != null ? columnCounts : new HashMap<>();
		this.postfixFilter = postfixFilter != null ? postfixFilter : new ArrayList<>();
		rowIndex = 0;
		nextRowIndex = 0;

		rows = new ArrayList<>();
		for (int i = 0; i < columnCount; i++) {
			rows.add(new ArrayList<>());
		}

		format = NumberFormat.getInstance(mapper.getLocale());
	}

	List<List<CellValue>> getCells() {
		createContentRows();
		return rows;
	}

	private void createContentRows() {
		columnCount = 0;
		initRow();
		for (String key : columns.keySet()) {
			BeanColumnDefinition column = columns.get(key);
			if (column.isCollection()) {
				createListCells(bean, column);
			} else {
				createNestedCells(bean, column);
			}
		}
	}

	/**
	 * Creates cells from properties.
	 *
	 * @param bean
	 *            The bean to get the data from.
	 * @param column
	 *            The bean column to get data from.
	 */
	private void createNestedCells(Object bean, BeanColumnDefinition column) {
		createNestedCells(bean, column, "");
	}

	/**
	 * Creates cells from properties.
	 *
	 * @param bean
	 *            The bean to get the data from.
	 * @param column
	 *            The bean column to get data from.
	 * @param prefixToRemove
	 *            The prefix to remove from the current propertyId
	 */
	private void createNestedCells(Object bean, BeanColumnDefinition column, String prefixToRemove) {
		String propertyId = column.getPropertyId();

		if (propertyId.startsWith(prefixToRemove + ".")) {
			propertyId = propertyId.replace(prefixToRemove + ".", "");
		}

		if (postfixFilter.contains(propertyId)) {
			return;
		}

		//TODO Add Cell for parent caption here?
		if (mapper.getPropertyAsString(bean, propertyId) == null) {
			columnCount += columnCounts.getOrDefault(column, 1);
			return;
		}

		if (!column.getChildren().isEmpty()) {
			column.getChildren().forEach(c -> createNestedCells(bean, c, prefixToRemove));
		} else {
			createCell(bean, column, propertyId);
		}
	}

	/**
	 * Creates a cell in the current sheet. The value will be retrieved by a
	 * property id from the given bean.
	 *
	 * A custom property id can be provided to customize the property id for
	 * properties in a collection.
	 *
	 * @param bean
	 *            The bean to get the data from.
	 * @param column
	 *            The bean column to get the value from.
	 * @param propertyId
	 *            The customized property id.
	 */
	private void createCell(Object bean, BeanColumnDefinition column, String propertyId) {
		if (postfixFilter.stream().anyMatch(f -> column.getPropertyId().endsWith(f))) {
			return;
		}

		if (column == null) {
			return;
		}
		CellValue cellValue = rows.get(columnCount++).get(rowIndex);

		if (propertyId == null) {
			propertyId = column.getPropertyId();
		}

		String value = mapper.getPropertyAsString(bean, propertyId, column);

		cellValue.setType(column.getType());

		if (Number.class.isAssignableFrom(column.getType()) && !value.isEmpty()) {
			try {
				double number = format.parse(value).doubleValue();
				// Missing values (NaN) stay empty. Up to Java 8 NaN was formatted as U+FFFD, which could not be parsed;
				// since Java 9 it is "NaN", which would end up as error value #NUM! in Excel
				if (!Double.isNaN(number)) {
					cellValue.setValue(number);
					cellValue.setFormat(column.getFormatDefinition());
				}
			} catch (ParseException e) {
				// No logging here because of enormous log output when downloading results
			}
		} else if (propertyId.endsWith(STRUCTURE_POSTFIX)) {
			cellValue.setValue(getImage(value));
		} else {
			cellValue.setValue(value);
		}
	}

	/**
	 * Create cells from a property which is a collection. Each column will be
	 * checked if it has children. If it so, the children will be a own columns
	 * in the file.
	 *
	 * @param bean
	 *            The bean to get the data from.
	 * @param column
	 *            The column which defines a list column.
	 */
	private void createListCells(Object bean, BeanColumnDefinition column) {
		Object value = mapper.getValue(bean, column.getPropertyId(), column);
		if (value == null) {
			return;
		}

		@SuppressWarnings("unchecked")
		Collection<?> collection = (Collection<?>) value;

		int currentColumnCount = columnCount;
		int rowID = rowIndex;

		for (Object listContainedBean : collection) {
			if (rowIndex >= nextRowIndex) {
				initRow();
			}
			// reset the column count to start right on the next line
			columnCount = currentColumnCount;
			createCollectionContentCells(listContainedBean, column);
			rowIndex++;
		}
		if (collection.isEmpty()) {
			columnCount += columnCounts.get(column);
		}
		rowIndex = rowID;
	}

	private void createCollectionContentCells(Object bean, BeanColumnDefinition column) {
		if (column.getChildren().isEmpty()) {
			createCell(bean, column, null);
		} else {
			column.getChildren().forEach(c -> createNestedCells(bean, c, column.getPropertyId()));
		}
	}

	private void initRow() {
		rows.forEach(c -> c.add(new CellValue()));
		nextRowIndex++;
	}

	private byte[] getImage(String smiles) {
		if (calculationService == null || !calculationService.isAvailable()) return null;
		return calculationService.getSmilesAsImage(smiles, 220, 220);
	}

	public static class CellValue {

		private Object value;
		private String format;
		private Class<?> type;

		public Optional<Object> getValue() {
			return Optional.ofNullable(value);
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public Optional<String> getFormat() {
			return Optional.ofNullable(format);
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public Class<?> getType() {
			return type;
		}

		public void setType(Class<?> type) {
			this.type = type;
		}
	}
}
