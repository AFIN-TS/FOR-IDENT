package de.hswt.fi.ui.flow.views.search;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.hswt.fi.common.RangeParser;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.properties.NumberValueProperty;
import de.hswt.fi.search.service.mass.search.model.properties.StringValueProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result list of a search (SearchResultsComponent of the Vaadin 8 UI): sortable columns with a filter field each,
 * number of found and visible entries, buttons to clear the filters, to download the visible results and to report
 * them as faulty.
 */
public class ResultsPanel extends VerticalLayout {

	private final Grid<Entry> grid = new Grid<>();

	private final Span count = new Span();

	private final Map<Function<Entry, String>, TextField> textFilters = new LinkedHashMap<>();

	private final TextField massFilter = new TextField();

	private ListDataProvider<Entry> dataProvider = new ListDataProvider<>(new ArrayList<>());

	public ResultsPanel(Consumer<List<Entry>> download, Consumer<List<Entry>> report) {
		setSizeFull();
		setPadding(false);
		addClassName("fi-panel");

		Button clearFilter = iconButton(VaadinIcon.FILTER, "ui.search.clearFilter", e -> clearFilters());
		Button downloadButton = iconButton(VaadinIcon.DOWNLOAD, "ui.search.download", e -> visibleEntries()
				.filter(entries -> !entries.isEmpty()).ifPresent(download));
		Button reportButton = iconButton(VaadinIcon.BUG, "ui.search.reportResults", e -> visibleEntries()
				.filter(entries -> !entries.isEmpty()).ifPresent(report));
		H3 title = new H3(getTranslation("ui.search.results"));
		count.addClassName("fi-panel-count");
		HorizontalLayout header = new HorizontalLayout(title, count, clearFilter, downloadButton, reportButton);
		header.addClassName("fi-panel-header");
		header.setWidthFull();
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.expand(count);

		addTextColumn(entry -> value(entry.getName()), "ui.search.column.name", 2);
		addTextColumn(entry -> value(entry.getCas()), "ui.search.column.cas", 1);
		addTextColumn(entry -> value(entry.getSmiles()), "ui.search.column.smiles", 2);
		addTextColumn(entry -> value(entry.getIupac()), "ui.search.column.iupac", 2);
		addTextColumn(entry -> value(entry.getElementalFormula()), "ui.search.column.formula", 1);
		Grid.Column<Entry> mass = grid.addColumn(entry -> formatMass(mass(entry)))
				.setHeader(getTranslation("ui.search.column.mass"))
				.setComparator(entry -> Optional.ofNullable(mass(entry)).orElse(Double.NaN))
				.setResizable(true).setFlexGrow(1);
		addTextColumn(Entry::getDatasourceName, "ui.search.column.datasource", 1);

		HeaderRow filterRow = grid.appendHeaderRow();
		textFilters.forEach((getter, field) -> filterRow.getCell(columnOf(getter)).setComponent(field));
		configureFilterField(massFilter);
		filterRow.getCell(mass).setComponent(massFilter);

		grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
		grid.setSizeFull();
		grid.setColumnReorderingAllowed(true);
		grid.setDataProvider(dataProvider);
		grid.setEmptyStateText(getTranslation("ui.search.results.none"));
		add(header, grid);
		expand(grid);
		updateCount();
	}

	private final Map<Function<Entry, String>, Grid.Column<Entry>> columns = new LinkedHashMap<>();

	private void addTextColumn(Function<Entry, String> getter, String captionKey, int flexGrow) {
		Grid.Column<Entry> column = grid.addColumn(getter::apply)
				.setHeader(getTranslation(captionKey))
				.setComparator(entry -> Optional.ofNullable(getter.apply(entry)).orElse("").toLowerCase(Locale.ROOT))
				.setResizable(true).setFlexGrow(flexGrow);
		columns.put(getter, column);
		TextField filter = new TextField();
		configureFilterField(filter);
		textFilters.put(getter, filter);
	}

	private Grid.Column<Entry> columnOf(Function<Entry, String> getter) {
		return columns.get(getter);
	}

	private void configureFilterField(TextField field) {
		field.setPlaceholder(getTranslation("ui.search.filter"));
		field.setClearButtonVisible(true);
		field.setValueChangeMode(ValueChangeMode.LAZY);
		field.setWidthFull();
		field.addValueChangeListener(e -> applyFilters());
	}

	private Button iconButton(VaadinIcon icon, String tooltipKey,
							  com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<Button>> listener) {
		Button button = new Button(icon.create(), listener);
		button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
		button.setTooltipText(getTranslation(tooltipKey));
		button.setAriaLabel(getTranslation(tooltipKey));
		return button;
	}

	/** Shows the results of a search; the first entry is selected (as before). */
	public void setResults(List<Entry> entries) {
		dataProvider = new ListDataProvider<>(new ArrayList<>(entries));
		grid.setDataProvider(dataProvider);
		applyFilters();
		entries.stream().findFirst().ifPresentOrElse(grid::select, grid::deselectAll);
	}

	public void addSelectionListener(Consumer<Entry> listener) {
		grid.asSingleSelect().addValueChangeListener(e -> listener.accept(e.getValue()));
	}

	private void clearFilters() {
		textFilters.values().forEach(TextField::clear);
		massFilter.clear();
	}

	private void applyFilters() {
		dataProvider.setFilter(entry -> textFilters.entrySet().stream().allMatch(filter -> matches(
				filter.getKey().apply(entry), filter.getValue().getValue())) && matchesMass(mass(entry), massFilter.getValue()));
		updateCount();
	}

	private static boolean matches(String value, String filter) {
		return filter == null || filter.isBlank()
				|| (value != null && value.toLowerCase(Locale.ROOT).contains(filter.trim().toLowerCase(Locale.ROOT)));
	}

	/** Mass filter: a range ("188-189", ">200") or the beginning of the value ("188.0"). */
	public static boolean matchesMass(Double mass, String filter) {
		if (filter == null || filter.isBlank()) {
			return true;
		}
		if (mass == null) {
			return false;
		}
		RangeParser range = new RangeParser();
		if (range.parse(filter)) {
			return mass >= range.getMin() && mass <= range.getMax();
		}
		return formatMass(mass).startsWith(filter.trim().replace(',', '.'));
	}

	private Optional<List<Entry>> visibleEntries() {
		return Optional.of(dataProvider.getItems().stream().filter(item -> dataProvider.getFilter() == null
				|| dataProvider.getFilter().test(item)).toList());
	}

	private void updateCount() {
		int found = dataProvider.getItems().size();
		int visible = visibleEntries().map(List::size).orElse(0);
		count.setText(found == 0 ? "" : getTranslation("ui.search.results.count", found, visible));
	}

	static String value(StringValueProperty property) {
		return property == null ? null : property.getValue();
	}

	static Double mass(Entry entry) {
		NumberValueProperty mass = entry.getAccurateMass();
		return mass == null ? null : mass.getValue();
	}

	static String formatMass(Double mass) {
		return mass == null ? "" : String.format(Locale.ROOT, "%.4f", mass);
	}
}
