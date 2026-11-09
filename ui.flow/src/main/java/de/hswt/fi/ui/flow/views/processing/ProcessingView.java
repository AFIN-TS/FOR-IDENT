package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import de.hswt.fi.common.Ionisation;
import de.hswt.fi.common.StationaryPhase;
import de.hswt.fi.export.service.excel.ExcelSheetDefinition;
import de.hswt.fi.model.Feature;
import de.hswt.fi.processing.service.model.ProcessCandidate;
import de.hswt.fi.processing.service.model.ProcessingJob;
import de.hswt.fi.processing.service.model.ProcessingSettings;
import de.hswt.fi.processing.service.model.ProcessingUnit;
import de.hswt.fi.processing.service.model.ProcessingUnitState;
import de.hswt.fi.processing.service.model.ProcessingUnitState.UnitState;
import de.hswt.fi.processing.service.model.ScoreSettings;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import de.hswt.fi.ui.flow.dialogs.ExportDialog;
import de.hswt.fi.ui.flow.dialogs.ReportDialog;
import de.hswt.fi.ui.flow.layout.MainLayout;
import de.hswt.fi.ui.flow.processing.ProcessingSession;
import de.hswt.fi.ui.flow.processing.ProcessingSession.Run;
import de.hswt.fi.ui.flow.processing.ProcessingSession.UploadedFile;
import de.hswt.fi.ui.flow.processing.ProcessingViewServices;
import de.hswt.fi.ui.flow.processing.ScoreWeights;
import de.hswt.fi.ui.flow.views.DatabaseSelection;
import de.hswt.fi.ui.flow.views.Panels;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Molecular screening (ProcessingView of the Vaadin 8 UI), for logged in users: on the left the files, the processes
 * with their weights, the parameters, the data sources and the runs of the session; in the middle the results, the
 * content of the selected file and the help; on the right the details of the selected candidate or the spectrum of
 * the selected feature. The processing runs in the background with a progress dialog.
 */
@Route(value = "processing", layout = MainLayout.class)
@PermitAll
public class ProcessingView extends SplitLayout implements HasDynamicTitle, MainLayout.HasViewTitle,
		ProcessingSession.Listener {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	static final List<Double> PH_VALUES = List.of(3.0, 5.0, 7.0, 9.0);

	static final List<Double> PPM_VALUES = List.of(1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 500.0, 1000.0, 1500.0,
			2000.0, 5000.0);

	private final transient ProcessingViewServices services;

	private final boolean tpEnabled;

	private final Grid<UploadedFile> files = new Grid<>();

	private final Span calibrationInfo = new Span();

	private final Div processes = new Div();

	private final Select<Double> ph = new Select<>();

	private final ComboBox<Double> precursorPpm = new ComboBox<>();

	private final ComboBox<Double> fragmentPpm = new ComboBox<>();

	private final NumberField intensityThreshold = new NumberField();

	private final Select<Ionisation> ionisation = new Select<>();

	private final Select<StationaryPhase> stationaryPhase = new Select<>();

	private final Checkbox useFormulae = new Checkbox();

	private final NumberField formulaPpm = new NumberField();

	private final DatabaseSelection databases;

	private final Grid<Run> history = new Grid<>();

	private final TabSheet center = new TabSheet();

	private final Tab resultsTab;

	private final Tab dataTab;

	private final ProcessingResultsPanel results;

	private final FeatureDataPanel data;

	private final CandidateDetailsPanel details;

	private final SpectrumPanel spectrum = new SpectrumPanel();

	private final Div right = new Div();

	private Run current;

	private ProgressDialog progress;

	public ProcessingView(ProcessingViewServices services,
						  @Value("${spring.datasource.pathway.enabled:false}") boolean tpEnabled,
						  @Value("${html-help-processing-english:processing_help_en.html}") String englishHelp,
						  @Value("${html-help-processing-german:processing_help_de.html}") String germanHelp) {
		this.services = services;
		this.tpEnabled = tpEnabled;
		setSizeFull();
		addClassName("fi-search-view");
		databases = new DatabaseSelection(services.search().search());

		results = new ProcessingResultsPanel(new ResultActions());
		data = new FeatureDataPanel(spectrum::setFeature);
		details = new CandidateDetailsPanel(services.search(), entry -> downloadEntry(entry),
				entry -> ReportDialog.forEntries(services.search(), List.of(entry)).open());
		results.addSelectionListener(details::setCandidate);

		boolean german = "de".equals(getLocale().getLanguage());
		IFrame help = new IFrame("/" + (german ? germanHelp : englishHelp));
		help.setSizeFull();
		help.getElement().setAttribute("title", getTranslation("ui.processing.help"));
		help.addClassName("fi-help-frame");

		center.setSizeFull();
		resultsTab = center.add(getTranslation("ui.search.results"), results);
		dataTab = center.add(getTranslation("ui.processing.data.title"), data);
		center.add(getTranslation("ui.processing.help"), help);
		center.setSelectedIndex(2);
		center.addSelectedChangeListener(e -> showRight());

		right.setSizeFull();
		SplitLayout centerAndRight = new SplitLayout(center, right);
		centerAndRight.setSplitterPosition(62);
		centerAndRight.setSizeFull();
		addToPrimary(createLeftColumn());
		addToSecondary(centerAndRight);
		setSplitterPosition(24);

		refreshFiles();
		refreshHistory();
		showRight();
	}

	private Component createLeftColumn() {
		Button upload = Panels.iconButton(VaadinIcon.UPLOAD, getTranslation("ui.processing.files.add"),
				e -> new UploadDialog(services, this::refreshFiles).open());
		Button clearFiles = Panels.iconButton(VaadinIcon.ERASER, getTranslation("ui.processing.files.clear"),
				e -> clearFiles());
		Button editProcesses = Panels.iconButton(VaadinIcon.COG, getTranslation("ui.processing.score.edit"),
				e -> editProcesses());
		Button start = new Button(getTranslation("ui.processing.start"), VaadinIcon.PLAY.create(), e -> start());
		start.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
		Button clearHistory = Panels.iconButton(VaadinIcon.ERASER, getTranslation("ui.search.history.clear"), e -> {
			services.session().clearRuns();
			refreshHistory();
		});

		VerticalLayout column = new VerticalLayout(
				Panels.panel(getTranslation("ui.processing.files.title"), createFileList(), upload, clearFiles),
				Panels.panel(getTranslation("ui.processing.processes.title"), processes, editProcesses),
				Panels.panel(getTranslation("ui.processing.parameters.title"), createParameters(), start),
				Panels.panel(getTranslation("ui.search.databases"), databases),
				Panels.panel(getTranslation("ui.processing.history.title"), createHistory(), clearHistory));
		column.setPadding(false);
		column.setSpacing(true);
		column.setSizeFull();
		column.getStyle().set("overflow", "auto");
		// The panels keep their height; the column scrolls
		column.getChildren().forEach(panel -> panel.getElement().getStyle().set("flex-shrink", "0"));
		return column;
	}

	private Component createFileList() {
		files.addColumn(UploadedFile::name).setFlexGrow(1).setTooltipGenerator(UploadedFile::name);
		files.addColumn(file -> file.job().getDataSize()).setFlexGrow(0).setWidth("5rem")
				.setTooltipGenerator(file -> getTranslation("ui.processing.files.targets"));
		files.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER);
		files.setAllRowsVisible(true);
		files.setEmptyStateText(getTranslation("ui.processing.files.none"));
		files.asSingleSelect().addValueChangeListener(e -> fileSelected(e.getValue()));
		files.addItemDoubleClickListener(e -> start());
		calibrationInfo.addClassName("fi-hint");
		VerticalLayout layout = new VerticalLayout(files, calibrationInfo);
		layout.setPadding(false);
		layout.setSpacing(false);
		return layout;
	}

	private Component createParameters() {
		ProcessingSettings parameters = services.session().getParameters();
		ph.setLabel("pH");
		ph.setItems(PH_VALUES);
		ph.setItemLabelGenerator(ProcessingView::formatNumber);
		ph.setValue(PH_VALUES.contains(parameters.getPh()) ? parameters.getPh() : PH_VALUES.get(0));
		ph.addValueChangeListener(e -> parameters.setPh(e.getValue()));

		configurePpm(precursorPpm, "ui.processing.parameters.precursorPpm", parameters.getRequestedPrecursorPpm());
		precursorPpm.addValueChangeListener(e -> parameters.setRequestedPrecursorPpm(e.getValue()));
		configurePpm(fragmentPpm, "ui.processing.parameters.fragmentPpm", parameters.getPpmFragments());
		fragmentPpm.addValueChangeListener(e -> parameters.setPpmFragments(e.getValue()));

		intensityThreshold.setLabel(getTranslation("ui.processing.parameters.intensityThreshold"));
		intensityThreshold.setMin(0);
		intensityThreshold.setValue(parameters.getIntensityThreshold());
		intensityThreshold.addValueChangeListener(e -> parameters.setIntensityThreshold(
				e.getValue() == null || e.getValue() < 0 ? 0 : e.getValue()));

		ionisation.setLabel(getTranslation("ui.search.ionisation"));
		ionisation.setItems(Ionisation.values());
		ionisation.setValue(parameters.getIonisation());
		ionisation.addValueChangeListener(e -> parameters.setIonisation(e.getValue()));

		stationaryPhase.setLabel(getTranslation("ui.processing.parameters.stationaryPhase"));
		stationaryPhase.setItems(StationaryPhase.values());
		stationaryPhase.setValue(parameters.getStationaryPhase());
		stationaryPhase.addValueChangeListener(e -> parameters.setStationaryPhase(e.getValue()));

		// Topic T9: masses calculated from the formulae of the file, with their own tolerance
		useFormulae.setLabel(getTranslation("ui.processing.parameters.useFormulae"));
		useFormulae.setValue(parameters.isUseFormulae());
		useFormulae.setTooltipText(getTranslation("ui.processing.parameters.useFormulaeHint"));
		formulaPpm.setLabel(getTranslation("ui.processing.parameters.formulaPpm"));
		formulaPpm.setHelperText(getTranslation("ui.processing.parameters.formulaPpmHint"));
		formulaPpm.setMin(0);
		formulaPpm.setStep(0.01);
		formulaPpm.setValue(parameters.getFormulaDerivedMassesPpm());
		formulaPpm.setEnabled(parameters.isUseFormulae());
		useFormulae.addValueChangeListener(e -> {
			parameters.setUseFormulae(e.getValue());
			formulaPpm.setEnabled(e.getValue());
		});
		formulaPpm.addValueChangeListener(e -> parameters.setFormulaDerivedMassesPpm(
				e.getValue() == null || e.getValue() < 0 ? 0 : e.getValue()));

		VerticalLayout layout = new VerticalLayout(ph, precursorPpm, fragmentPpm, intensityThreshold, ionisation,
				stationaryPhase, useFormulae, formulaPpm);
		layout.setPadding(false);
		layout.setSpacing(false);
		layout.getChildren().forEach(field -> field.getElement().getStyle().set("width", "100%"));
		return layout;
	}

	private void configurePpm(ComboBox<Double> field, String labelKey, double value) {
		field.setLabel(getTranslation(labelKey));
		field.setItems(PPM_VALUES);
		field.setItemLabelGenerator(ProcessingView::formatNumber);
		field.setAllowCustomValue(false);
		field.setValue(PPM_VALUES.contains(value) ? value : PPM_VALUES.get(2));
	}

	static String formatNumber(Double value) {
		if (value == null) {
			return "";
		}
		return value == Math.rint(value) ? String.valueOf(value.longValue()) : String.valueOf(value);
	}

	private Component createHistory() {
		history.addColumn(run -> run.time().format(TIME) + "  " + run.fileName() + " (" + run.result().getResults().size() + ")")
				.setFlexGrow(1);
		history.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER);
		history.setAllRowsVisible(true);
		history.setEmptyStateText(getTranslation("ui.processing.history.empty"));
		history.setTooltipGenerator(this::describe);
		history.asSingleSelect().addValueChangeListener(e -> {
			if (e.isFromClient() && e.getValue() != null) {
				show(e.getValue());
			}
		});
		return history;
	}

	private String describe(Run run) {
		ProcessingSettings settings = run.key().settings();
		return run.fileName() + ": pH " + formatNumber(settings.getPh()) + ", "
				+ formatNumber(settings.getRequestedPrecursorPpm()) + " ppm, " + settings.getIonisation() + ", "
				+ settings.getStationaryPhase() + "; " + String.join(", ", run.key().databases());
	}

	private Optional<UploadedFile> selectedFile() {
		return files.asSingleSelect().getOptionalValue();
	}

	private void refreshFiles() {
		ProcessingSession session = services.session();
		Optional<String> selected = selectedFile().map(UploadedFile::name);
		files.setItems(session.getFiles());
		Optional<UploadedFile> toSelect = selected.flatMap(session::findFile)
				.or(() -> session.getFiles().stream().reduce((first, second) -> second));
		toSelect.ifPresentOrElse(files::select, () -> fileSelected(null));
		calibrationInfo.setText(session.getCalibrationFile()
				.map(file -> getTranslation("ui.processing.files.calibration", file.name())).orElse(""));
	}

	private void fileSelected(UploadedFile file) {
		data.setFeatureSet(file == null ? null : file.job().getFeatureSet());
		spectrum.setFeature(null);
		refreshProcesses();
		if (file != null && current == null) {
			center.setSelectedTab(dataTab);
		}
	}

	private void clearFiles() {
		ProcessingSession session = services.session();
		List<Path> stored = new ArrayList<>();
		session.getFiles().forEach(file -> stored.add(file.path()));
		session.getCalibrationFile().ifPresent(file -> stored.add(file.path()));
		for (Path path : stored) {
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				warn("ui.processing.upload.failed");
			}
		}
		session.clearFiles();
		session.setCalibrationFile(null);
		refreshFiles();
	}

	/** Overview of the processes of the selected file (ScoreSettingsComponent of the Vaadin 8 UI). */
	private void refreshProcesses() {
		processes.removeAll();
		processes.addClassName("fi-process-table");
		Optional<UploadedFile> file = selectedFile();
		if (file.isEmpty()) {
			processes.add(new Span(getTranslation("ui.processing.files.selectFirst")));
			return;
		}
		ScoreSettings settings = file.get().job().getSettings().getScoreSettings();
		Map<ProcessingUnit, Integer> percent = ScoreDialog.percent(settings);
		for (ProcessingUnitState state : ScoreDialog.states(settings, tpEnabled)) {
			Span name = new Span(ProcessingTexts.unit(this, state.getProcessUnit()));
			if (!state.isEnabled()) {
				name.addClassName("fi-disabled-text");
			}
			String weight = !state.isScoreable() || !state.isEnabled() ? ""
					: percent.getOrDefault(state.getProcessUnit(), 0) + " %";
			processes.add(name, ProcessingTexts.availableIcon(this, state.isDataAvailable()), new Span(weight));
		}
		int sum = ScoreWeights.sum(percent);
		if (sum != ScoreWeights.TOTAL) {
			Span warning = new Span(getTranslation("ui.processing.score.sumWarning", sum));
			warning.addClassName("fi-score-sum-wrong");
			processes.add(warning);
		}
	}

	private void editProcesses() {
		selectedFile().ifPresentOrElse(
				file -> new ScoreDialog(file.job().getSettings().getScoreSettings(), tpEnabled, this::refreshProcesses).open(),
				() -> warn("ui.processing.files.selectFirst"));
	}

	private void refreshHistory() {
		List<Run> runs = services.session().getRuns();
		history.setItems(runs);
		if (current != null && runs.contains(current)) {
			history.select(current);
		}
	}

	/**
	 * Starts the processing of the selected file with the parameters of the form, the processes of the file and the
	 * selected data sources; shows the earlier result if the same run was done before.
	 */
	private void start() {
		ProcessingSession session = services.session();
		if (session.isRunning()) {
			warn("ui.processing.running");
			return;
		}
		Optional<UploadedFile> file = selectedFile();
		if (file.isEmpty()) {
			warn("ui.processing.files.selectFirst");
			return;
		}
		List<CompoundSearchService> available = services.search().search().getAvailableDatabases();
		List<CompoundSearchService> selected = databases.getSelected().stream().filter(available::contains).toList();
		if (selected.isEmpty()) {
			warn("ui.search.noDatabase.message");
			return;
		}
		ScoreSettings scoreSettings = file.get().job().getSettings().getScoreSettings();
		if (ScoreWeights.sum(ScoreDialog.percent(scoreSettings)) != ScoreWeights.TOTAL) {
			warn("ui.processing.score.fixFirst");
			editProcesses();
			return;
		}

		ProcessingSettings settings = ProcessingViewServices.copy(session.getParameters());
		settings.setScoreSettings(ProcessingViewServices.copy(scoreSettings));
		ProcessingSession.RunKey key = new ProcessingSession.RunKey(file.get().name(), ProcessingViewServices.copy(settings),
				selected.stream().map(CompoundSearchService::getDatasourceName).toList());
		Optional<Run> earlier = session.findRun(key);
		if (earlier.isPresent()) {
			show(earlier.get());
			return;
		}
		ProcessingJob job = new ProcessingJob(settings, file.get().job().getFeatureSet());
		job.setRequestedProcessUnits(services.getAvailableUnits(job));
		job.setSelectedSearchServices(new ArrayList<>(selected));
		openProgress(services.getUnitOrder(job));
		getUI().ifPresent(ui -> services.start(ui, key, job));
	}

	private void openProgress(Set<ProcessingUnit> units) {
		if (progress != null) {
			progress.close();
		}
		progress = new ProgressDialog(units);
		progress.open();
	}

	private void closeProgress() {
		if (progress != null) {
			progress.close();
			progress = null;
		}
	}

	@Override
	public void progress(ProcessingUnit unit, UnitState state) {
		if (progress != null) {
			progress.setState(unit, state);
		}
	}

	@Override
	public void finished(Run run) {
		closeProgress();
		refreshHistory();
		if (run.result().getResults().isEmpty()) {
			Notification.show(getTranslation("ui.processing.results.none"), 4000, Notification.Position.MIDDLE);
		} else {
			show(run);
		}
		new SummaryDialog(run.result().getResultSummary(), getLocale()).open();
	}

	@Override
	public void failed() {
		closeProgress();
		Notification.show(getTranslation("ui.processing.failed"), 6000, Notification.Position.MIDDLE)
				.addThemeVariants(NotificationVariant.LUMO_ERROR);
	}

	/** Shows a run of the history. */
	private void show(Run run) {
		current = run;
		results.setRun(run);
		center.setSelectedTab(resultsTab);
		refreshHistory();
	}

	/** Right side: the spectrum of the selected feature on the content tab, otherwise the candidate details. */
	private void showRight() {
		right.removeAll();
		right.add(center.getSelectedTab() == dataTab ? spectrum : details);
	}

	private void downloadEntry(Entry entry) {
		String fileName = entry.getName() == null ? "results" : entry.getName().getValue();
		new ExportDialog<>(services.search(), List.of(entry), fileName, getLocale()).open();
	}

	/** Downloads and report of the result list (the download handlers of the Vaadin 8 UI). */
	private class ResultActions implements ProcessingResultsPanel.Actions {

		@Override
		public void showSummary() {
			if (current != null) {
				new SummaryDialog(current.result().getResultSummary(), getLocale()).open();
			}
		}

		@Override
		public void downloadResults(List<ProcessCandidate> visible) {
			new ExportDialog<>(services.search(), visible, baseName(), getLocale(), new ExportDialog.Options(
					getTranslation("ui.processing.export.results"), candidateColumns(visible), null, null)).open();
		}

		@Override
		public void downloadNonResults() {
			if (current == null) {
				return;
			}
			List<Feature> nonResults = nonResults();
			if (nonResults.isEmpty()) {
				Notification.show(getTranslation("ui.processing.export.noNonResults"), 3000, Notification.Position.MIDDLE);
				return;
			}
			new ExportDialog<>(services.search(), nonResults, baseName(), getLocale(), new ExportDialog.Options(
					getTranslation("ui.processing.export.nonResults"), null, settingsSheets(), null)).open();
		}

		@Override
		public void downloadComplete(List<ProcessCandidate> visible) {
			List<ExcelSheetDefinition<?>> sheets = new ArrayList<>();
			List<Feature> nonResults = nonResults();
			if (!nonResults.isEmpty()) {
				ExcelSheetDefinition<Feature> nonResultsSheet = new ExcelSheetDefinition<>(
						getTranslation("ui.processing.export.nonResults"));
				nonResultsSheet.setColumnDirection(ExcelSheetDefinition.ColumnDirection.VERTICAL);
				nonResultsSheet.setData(nonResults);
				sheets.add(nonResultsSheet);
			}
			sheets.addAll(settingsSheets());
			Path source = current.job().getFeatureSet().getSourceFilePath();
			boolean excel = source != null && source.getFileName().toString().toLowerCase(Locale.ROOT).matches(".*\\.xlsx?");
			new ExportDialog<>(services.search(), visible, baseName(), getLocale(), new ExportDialog.Options(
					getTranslation("ui.processing.export.results"), candidateColumns(visible), sheets,
					excel && Files.exists(source) ? source : null)).open();
		}

		@Override
		public void report(List<ProcessCandidate> visible) {
			ReportDialog.forEntries(services.search(), visible.stream().map(ProcessCandidate::getEntry).toList()).open();
		}

		private String baseName() {
			String name = current == null ? "results" : current.fileName();
			int dot = name.lastIndexOf('.');
			return dot > 0 ? name.substring(0, dot) : name;
		}

		/** Features of the file without a candidate. */
		private List<Feature> nonResults() {
			Set<String> located = current.result().getResults().stream().map(ProcessCandidate::getTargetIdentifier)
					.collect(Collectors.toSet());
			return current.job().getFeatureSet().getFeatures().stream()
					.filter(feature -> !located.contains(feature.getIdentifier())).toList();
		}

		/** Sheets with the parameters and the score settings of the run. */
		private List<ExcelSheetDefinition<?>> settingsSheets() {
			ExcelSheetDefinition<ProcessingSettings> parameters = new ExcelSheetDefinition<>(
					getTranslation("ui.processing.export.parameters"));
			parameters.setColumnDirection(ExcelSheetDefinition.ColumnDirection.HORIZONTAL);
			parameters.setData(Collections.singletonList(current.job().getSettings()));
			ExcelSheetDefinition<ScoreSettings> scores = new ExcelSheetDefinition<>(
					getTranslation("ui.processing.export.scoreSettings"));
			scores.setColumnDirection(ExcelSheetDefinition.ColumnDirection.HORIZONTAL);
			scores.setData(Collections.singletonList(current.job().getSettings().getScoreSettings()));
			return List.of(parameters, scores);
		}

		/**
		 * Columns of the candidates without those of processes that did not run (e.g. no RTI screening): fields that
		 * are empty in the first candidate are left out, as before.
		 */
		private List<String> candidateColumns(List<ProcessCandidate> candidates) {
			ProcessCandidate first = candidates.get(0);
			List<String> columns = new ArrayList<>(services.search().mapper(getLocale()).getSelectorColumns(first));
			for (Field field : ProcessCandidate.class.getDeclaredFields()) {
				if (field.isAnnotationPresent(de.hswt.fi.beans.annotations.BeanColumn.class)) {
					try {
						field.setAccessible(true);
						if (field.get(first) == null) {
							columns.removeIf(column -> column.startsWith(field.getName()));
						}
					} catch (IllegalAccessException | RuntimeException e) {
						// keep the columns
					}
				}
			}
			return columns;
		}
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		ProcessingSession session = services.session();
		session.setListener(this);
		if (session.isRunning()) {
			openProgress(session.getRunningUnits());
		}
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		services.session().removeListener(this);
		closeProgress();
	}

	private void warn(String key, Object... parameters) {
		Notification.show(getTranslation(key, parameters), 4000, Notification.Position.MIDDLE)
				.addThemeVariants(NotificationVariant.LUMO_WARNING);
	}

	@Override
	public String getViewTitle() {
		return getTranslation("ui.nav.processing");
	}

	@Override
	public String getPageTitle() {
		return getViewTitle() + " | " + services.search().properties().getUi().getHeader().getCaption();
	}
}
