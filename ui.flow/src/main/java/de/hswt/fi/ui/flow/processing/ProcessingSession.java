package de.hswt.fi.ui.flow.processing;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import de.hswt.fi.processing.service.model.ProcessingJob;
import de.hswt.fi.processing.service.model.ProcessingResult;
import de.hswt.fi.processing.service.model.ProcessingSettings;
import de.hswt.fi.processing.service.model.ProcessingUnit;
import de.hswt.fi.processing.service.model.ProcessingUnitState.UnitState;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Molecular screening data of the user session (SessionSharedObjects and ProcessingViewState of the Vaadin 8 UI): the
 * uploaded files with their processing jobs, the RTI calibration file, the parameters last used and the processing runs
 * of the session. Kept when the user leaves the view; the files themselves are deleted with the session (see
 * {@link SessionUploads}).
 *
 * <p>A processing job runs in the background. The view that is currently shown registers as {@link Listener} and gets
 * the progress and the result, also if the user left the view and came back in the meantime.
 */
@Component
@VaadinSessionScope
public class ProcessingSession implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Number of processing runs kept in the session (the Vaadin 8 UI kept all of them until the view was closed). */
	public static final int MAX_RUNS = 10;

	/** An uploaded file and its processing job (the score settings depend on the data in the file). */
	public record UploadedFile(String name, Path path, ProcessingJob job) implements Serializable {
	}

	/**
	 * What makes a run the same as an earlier one: the file, the settings and the data sources, taken before the run
	 * (the processing service changes the job while it runs).
	 */
	public record RunKey(String fileName, ProcessingSettings settings, List<String> databases) implements Serializable {
	}

	/** A finished processing run. */
	public record Run(RunKey key, ProcessingJob job, ProcessingResult result, LocalTime time) implements Serializable {

		public String fileName() {
			return key.fileName();
		}
	}

	/** Receives the progress and the end of a processing job; called in the UI thread (UI.access). */
	public interface Listener extends Serializable {

		void progress(ProcessingUnit unit, UnitState state);

		void finished(Run run);

		void failed();
	}

	private final List<UploadedFile> files = new ArrayList<>();

	private final LinkedList<Run> runs = new LinkedList<>();

	private UploadedFile calibrationFile;

	private ProcessingSettings parameters = new ProcessingSettings();

	private boolean running;

	private Set<ProcessingUnit> runningUnits = Set.of();

	private Listener listener;

	public List<UploadedFile> getFiles() {
		return Collections.unmodifiableList(files);
	}

	public Optional<UploadedFile> findFile(String name) {
		return files.stream().filter(file -> file.name().equals(name)).findFirst();
	}

	public void addFile(UploadedFile file) {
		files.add(file);
	}

	public void removeFile(UploadedFile file) {
		files.remove(file);
	}

	public void clearFiles() {
		files.clear();
	}

	public Optional<UploadedFile> getCalibrationFile() {
		return Optional.ofNullable(calibrationFile);
	}

	public void setCalibrationFile(UploadedFile calibrationFile) {
		this.calibrationFile = calibrationFile;
	}

	/** Parameters of the form (pH, ppm, ionisation, ...), used for all files. */
	public ProcessingSettings getParameters() {
		return parameters;
	}

	public void setParameters(ProcessingSettings parameters) {
		this.parameters = parameters;
	}

	/** The runs of the session, newest first. */
	public List<Run> getRuns() {
		return List.copyOf(runs);
	}

	/** An earlier run with the same file, parameters, processes and data sources. */
	public Optional<Run> findRun(RunKey key) {
		return runs.stream().filter(run -> run.key().equals(key)).findFirst();
	}

	public void clearRuns() {
		runs.clear();
	}

	public boolean isRunning() {
		return running;
	}

	/** The processes of the running job, in their order. */
	public Set<ProcessingUnit> getRunningUnits() {
		return runningUnits;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void removeListener(Listener listener) {
		if (this.listener == listener) {
			this.listener = null;
		}
	}

	void started(Set<ProcessingUnit> units) {
		running = true;
		runningUnits = new LinkedHashSet<>(units);
	}

	void progress(ProcessingUnit unit, UnitState state) {
		if (listener != null) {
			listener.progress(unit, state);
		}
	}

	void finished(Run run) {
		running = false;
		// Runs without results are not kept, as before
		if (!run.result().getResults().isEmpty()) {
			runs.addFirst(run);
			while (runs.size() > MAX_RUNS) {
				runs.removeLast();
			}
		}
		if (listener != null) {
			listener.finished(run);
		}
	}

	void failed() {
		running = false;
		if (listener != null) {
			listener.failed();
		}
	}
}
