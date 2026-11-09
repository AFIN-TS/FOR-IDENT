package de.hswt.fi.ui.flow.processing;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import de.hswt.fi.fileimport.service.api.FeatureContentImporter;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.processing.service.api.ProcessingService;
import de.hswt.fi.processing.service.model.ProcessingJob;
import de.hswt.fi.processing.service.model.ProcessingResult;
import de.hswt.fi.processing.service.model.ProcessingUnit;
import de.hswt.fi.processing.service.model.ProcessingUnitState;
import de.hswt.fi.ui.flow.search.SearchViewServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Services of the molecular screening view (the processing handlers of the Vaadin 8 UI): import of uploaded files,
 * creation and background execution of processing jobs. The search services (data sources, export, report) are shared
 * with the search views.
 */
@Component
public class ProcessingViewServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingViewServices.class);

	/** Maximum number of features in a file (as before). */
	public static final int MAX_FEATURES = 10000;

	private final FeatureContentImporter importer;

	private final ObjectProvider<ProcessingService> processingServices;

	private final ObjectProvider<ProcessingSession> session;

	private final SessionUploads uploads;

	private final SearchViewServices search;

	public ProcessingViewServices(FeatureContentImporter importer, ObjectProvider<ProcessingService> processingServices,
								  ObjectProvider<ProcessingSession> session, SessionUploads uploads,
								  SearchViewServices search) {
		this.importer = importer;
		this.processingServices = processingServices;
		this.session = session;
		this.uploads = uploads;
		this.search = search;
	}

	/** Search services: data sources, export, report, details. */
	public SearchViewServices search() {
		return search;
	}

	/** Molecular screening data of the current session. */
	public ProcessingSession session() {
		return session.getObject();
	}

	/** Directory of the current session for uploaded files of the given kind; deleted with the session. */
	public Path uploadDirectory(String kind) {
		if (!(VaadinSession.getCurrent().getSession() instanceof WrappedHttpSession wrapped)) {
			throw new IllegalStateException("no HTTP session");
		}
		return uploads.directory(wrapped.getHttpSession(), kind);
	}

	/** Reads the features of a file, with the RTI calibration data of a separate file if given. */
	public Optional<FeatureSet> importFile(Path file, Optional<Path> calibrationFile) {
		try {
			return calibrationFile.isPresent()
					? importer.importFromFileWithCalibrationData(file, calibrationFile.get())
					: importer.importFromFile(file);
		} catch (RuntimeException e) {
			LOGGER.warn("cannot import {}", file.getFileName(), e);
			return Optional.empty();
		}
	}

	/** New job for the features of a file, with the processes for which data is available and equal score weights. */
	public ProcessingJob createJob(FeatureSet featureSet) {
		return processingServices.getObject().getProcessingJob(featureSet);
	}

	public List<ProcessingUnit> getAvailableUnits(ProcessingJob job) {
		return processingServices.getObject().getAvailableProcessingUnits(job);
	}

	/** The processes the job will run, in their order. */
	public Set<ProcessingUnit> getUnitOrder(ProcessingJob job) {
		return processingServices.getObject().getProcessingUnitOrder(job.getSettings());
	}

	/**
	 * Runs the job in the background; progress and result go to the listener of the session, in the UI thread. Only
	 * one job runs per session at a time.
	 */
	public void start(UI ui, ProcessingSession.RunKey key, ProcessingJob job) {
		ProcessingSession processingSession = session();
		VaadinSession vaadinSession = ui.getSession();
		ProcessingService service = processingServices.getObject();
		processingSession.started(service.getProcessingUnitOrder(job.getSettings()));

		// Since Java 9 the common pool threads use the system class loader, which does not see the libraries inside
		// the Spring Boot JAR (e.g. the native InChI library used by MetFrag), see 2.0.1
		ClassLoader applicationClassLoader = Thread.currentThread().getContextClassLoader();
		CompletableFuture.supplyAsync(() -> {
			Thread thread = Thread.currentThread();
			ClassLoader previousClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader(applicationClassLoader);
			try {
				return service.executeJob(job, state -> progress(ui, vaadinSession, processingSession, state));
			} catch (Exception | Error e) {
				// Errors too (e.g. no more native threads), otherwise the progress dialog stays open forever
				LOGGER.error("error in processing", e);
				return null;
			} finally {
				thread.setContextClassLoader(previousClassLoader);
			}
		}).thenAccept(result -> access(ui, vaadinSession, () -> finish(processingSession, key, job, result)));
	}

	private static void progress(UI ui, VaadinSession vaadinSession, ProcessingSession processingSession,
								 ProcessingUnitState state) {
		// The service changes the state object later on, so the values are taken now
		ProcessingUnit unit = state.getProcessUnit();
		ProcessingUnitState.UnitState unitState = state.getUnitState();
		access(ui, vaadinSession, () -> processingSession.progress(unit, unitState));
	}

	private static void finish(ProcessingSession processingSession, ProcessingSession.RunKey key, ProcessingJob job,
							   ProcessingResult result) {
		if (result == null) {
			processingSession.failed();
		} else {
			processingSession.finished(new ProcessingSession.Run(key, job, result, LocalTime.now()));
		}
	}

	/** Deep copy of settings, so later changes of the form do not change the settings of a run. */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T copy(T object) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
				out.writeObject(object);
			}
			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
				return (T) in.readObject();
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new IllegalStateException("cannot copy " + object, e);
		}
	}

	/** Runs the command in the UI; if the browser window was closed in the meantime, at least in the session. */
	private static void access(UI ui, VaadinSession vaadinSession, Command command) {
		try {
			ui.access(command);
		} catch (UIDetachedException e) {
			vaadinSession.access(command);
		}
	}
}
