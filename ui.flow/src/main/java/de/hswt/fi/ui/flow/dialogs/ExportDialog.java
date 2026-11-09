package de.hswt.fi.ui.flow.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.hswt.fi.beans.BeanComponentMapper;
import de.hswt.fi.export.service.excel.ExcelFileDefinition;
import de.hswt.fi.export.service.excel.ExcelSheetDefinition;
import de.hswt.fi.ui.flow.search.SearchViewServices;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Excel download of search results (DownloadWindow of the Vaadin 8 UI): file name, columns to export, optionally the
 * sources of the values. Column captions and number formats follow the language of the user interface. With
 * {@link Options} further sheets (e.g. the parameters of a processing run) and the sheets of the uploaded Excel file can
 * be added.
 */
public class ExportDialog<T> extends Dialog {

	/**
	 * Further content of the file.
	 *
	 * @param sheetName   name of the sheet with the entries
	 * @param columnIds   the columns that can be exported (null: all columns of the entries)
	 * @param extraSheets sheets added after the entries
	 * @param sourceFile  uploaded file whose sheets can be added (only Excel files, otherwise null)
	 */
	public record Options(String sheetName, List<String> columnIds, List<ExcelSheetDefinition<?>> extraSheets,
						  Path sourceFile) {
	}

	private static final String SUFFIX = ".xlsx";

	private final transient SearchViewServices services;

	private final List<T> entries;

	private final Locale locale;

	private final TextField fileName = new TextField();

	private final CheckboxGroup<String> columns = new CheckboxGroup<>();

	private final Checkbox includeSources = new Checkbox();

	private final Checkbox includeSourceFile = new Checkbox();

	private final Options options;

	public ExportDialog(SearchViewServices services, List<T> entries, String defaultFileName, Locale locale) {
		this(services, entries, defaultFileName, locale, null);
	}

	public ExportDialog(SearchViewServices services, List<T> entries, String defaultFileName, Locale locale,
						Options options) {
		this.services = services;
		this.entries = List.copyOf(entries);
		this.locale = locale;
		this.options = options;
		setHeaderTitle(getTranslation("ui.export.title"));
		setWidth("min(40rem, 95vw)");

		BeanComponentMapper mapper = services.mapper(locale);
		T first = this.entries.get(0);
		List<String> columnIds = options != null && options.columnIds() != null
				? options.columnIds() : mapper.getSelectorColumns(first);
		Map<String, String> captions = mapper.getPropertyCaptions(first, columnIds);

		fileName.setLabel(getTranslation("ui.export.fileName"));
		fileName.setValue(fileNameOf(defaultFileName));
		fileName.setMaxLength(255);
		fileName.setWidthFull();
		columns.setLabel(getTranslation("ui.export.columns"));
		columns.setItems(columnIds);
		columns.setItemLabelGenerator(id -> captions.getOrDefault(id, id));
		columns.setValue(new LinkedHashSet<>(columnIds));
		columns.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
		includeSources.setLabel(getTranslation("ui.export.includeSources"));

		Button selectAll = new Button(getTranslation("ui.export.selectAll"),
				e -> columns.setValue(new LinkedHashSet<>(columnIds)));
		Button deselectAll = new Button(getTranslation("ui.export.deselectAll"), e -> columns.clear());
		selectAll.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
		deselectAll.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
		add(fileName, new HorizontalLayout(selectAll, deselectAll), columns, includeSources);
		if (options != null && options.sourceFile() != null) {
			includeSourceFile.setLabel(getTranslation("ui.export.includeSourceFile"));
			includeSourceFile.setValue(true);
			add(includeSourceFile);
		}

		Anchor download = new Anchor(DownloadHandler.fromInputStream(event -> {
			byte[] file = createFile(columnIds);
			return new DownloadResponse(new ByteArrayInputStream(file), fileNameOf(fileName.getValue()),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.length);
		}), AttachmentType.DOWNLOAD, "");
		Button downloadButton = new Button(getTranslation("ui.export.download"), VaadinIcon.DOWNLOAD.create());
		downloadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		download.add(downloadButton);
		columns.addValueChangeListener(e -> downloadButton.setEnabled(!e.getValue().isEmpty()));
		Button close = new Button(getTranslation("ui.button.close"), e -> close());
		getFooter().add(close, download);
	}

	/** The selected columns in the original order. */
	private byte[] createFile(List<String> columnIds) {
		List<String> selected = new ArrayList<>(columnIds);
		selected.retainAll(columns.getValue());
		ExcelSheetDefinition<T> sheet = new ExcelSheetDefinition<>(options != null && options.sheetName() != null
				? options.sheetName() : getTranslation("ui.export.sheet"));
		sheet.setData(entries);
		sheet.setColumnPropertyIds(selected);
		sheet.setIncludeSource(includeSources.getValue());
		sheet.setCreateIndex(true);
		ExcelFileDefinition definition = new ExcelFileDefinition();
		definition.setFilename(fileNameOf(fileName.getValue()));
		List<ExcelSheetDefinition> sheets = new ArrayList<>(List.of(sheet));
		if (options != null && options.extraSheets() != null) {
			sheets.addAll(options.extraSheets());
		}
		definition.setSheets(sheets);
		if (options != null && options.sourceFile() != null) {
			definition.setSourceFile(options.sourceFile());
			definition.setAddSources(includeSourceFile.getValue());
		}
		try (Workbook workbook = services.excelCreator(locale).createWorkbook(definition);
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** File name with the suffix .xlsx and without characters that are not allowed in file names. */
	static String fileNameOf(String name) {
		String clean = name == null ? "" : name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
		if (clean.isEmpty() || clean.equalsIgnoreCase(SUFFIX)) {
			clean = "results";
		}
		return clean.toLowerCase(Locale.ROOT).endsWith(SUFFIX) ? clean : clean + SUFFIX;
	}
}
