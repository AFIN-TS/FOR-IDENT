package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.processing.service.model.ProcessingJob;
import de.hswt.fi.ui.flow.processing.ProcessingSession;
import de.hswt.fi.ui.flow.processing.ProcessingSession.UploadedFile;
import de.hswt.fi.ui.flow.processing.ProcessingViewServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Upload of the files to screen (FileUploadWindow and ProcessingImportHandler of the Vaadin 8 UI): up to 15 files and
 * optionally one RTI calibration file, which is used for all files without RTI calibration data. The files are stored in
 * the directory of the session and read on OK; a file that was read before keeps its process settings unless the
 * calibration file changed.
 */
public class UploadDialog extends Dialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadDialog.class);

	static final int MAX_FILES = 15;

	/** Maximum file size in bytes (as before). */
	static final int MAX_FILE_SIZE = 15_360_000;

	static final String PROCESSING = "processing";

	static final String CALIBRATION = "calibration";

	private static final String PARTIAL_PREFIX = ".upload-";

	private final transient ProcessingViewServices services;

	private final Runnable onChange;

	private final Path fileDirectory;

	private final Path calibrationDirectory;

	private final VerticalLayout fileList = new VerticalLayout();

	private final VerticalLayout calibrationList = new VerticalLayout();

	private final Upload fileUpload = new Upload();

	private final Upload calibrationUpload = new Upload();

	public UploadDialog(ProcessingViewServices services, Runnable onChange) {
		this.services = services;
		this.onChange = onChange;
		this.fileDirectory = services.uploadDirectory(PROCESSING);
		this.calibrationDirectory = services.uploadDirectory(CALIBRATION);
		setHeaderTitle(getTranslation("ui.processing.upload.title"));
		setWidth("min(36rem, 95vw)");

		configure(fileUpload, fileDirectory, MAX_FILES, "ui.processing.upload.addFiles");
		configure(calibrationUpload, calibrationDirectory, 1, "ui.processing.upload.addCalibration");
		for (VerticalLayout list : List.of(fileList, calibrationList)) {
			list.setPadding(false);
			list.setSpacing(false);
			list.addClassName("fi-file-list");
		}

		Span filesCaption = new Span(getTranslation("ui.processing.upload.files", MAX_FILES));
		filesCaption.addClassName("fi-field-caption");
		Paragraph calibrationHint = new Paragraph(getTranslation("ui.processing.upload.calibrationHint"));
		calibrationHint.addClassName("fi-hint");
		add(filesCaption, fileList, fileUpload, calibrationHint, calibrationList, calibrationUpload);

		Button cancel = new Button(getTranslation("ui.button.cancel"), e -> close());
		Button ok = new Button(getTranslation("ui.button.ok"), e -> apply());
		ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		getFooter().add(cancel, ok);
		refreshLists();
	}

	private void configure(Upload upload, Path directory, int maxFiles, String buttonKey) {
		upload.setUploadHandler(UploadHandler.toFile((metadata, file) -> stored(upload, directory, metadata, file),
				metadata -> directory.resolve(PARTIAL_PREFIX + UUID.randomUUID()).toFile()));
		upload.setMaxFiles(maxFiles);
		upload.setMaxFileSize(MAX_FILE_SIZE);
		upload.setWidthFull();
		UploadI18N i18n = new UploadI18N();
		i18n.setAddFiles(new UploadI18N.AddFiles().setOne(getTranslation(buttonKey)).setMany(getTranslation(buttonKey)));
		i18n.setDropFiles(new UploadI18N.DropFiles().setOne(getTranslation("ui.processing.upload.drop"))
				.setMany(getTranslation("ui.processing.upload.drop")));
		i18n.setError(new UploadI18N.Error()
				.setTooManyFiles(getTranslation("ui.processing.upload.tooManyFiles", maxFiles))
				.setFileIsTooBig(getTranslation("ui.processing.upload.tooBig", MAX_FILE_SIZE / 1_024_000))
				.setIncorrectFileType(getTranslation("ui.processing.upload.failed")));
		upload.setI18n(i18n);
		upload.addFileRejectedListener(e -> error(e.getErrorMessage()));
	}

	/** Called in the UI thread when a file was received: keeps it under its name unless that name exists already. */
	private void stored(Upload upload, Path directory, UploadMetadata metadata, File file) throws IOException {
		upload.clearFileList();
		String name = safeFileName(metadata.fileName());
		Path target = directory.resolve(name);
		if (Files.exists(target)) {
			Files.deleteIfExists(file.toPath());
			error(getTranslation("ui.processing.upload.exists", name));
		} else if (directory.equals(calibrationDirectory) && !files(calibrationDirectory).isEmpty()) {
			Files.deleteIfExists(file.toPath());
			error(getTranslation("ui.processing.upload.oneCalibration"));
		} else if (directory.equals(fileDirectory) && files(fileDirectory).size() >= MAX_FILES) {
			Files.deleteIfExists(file.toPath());
			error(getTranslation("ui.processing.upload.tooManyFiles", MAX_FILES));
		} else {
			Files.move(file.toPath(), target, StandardCopyOption.ATOMIC_MOVE);
		}
		refreshLists();
	}

	/** File name without directories and without characters that are not allowed in file names. */
	static String safeFileName(String name) {
		String fileName = name == null ? "" : name.replace('\\', '/').replaceAll(".*/", "");
		fileName = fileName.replaceAll("[:*?\"<>|\\p{Cntrl}]", "_").trim();
		if (fileName.isEmpty() || fileName.startsWith(".")) {
			fileName = "file" + fileName;
		}
		return fileName;
	}

	private void refreshLists() {
		fileList.removeAll();
		files(fileDirectory).forEach(file -> fileList.add(fileRow(file)));
		if (fileList.getComponentCount() == 0) {
			fileList.add(new Span(getTranslation("ui.processing.upload.noFiles")));
		}
		calibrationList.removeAll();
		files(calibrationDirectory).forEach(file -> calibrationList.add(fileRow(file)));
		calibrationUpload.setVisible(calibrationList.getComponentCount() == 0);
		fileUpload.setVisible(files(fileDirectory).size() < MAX_FILES);
	}

	private Component fileRow(Path file) {
		Span name = new Span(VaadinIcon.FILE_O.create(), new Span(" " + file.getFileName()));
		Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
			try {
				Files.deleteIfExists(file);
			} catch (IOException ex) {
				LOGGER.error("cannot delete {}", file, ex);
				error(getTranslation("ui.processing.upload.failed"));
			}
			refreshLists();
		});
		delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
		delete.setTooltipText(getTranslation("ui.processing.upload.remove"));
		delete.setAriaLabel(getTranslation("ui.processing.upload.remove"));
		HorizontalLayout row = new HorizontalLayout(name, delete);
		row.setWidthFull();
		row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		row.expand(name);
		return row;
	}

	/** The stored files of a directory in alphabetical order, without unfinished uploads. */
	static List<Path> files(Path directory) {
		try (Stream<Path> files = Files.list(directory)) {
			return files.filter(Files::isRegularFile)
					.filter(file -> !file.getFileName().toString().startsWith(PARTIAL_PREFIX))
					.sorted()
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Reads the files and replaces the files of the session. Files that cannot be read, are empty or contain too many
	 * features are reported and deleted.
	 */
	private void apply() {
		ProcessingSession session = services.session();
		Optional<Path> calibration = files(calibrationDirectory).stream().findFirst();
		Optional<Path> previousCalibration = session.getCalibrationFile().map(UploadedFile::path);
		boolean calibrationChanged = !Objects.equals(calibration, previousCalibration);
		if (calibrationChanged) {
			session.setCalibrationFile(calibration
					.map(path -> new UploadedFile(path.getFileName().toString(), path, null)).orElse(null));
		}

		List<UploadedFile> imported = new ArrayList<>();
		for (Path file : files(fileDirectory)) {
			String name = file.getFileName().toString();
			Optional<UploadedFile> existing = session.findFile(name);
			if (existing.isPresent() && !calibrationChanged) {
				imported.add(existing.get());
				continue;
			}
			Optional<FeatureSet> featureSet = services.importFile(file, calibration);
			Optional<String> problem = check(name, featureSet);
			if (problem.isPresent()) {
				error(problem.get());
				delete(file);
			} else {
				ProcessingJob job = services.createJob(featureSet.get());
				imported.add(new UploadedFile(name, file, job));
			}
		}
		session.clearFiles();
		imported.forEach(session::addFile);
		onChange.run();
		close();
	}

	private Optional<String> check(String name, Optional<FeatureSet> featureSet) {
		if (featureSet.isEmpty() || featureSet.get().getFeatures() == null || featureSet.get().getFeatures().isEmpty()) {
			return Optional.of(getTranslation("ui.processing.upload.invalid", name));
		}
		int count = featureSet.get().getFeatures().size();
		if (count > ProcessingViewServices.MAX_FEATURES) {
			return Optional.of(getTranslation("ui.processing.upload.tooManyFeatures", name,
					ProcessingViewServices.MAX_FEATURES, count));
		}
		return Optional.empty();
	}

	private static void delete(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			LOGGER.error("cannot delete {}", file, e);
		}
	}

	private static void error(String message) {
		Notification.show(message, 5000, Notification.Position.MIDDLE)
				.addThemeVariants(NotificationVariant.LUMO_ERROR);
	}
}
