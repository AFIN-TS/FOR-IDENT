package de.hswt.fi.ui.flow.views.search;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import de.hswt.fi.ui.flow.dialogs.ExportDialog;
import de.hswt.fi.ui.flow.dialogs.ReportDialog;
import de.hswt.fi.ui.flow.layout.MainLayout;
import de.hswt.fi.ui.flow.search.SearchHistory;
import de.hswt.fi.ui.flow.search.SearchParameters;
import de.hswt.fi.ui.flow.search.SearchViewServices;
import de.hswt.fi.ui.flow.views.DatabaseSelection;
import de.hswt.fi.ui.flow.views.Panels;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Common frame of quick search, search and index search (AbstractSearchView of the Vaadin 8 UI): on the left the search
 * form, the data sources and the search history; in the middle the results; on the right the details of the selected
 * substance. Search results are kept in the session history and reused for the same parameters.
 */
public abstract class AbstractSearchView extends SplitLayout implements HasDynamicTitle, MainLayout.HasViewTitle {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	protected final transient SearchViewServices services;

	private final DatabaseSelection databases;

	private final Grid<SearchHistory.Run> history = new Grid<>();

	private final ResultsPanel results;

	private final DetailsPanel details;

	private SearchHistory.Run current;

	protected AbstractSearchView(SearchViewServices services) {
		this.services = services;
		databases = new DatabaseSelection(services.search());
		setSizeFull();
		addClassName("fi-search-view");

		results = new ResultsPanel(this::download, entries -> ReportDialog.forEntries(services, entries).open());
		details = new DetailsPanel(services, entry -> download(List.of(entry)),
				entry -> ReportDialog.forEntries(services, List.of(entry)).open());
		results.addSelectionListener(details::setEntry);

		SplitLayout resultsAndDetails = new SplitLayout(results, details);
		resultsAndDetails.setSplitterPosition(58);
		resultsAndDetails.setSizeFull();

		addToPrimary(createLeftColumn());
		addToSecondary(resultsAndDetails);
		setSplitterPosition(24);
	}

	/** The search form of the view. */
	protected abstract Component createForm();

	protected abstract SearchHistory.Kind getKind();

	private Component createLeftColumn() {
		VerticalLayout form = Panels.panel(getTranslation("ui.search.form"), createForm());
		VerticalLayout sources = Panels.panel(getTranslation("ui.search.databases"), databases);
		VerticalLayout historyPanel = Panels.panel(getTranslation("ui.search.history"), createHistory(), historyButtons());
		VerticalLayout column = new VerticalLayout(form, sources, historyPanel);
		column.setPadding(false);
		column.setSpacing(true);
		column.setSizeFull();
		column.getStyle().set("overflow", "auto");
		// The panels keep their height; the column scrolls
		form.getStyle().set("flex-shrink", "0");
		sources.getStyle().set("flex-shrink", "0");
		historyPanel.getStyle().set("flex-shrink", "0").set("min-height", "10rem");
		return column;
	}

	private Component createHistory() {
		history.addColumn(this::historyLabel).setFlexGrow(1);
		history.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER);
		history.setAllRowsVisible(true);
		history.setEmptyStateText(getTranslation("ui.search.history.empty"));
		history.setTooltipGenerator(run -> SearchParameters.describe(run.parameter(), this::getTranslation));
		history.asSingleSelect().addValueChangeListener(e -> {
			if (e.isFromClient() && e.getValue() != null) {
				show(e.getValue());
			}
		});
		refreshHistory();
		return history;
	}

	private Component[] historyButtons() {
		Button clear = new Button(VaadinIcon.ERASER.create(), e -> {
			services.history().clear();
			refreshHistory();
		});
		clear.setTooltipText(getTranslation("ui.search.history.clear"));
		Button report = new Button(VaadinIcon.BUG.create(), e -> Optional.ofNullable(current).ifPresent(run ->
				ReportDialog.forSearch(services, run.parameter(),
						SearchParameters.describe(run.parameter(), this::getTranslation)).open()));
		report.setTooltipText(getTranslation("ui.search.reportSearch"));
		for (Button button : List.of(clear, report)) {
			button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
			button.setAriaLabel(button.getTooltip().getText());
		}
		return new Component[] {clear, report};
	}

	private String historyLabel(SearchHistory.Run run) {
		String kind = switch (run.kind()) {
			case QUICK -> getTranslation("ui.search.history.quick");
			case INDEX -> getTranslation("ui.search.history.index") + " " + Character.toUpperCase(run.parameter().getIndexChar());
			case SEARCH -> getTranslation("ui.search.history.search");
		};
		return run.time().format(TIME) + "  " + kind + " (" + run.results().size() + "): "
				+ SearchParameters.describe(run.parameter(), this::getTranslation);
	}

	private void refreshHistory() {
		history.setItems(services.history().getRuns());
		if (current != null) {
			history.asSingleSelect().setValue(services.history().find(current.parameter()).orElse(null));
		}
	}

	/**
	 * Runs a search with the parameters of the form, or shows the earlier result of the same search. Shows a message if
	 * no parameter is set or no data source is selected.
	 */
	protected void search(SearchParameter parameter) {
		if (SearchParameters.isEmpty(parameter)) {
			warn("ui.search.missing.title", "ui.search.missing.message");
			return;
		}
		List<CompoundSearchService> selected = services.search().selectDatabases(parameter,
				databases.getSelected());
		if (selected.isEmpty()) {
			warn("ui.search.noDatabase.title", "ui.search.noDatabase.message");
			return;
		}
		SearchHistory.Run run = services.history().find(parameter).orElseGet(() -> {
			SearchHistory.Run newRun = new SearchHistory.Run(getKind(), parameter,
					services.search().search(parameter, selected), LocalTime.now());
			services.history().add(newRun);
			return newRun;
		});
		show(run);
		refreshHistory();
		if (run.results().isEmpty()) {
			Notification.show(getTranslation("ui.search.results.none"), 3000, Notification.Position.MIDDLE);
		}
	}

	/** Shows a search of the history; the form shows its parameters. */
	private void show(SearchHistory.Run run) {
		current = run;
		results.setResults(run.results());
		if (run.kind() == getKind()) {
			showParameter(run.parameter());
		}
	}

	/** Fills the form with the parameters of an earlier search. */
	protected abstract void showParameter(SearchParameter parameter);

	private void download(List<Entry> entries) {
		String fileName = entries.size() == 1 && entries.get(0).getName() != null
				? entries.get(0).getName().getValue() : "results";
		new ExportDialog<>(services, entries, fileName, getLocale()).open();
	}

	protected void warn(String titleKey, String messageKey, Object... parameters) {
		Notification.show(getTranslation(titleKey) + ": " + getTranslation(messageKey, parameters), 4000,
				Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
	}

	@Override
	public String getPageTitle() {
		return getViewTitle() + " | " + services.properties().getUi().getHeader().getCaption();
	}
}
