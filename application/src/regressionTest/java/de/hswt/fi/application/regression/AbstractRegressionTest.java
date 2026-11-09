package de.hswt.fi.application.regression;

import de.hswt.fi.application.Application;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class of the regression tests. Starts the complete application with the production profile
 * ("lc") against the local databases and compares results with golden files.
 *
 * <p>System properties (set by the Gradle task "regressionTest"):
 * <ul>
 * <li>regression.rootDir - root of the repository, input files are resolved against it</li>
 * <li>regression.goldenDir - directory of the golden files</li>
 * <li>regression.actualDir - mismatching results are written here for comparison</li>
 * <li>regression.update - "true" rewrites the golden files instead of comparing</li>
 * </ul>
 */
// Vaadin registers servlet listeners, which the mock servlet context does not support
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"lc"})
public abstract class AbstractRegressionTest {

	protected static final Path ROOT_DIR = Paths.get(System.getProperty("regression.rootDir", "."));

	private static final Path GOLDEN_DIR = Paths.get(System.getProperty("regression.goldenDir", "golden"));

	private static final Path ACTUAL_DIR = Paths.get(System.getProperty("regression.actualDir", "actual"));

	private static final boolean UPDATE = Boolean.parseBoolean(System.getProperty("regression.update", "false"));

	// Expected number of substances per database; edit it deliberately when the data is updated
	private static final String DATABASE_STATE = "database/substance-counts.txt";

	private static boolean databaseStateChecked;

	@Autowired
	private List<CompoundSearchService> compoundSearchServices;

	private List<String> mismatches;

	@BeforeEach
	public void initMismatches() {
		mismatches = new ArrayList<>();
	}

	/**
	 * The golden results are only valid for the reference data (the compound
	 * databases the golden files were created with). Stop before comparing anything if a database was changed, e.g. by a test that
	 * wrote test data into it.
	 */
	@BeforeEach
	public void checkDatabaseState() throws IOException {
		if (databaseStateChecked) {
			return;
		}
		StringBuilder state = new StringBuilder();
		compoundSearchServices.stream()
				.sorted(Comparator.comparing(CompoundSearchService::getDatasourceName))
				.forEach(service -> state.append(service.getDatasourceName()).append(' ')
						.append(service.findAllPublicIDs(PageRequest.of(0, 1)).getTotalElements()).append('\n'));
		Path golden = GOLDEN_DIR.resolve(DATABASE_STATE);
		if (!Files.exists(golden)) {
			Files.createDirectories(golden.getParent());
			Files.write(golden, state.toString().getBytes(StandardCharsets.UTF_8));
		}
		String expected = new String(Files.readAllBytes(golden), StandardCharsets.UTF_8).replace("\r\n", "\n");
		assertEquals(expected, state.toString(), "The compound databases differ from the reference data, restore them from the dumps"
				+ " (dev/setup-local-db.ps1) before running the regression tests");
		databaseStateChecked = true;
	}

	/**
	 * Compares the content with the golden file (or rewrites it in update mode). Mismatches are
	 * collected, so one run reports all differing files.
	 */
	protected void checkGolden(String name, String result) throws IOException {
		// Jackson's pretty printer uses the platform line separator
		String content = result.replace("\r\n", "\n");
		Path golden = GOLDEN_DIR.resolve(name);
		if (UPDATE) {
			Files.createDirectories(golden.getParent());
			Files.write(golden, content.getBytes(StandardCharsets.UTF_8));
			return;
		}
		if (!Files.exists(golden)) {
			mismatches.add(name + ": golden file missing");
			writeActual(name, content);
			return;
		}
		String expected = new String(Files.readAllBytes(golden), StandardCharsets.UTF_8).replace("\r\n", "\n");
		if (!expected.equals(content)) {
			mismatches.add(name + ": " + firstDifference(expected, content));
			writeActual(name, content);
		}
	}

	/** Compares two results directly, e.g. local against production, without a golden file. */
	protected void checkEqual(String name, String expectedResult, String actualResult) throws IOException {
		String expected = expectedResult.replace("\r\n", "\n");
		String actual = actualResult.replace("\r\n", "\n");
		if (!expected.equals(actual)) {
			mismatches.add(name + ": " + firstDifference(expected, actual));
			writeActual(name, actual);
		}
	}

	@AfterEach
	public void reportMismatches() {
		if (!mismatches.isEmpty()) {
			fail(mismatches.size() + " result(s) differ from the golden files (actual results in "
					+ ACTUAL_DIR + "):\n  " + String.join("\n  ", mismatches));
		}
	}

	private void writeActual(String name, String content) throws IOException {
		Path actual = ACTUAL_DIR.resolve(name);
		Files.createDirectories(actual.getParent());
		Files.write(actual, content.getBytes(StandardCharsets.UTF_8));
	}

	private static String firstDifference(String expected, String actual) {
		String[] expectedLines = expected.split("\n", -1);
		String[] actualLines = actual.split("\n", -1);
		int lines = Math.min(expectedLines.length, actualLines.length);
		for (int i = 0; i < lines; i++) {
			if (!expectedLines[i].equals(actualLines[i])) {
				return "line " + (i + 1) + ": expected [" + expectedLines[i].trim() + "] but was ["
						+ actualLines[i].trim() + "]";
			}
		}
		return "expected " + expectedLines.length + " lines but was " + actualLines.length;
	}
}
