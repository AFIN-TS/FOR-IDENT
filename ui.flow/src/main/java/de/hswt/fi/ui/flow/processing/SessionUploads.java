package de.hswt.fi.ui.flow.processing;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Directories of the uploaded files (SessionTempDirectories and CustomHttpSessionListener of the Vaadin 8 UI).
 *
 * <p>Uploaded files are not stored permanently: each session has its own directory, which is deleted when the session
 * ends (logout, or the session timeout after the browser window was closed; the Vaadin heartbeat keeps the session of an
 * open window alive). All session directories live below one root, which only the service user may read and which is
 * emptied on startup, so files of sessions that did not end properly (server crash, hard restart) are removed as well.
 *
 * <p>The directory of a session has a random name, stored as session attribute. It does not depend on the session ID,
 * which changes on login (protection against session fixation); Spring Security copies the attribute into the new
 * session.
 *
 * <p>Spring Boot registers this bean as listener of the servlet context.
 */
@Component
public class SessionUploads implements HttpSessionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionUploads.class);

	private static final String SESSION_ATTRIBUTE = SessionUploads.class.getName();

	private final Path root;

	public SessionUploads(@Value("${de.hswt.fi.session-temp-directory}") String root) {
		this.root = Paths.get(root);
	}

	/** Removes leftovers of a previous run; no session exists yet at startup. */
	@PostConstruct
	public void deleteLeftovers() throws IOException {
		Files.createDirectories(root);
		// Uploaded files are user data: only the user running the application may read them
		if (root.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
		}
		try (Stream<Path> leftovers = Files.list(root)) {
			for (Path leftover : (Iterable<Path>) leftovers::iterator) {
				LOGGER.info("deleting temporary session directory of a previous run: {}", leftover);
				delete(leftover);
			}
		}
	}

	/** Directory of the session for files of the given kind (e.g. "processing"), created on the first call. */
	public Path directory(HttpSession session, String kind) {
		Object directory = session.getAttribute(SESSION_ATTRIBUTE);
		if (directory == null) {
			directory = root.resolve(UUID.randomUUID().toString()).toString();
			session.setAttribute(SESSION_ATTRIBUTE, directory);
		}
		Path path = Paths.get(directory.toString()).resolve(kind);
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return path;
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		Object directory = event.getSession().getAttribute(SESSION_ATTRIBUTE);
		if (directory == null) {
			return;
		}
		Path path = Paths.get(directory.toString());
		try {
			if (Files.exists(path)) {
				LOGGER.info("deleting temporary session directory: {}", path);
				delete(path);
			}
		} catch (IOException e) {
			LOGGER.error("cannot delete temporary session directory {}", path, e);
		}
	}

	public Path getRoot() {
		return root;
	}

	private static void delete(Path path) throws IOException {
		try (Stream<Path> files = Files.walk(path)) {
			for (Path file : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) {
				Files.delete(file);
			}
		}
	}
}
