package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.hswt.fi.beans.BeanMappingConfiguration;
import de.hswt.fi.model.Score;
import de.hswt.fi.processing.service.model.ProcessCandidate;
import de.hswt.fi.processing.service.model.ProcessingUnit;
import de.hswt.fi.search.service.index.model.IndexSearchResult;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SourceList;
import de.hswt.fi.search.service.mass.search.model.properties.StringValueProperty;
import de.hswt.fi.ui.flow.processing.ProcessingSession;
import de.hswt.fi.ui.flow.views.Panels;
import de.hswt.fi.ui.flow.views.search.ResultsPanel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Result list of a processing run (AbstractProcessingResultsComponent and ProcessingResultsComponentLC of the Vaadin 8
 * UI): candidates grouped in result, scores, mass screening, RTI screening (if executed) and substance columns, a
 * filter field for most columns, the number of found and visible candidates, buttons for the summary, clearing the
 * filters, the downloads (results, features without results, complete) and reporting the candidates as faulty.
 */
public class ProcessingResultsPanel extends VerticalLayout {

	/** What the buttons of the panel do. */
	public interface Actions {

		void showSummary();

		void downloadResults(List<ProcessCandidate> visible);

		void downloadNonResults();

		void downloadComplete(List<ProcessCandidate> visible);

		void report(List<ProcessCandidate> visible);
	}

	private final Span count = new Span();

	private final HorizontalLayout header;

	private final List<Consumer<ProcessCandidate>> selectionListeners = new ArrayList<>();

	private final Map<Function<ProcessCandidate, String>, TextField> textFilters = new LinkedHashMap<>();

	private final Map<Function<ProcessCandidate, Double>, TextField> numberFilters = new LinkedHashMap<>();

	private Grid<ProcessCandidate> grid;

	private ListDataProvider<ProcessCandidate> dataProvider = new ListDataProvider<>(new ArrayList<>());

	public ProcessingResultsPanel(Actions actions) {
		setSizeFull();
		setPadding(false);
		addClassName("fi-panel");

		Button summary = Panels.iconButton(VaadinIcon.LINE_CHART, getTranslation("ui.processing.results.summary"),
				e -> actions.showSummary());
		Button clearFilter = Panels.iconButton(VaadinIcon.FILTER, getTranslation("ui.search.clearFilter"),
				e -> clearFilters());
		MenuBar download = new MenuBar();
		download.addThemeVariants(MenuBarVariant.LUMO_TERTIARY, MenuBarVariant.LUMO_ICON);
		MenuItem downloadItem = download.addItem(VaadinIcon.DOWNLOAD.create());
		downloadItem.setAriaLabel(getTranslation("ui.search.download"));
		download.setTooltipText(downloadItem, getTranslation("ui.search.download"));
		SubMenu downloads = downloadItem.getSubMenu();
		downloads.addItem(getTranslation("ui.processing.results.downloadResults"),
				e -> visible().ifPresent(actions::downloadResults));
		downloads.addItem(getTranslation("ui.processing.results.downloadNonResults"), e -> actions.downloadNonResults());
		downloads.addItem(getTranslation("ui.processing.results.downloadComplete"),
				e -> visible().ifPresent(actions::downloadComplete));
		Button report = Panels.iconButton(VaadinIcon.BUG, getTranslation("ui.search.reportResults"),
				e -> visible().ifPresent(actions::report));

		H3 title = new H3(getTranslation("ui.search.results"));
		count.addClassName("fi-panel-count");
		header = new HorizontalLayout(title, count, summary, clearFilter, download, report);
		header.addClassName("fi-panel-header");
		header.setWidthFull();
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.expand(count);
		add(header);
		setRun(null);
	}

	public void addSelectionListener(Consumer<ProcessCandidate> listener) {
		selectionListeners.add(listener);
	}

	/** Shows the candidates of a run; the columns depend on the processes the run executed. */
	public void setRun(ProcessingSession.Run run) {
		if (grid != null) {
			remove(grid);
		}
		textFilters.clear();
		numberFilters.clear();
		List<ProcessingUnit> units = run == null || run.job().getRequestedProcessUnits() == null
				? List.of() : run.job().getRequestedProcessUnits();
		grid = createGrid(units);
		dataProvider = new ListDataProvider<>(run == null ? new ArrayList<>() : new ArrayList<>(run.result().getResults()));
		grid.setDataProvider(dataProvider);
		add(grid);
		expand(grid);
		applyFilters();
		Optional.ofNullable(run).flatMap(r -> r.result().getResults().stream().findFirst())
				.ifPresentOrElse(grid::select, () -> selectionListeners.forEach(listener -> listener.accept(null)));
	}

	private Grid<ProcessCandidate> createGrid(List<ProcessingUnit> units) {
		Grid<ProcessCandidate> newGrid = new Grid<>();
		newGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
		newGrid.setSizeFull();
		newGrid.setColumnReorderingAllowed(true);
		newGrid.setEmptyStateText(getTranslation("ui.processing.results.none"));
		newGrid.asSingleSelect().addValueChangeListener(e -> selectionListeners.forEach(l -> l.accept(e.getValue())));
		// The filter fields get their row once all columns exist: the first header row holds the column captions
		Map<Grid.Column<ProcessCandidate>, TextField> filterRow = new LinkedHashMap<>();
		List<List<Grid.Column<ProcessCandidate>>> groups = new ArrayList<>();
		List<String> groupTitles = new ArrayList<>();

		// Results
		List<Grid.Column<ProcessCandidate>> results = new ArrayList<>();
		results.add(text(newGrid, filterRow, "ui.processing.results.target", ProcessCandidate::getTargetIdentifier, "8rem"));
		results.add(number(newGrid, filterRow, "ui.processing.results.rt",
				candidate -> candidate.getMassSearchResult().getTargetRetentionTime(), 1, "6rem"));
		results.add(text(newGrid, filterRow, "ui.processing.results.name", candidate -> value(candidate.getEntry().getName()), "12rem"));
		results.add(newGrid.addColumn(LitRenderer.<ProcessCandidate>of(
						"<vaadin-icon icon='vaadin:check-circle' class='fi-best-match' ?hidden=${!item.best}></vaadin-icon>")
						.withProperty("best", ProcessCandidate::isBestMatch))
				.setHeader(getTranslation("ui.processing.results.lookAt")).setWidth("6rem").setFlexGrow(0)
				.setComparator(ProcessCandidate::isBestMatch).setTooltipGenerator(candidate -> candidate.isBestMatch()
						? getTranslation("ui.processing.results.bestMatch") : null));
		results.add(score(newGrid, "ui.processing.results.score", ProcessCandidate::getScore,
				getTranslation("ui.processing.results.noScore")));
		groups.add(results);
		groupTitles.add(getTranslation("ui.processing.results.groupResults"));

		// Scores of the processes
		List<Grid.Column<ProcessCandidate>> scores = new ArrayList<>();
		if (units.contains(ProcessingUnit.MASS_SCREENING)) {
			scores.add(score(newGrid, "ui.processing.unit.MASS_SCREENING",
					candidate -> candidate.getMassSearchResult().getScore(), null));
		}
		if (units.contains(ProcessingUnit.RTI_SCREENING)) {
			scores.add(score(newGrid, "ui.processing.unit.RTI_SCREENING", candidate -> candidate.getIndexSearchResult() == null
					? null : candidate.getIndexSearchResult().getScore(), getTranslation("ui.processing.results.noRtiScore")));
		}
		if (units.contains(ProcessingUnit.MSMS)) {
			scores.add(score(newGrid, "ui.processing.results.msms", candidate -> candidate.getMsMsCandidate() == null
					? null : candidate.getMsMsCandidate().getScore(), getTranslation("ui.processing.results.noMsmsScore")));
		}
		if (units.contains(ProcessingUnit.MASSBANK_SIMPLE)) {
			scores.add(score(newGrid, "ui.processing.results.massbank", ProcessCandidate::getMassBankSimpleScore, null));
		}
		groups.add(scores);
		groupTitles.add(getTranslation("ui.processing.results.groupScores"));

		if (units.contains(ProcessingUnit.TP)) {
			groups.add(List.of(newGrid.addColumn(candidate -> candidate.getPathwayCandidate() != null ? "☑" : "☐")
					.setHeader(getTranslation("ui.processing.results.pathway")).setWidth("7rem").setFlexGrow(0)));
			groupTitles.add("");
		}

		// Mass screening
		groups.add(List.of(
				number(newGrid, filterRow, "ui.processing.results.targetMass",
						candidate -> candidate.getMassSearchResult().getTargetMass(), 4, "9rem"),
				number(newGrid, filterRow, "ui.processing.results.deltaMass",
						candidate -> candidate.getMassSearchResult().getDeltaMass(), 4, "8rem")));
		groupTitles.add(getTranslation("ui.processing.unit.MASS_SCREENING"));

		if (units.contains(ProcessingUnit.RTI_SCREENING)) {
			groups.add(rtiColumns(newGrid, filterRow));
			groupTitles.add(getTranslation("ui.processing.unit.RTI_SCREENING"));
		}

		// Substance
		groups.add(List.of(
				text(newGrid, filterRow, "ui.search.column.cas", candidate -> value(candidate.getEntry().getCas()), "8rem"),
				text(newGrid, filterRow, "ui.search.column.smiles", candidate -> value(candidate.getEntry().getSmiles()), "12rem"),
				text(newGrid, filterRow, "ui.search.column.iupac", candidate -> value(candidate.getEntry().getIupac()), "12rem"),
				text(newGrid, filterRow, "ui.search.column.formula",
						candidate -> value(candidate.getEntry().getElementalFormula()), "8rem"),
				text(newGrid, filterRow, "ui.processing.results.tonnage", candidate -> value(candidate.getEntry().getTonnage()), "8rem"),
				text(newGrid, filterRow, "ui.search.column.datasource", candidate -> datasource(candidate.getEntry()), "10rem")));
		groupTitles.add(getTranslation("ui.processing.results.groupSubstance"));

		HeaderRow filters = newGrid.appendHeaderRow();
		filterRow.forEach((column, field) -> filters.getCell(column).setComponent(field));
		HeaderRow groupRow = newGrid.prependHeaderRow();
		for (int i = 0; i < groups.size(); i++) {
			List<Grid.Column<ProcessCandidate>> columns = groups.get(i);
			if (columns.size() > 1) {
				groupRow.join(columns.toArray(Grid.Column[]::new)).setText(groupTitles.get(i));
			} else if (columns.size() == 1) {
				groupRow.getCell(columns.get(0)).setText(groupTitles.get(i));
			}
		}
		newGrid.getColumns().forEach(column -> column.setResizable(true));
		return newGrid;
	}

	private List<Grid.Column<ProcessCandidate>> rtiColumns(Grid<ProcessCandidate> newGrid,
														   Map<Grid.Column<ProcessCandidate>, TextField> filterRow) {
		Locale locale = getLocale();
		List<Grid.Column<ProcessCandidate>> columns = new ArrayList<>();
		columns.add(newGrid.addColumn(LitRenderer.<ProcessCandidate>of(
						"<span class='fi-ph-dependency fi-ph-${item.dependency}' title='${item.title}'></span>")
						.withProperty("dependency", candidate -> index(candidate).map(IndexSearchResult::getMoleculePhDependency)
								.map(dependency -> dependency.name().toLowerCase(Locale.ROOT)).orElse("none"))
						.withProperty("title", candidate -> index(candidate).map(IndexSearchResult::getMoleculePhDependency)
								.map(dependency -> BeanMappingConfiguration.translate(dependency.getI18nKey(), locale)).orElse("")))
				.setHeader(getTranslation("ui.processing.results.phDependency")).setWidth("6rem").setFlexGrow(0));
		columns.add(number(newGrid, filterRow, "ui.processing.results.rti",
				candidate -> index(candidate).map(IndexSearchResult::getRetentionTimeIndex).orElse(null), 1, "7rem"));
		columns.add(number(newGrid, filterRow, "ui.processing.results.logDDb",
				candidate -> index(candidate).map(IndexSearchResult::getResultLogD).orElse(null), 2, "7rem"));
		columns.add(number(newGrid, filterRow, "ui.processing.results.logDRti",
				candidate -> index(candidate).map(IndexSearchResult::getTargetLogD).orElse(null), 2, "7rem"));
		columns.add(number(newGrid, filterRow, "ui.processing.results.deltaLogDRtiDb",
				candidate -> index(candidate).map(IndexSearchResult::getDeltaLogDRtiDb).orElse(null), 2, "9rem"));
		columns.add(number(newGrid, filterRow, "ui.processing.results.adjustedLogD",
				candidate -> index(candidate).map(IndexSearchResult::getAdjustedLogD).orElse(null), 2, "8rem"));
		columns.add(number(newGrid, filterRow, "ui.processing.results.deltaLogDAdjustedDb",
				candidate -> index(candidate).map(IndexSearchResult::getDeltaLogDAdjustedDb).orElse(null), 2, "9rem"));
		return columns;
	}

	private static Optional<IndexSearchResult> index(ProcessCandidate candidate) {
		return Optional.ofNullable(candidate.getIndexSearchResult());
	}

	private Grid.Column<ProcessCandidate> text(Grid<ProcessCandidate> newGrid,
											   Map<Grid.Column<ProcessCandidate>, TextField> filterRow, String captionKey,
											   Function<ProcessCandidate, String> getter, String width) {
		Grid.Column<ProcessCandidate> column = newGrid.addColumn(getter::apply)
				.setHeader(getTranslation(captionKey)).setWidth(width).setFlexGrow(1)
				.setComparator(candidate -> Optional.ofNullable(getter.apply(candidate)).orElse("").toLowerCase(Locale.ROOT));
		TextField filter = filterField();
		textFilters.put(getter, filter);
		filterRow.put(column, filter);
		return column;
	}

	private Grid.Column<ProcessCandidate> number(Grid<ProcessCandidate> newGrid,
												 Map<Grid.Column<ProcessCandidate>, TextField> filterRow, String captionKey,
												 Function<ProcessCandidate, Double> getter, int decimals, String width) {
		Grid.Column<ProcessCandidate> column = newGrid.addColumn(candidate -> ProcessingTexts.number(getter.apply(candidate), decimals))
				.setHeader(getTranslation(captionKey)).setWidth(width).setFlexGrow(0)
				.setComparator(candidate -> Optional.ofNullable(getter.apply(candidate)).orElse(Double.NaN))
				.setPartNameGenerator(candidate -> "fi-number");
		TextField filter = filterField();
		numberFilters.put(getter, filter);
		filterRow.put(column, filter);
		return column;
	}

	/**
	 * Score column: a bar with the value between 0 and 1; a warning sign with an explanation if no score is available
	 * (null to show nothing then).
	 */
	private Grid.Column<ProcessCandidate> score(Grid<ProcessCandidate> newGrid, String captionKey,
												Function<ProcessCandidate, Score> getter, String missingText) {
		Function<ProcessCandidate, Double> value = candidate -> {
			Score score = getter.apply(candidate);
			Double scoreValue = score == null ? null : score.getScoreValue();
			return scoreValue == null || scoreValue.isNaN() || scoreValue < 0 ? null : scoreValue;
		};
		return newGrid.addColumn(LitRenderer.<ProcessCandidate>of(
						"<div class='fi-score-cell' title='${item.title}'>"
								+ "<div class='fi-score-bar' style='width:${item.percent}%'></div>"
								+ "<span class='${item.missing}'>${item.text}</span></div>")
						.withProperty("percent", candidate -> value.apply(candidate) == null ? 0
								: Math.round(Math.min(1, value.apply(candidate)) * 100))
						.withProperty("text", candidate -> value.apply(candidate) == null
								? (missingText == null ? "" : "⚠") : ProcessingTexts.number(value.apply(candidate), 2))
						.withProperty("missing", candidate -> value.apply(candidate) == null ? "fi-score-missing" : "")
						.withProperty("title", candidate -> value.apply(candidate) == null && missingText != null ? missingText : ""))
				.setHeader(getTranslation(captionKey)).setWidth("7rem").setFlexGrow(0)
				.setComparator(candidate -> Optional.ofNullable(value.apply(candidate)).orElse(-1d));
	}

	private TextField filterField() {
		TextField field = new TextField();
		field.setPlaceholder(getTranslation("ui.search.filter"));
		field.setClearButtonVisible(true);
		field.setValueChangeMode(ValueChangeMode.LAZY);
		field.setWidthFull();
		field.addThemeName("small");
		field.addValueChangeListener(e -> applyFilters());
		return field;
	}

	private void clearFilters() {
		textFilters.values().forEach(TextField::clear);
		numberFilters.values().forEach(TextField::clear);
	}

	private void applyFilters() {
		dataProvider.setFilter(candidate -> textFilters.entrySet().stream()
				.allMatch(filter -> matches(filter.getKey().apply(candidate), filter.getValue().getValue()))
				&& numberFilters.entrySet().stream()
				.allMatch(filter -> ResultsPanel.matchesMass(filter.getKey().apply(candidate), filter.getValue().getValue())));
		updateCount();
	}

	private static boolean matches(String value, String filter) {
		return filter == null || filter.isBlank()
				|| (value != null && value.toLowerCase(Locale.ROOT).contains(filter.trim().toLowerCase(Locale.ROOT)));
	}

	private Optional<List<ProcessCandidate>> visible() {
		List<ProcessCandidate> visible = dataProvider.getItems().stream()
				.filter(item -> dataProvider.getFilter() == null || dataProvider.getFilter().test(item)).toList();
		return visible.isEmpty() ? Optional.empty() : Optional.of(visible);
	}

	private void updateCount() {
		int found = dataProvider.getItems().size();
		int visibleCount = visible().map(List::size).orElse(0);
		count.setText(found == 0 ? "" : getTranslation("ui.search.results.count", found, visibleCount));
	}

	static String value(StringValueProperty property) {
		return property == null ? null : property.getValue();
	}

	static String datasource(Entry entry) {
		String name = entry.getDatasourceName();
		if (entry.getSourceLists() != null && !entry.getSourceLists().isEmpty()) {
			name += " [" + entry.getSourceLists().stream().map(SourceList::getName).collect(Collectors.joining(",")) + "]";
		}
		return name;
	}
}
