package ch.njol.skript.fabric.testing;

import ch.njol.skript.core.TestRegistry;
import com.google.gson.Gson;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Minimal bridge for writing Skript test results from the Fabric side so that
 * the existing upstream test runner (PlatformMain/Environment) can consume
 * them without any changes.
 *
 * This initial implementation writes a "dummy but valid" TestResults-shaped
 * object when running under the test harness, allowing us to exercise the full
 * Fabric test pipeline end-to-end. Richer semantics will be added in
 * subsequent iterations.
 */
public final class FabricTestResults {

	private static final Gson gson = new Gson();

	private FabricTestResults() {
	}

	public static void maybeWriteInitialResultsAndExit(Logger logger) {
		if (!Boolean.getBoolean("skript.testing.enabled")) {
			return;
		}

		String resultsProperty = System.getProperty("skript.testing.results", "test_results.json");
		Path resultsPath = Paths.get(resultsProperty);

		try {
			Path parent = resultsPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
		} catch (IOException e) {
			logger.error("Failed to create parent directory for test results at " + resultsPath, e);
		}

		Set<String> succeeded = TestRegistry.getSucceededTests();
		Map<String, String> failed = TestRegistry.getFailedTests();

		// Mirror the TestResults shape used by the upstream Bukkit-based
		// test runner. For now we only distinguish succeeded vs failed
		// tests; documentation generation failures are always reported
		// as 'false' in the Fabric pipeline until doc support is added.
		boolean docsFailed = false;
		ResultsPayload results = new ResultsPayload(succeeded, failed, docsFailed);

		try {
			String json = gson.toJson(results);
			Files.writeString(resultsPath, json, StandardCharsets.UTF_8);
			logger.info("Wrote initial Skript test results to " + resultsPath.toAbsolutePath());
		} catch (IOException e) {
			logger.error("Failed to write Skript test results to " + resultsPath, e);
			// Let the process continue so the runner can report a more useful error.
			return;
		}

		// For now, terminate the server process immediately once results are written
		// so that the upstream runner can consume them and finish the build.
		System.exit(0);
	}

	/**
	 * Local DTO whose JSON shape matches ch.njol.skript.test.utils.TestResults
	 * in the root project. Gson on the Java-side runner will happily
	 * deserialize this into that class based on matching field names.
	 */
	private static final class ResultsPayload {

		@SuppressWarnings("unused")
		private final Set<String> succeeded;

		@SuppressWarnings("unused")
		private final Map<String, String> failed;

		@SuppressWarnings("unused")
		private final boolean docsFailed;

		private ResultsPayload(Set<String> succeeded, Map<String, String> failed, boolean docsFailed) {
			this.succeeded = succeeded;
			this.failed = failed;
			this.docsFailed = docsFailed;
		}
	}
}


