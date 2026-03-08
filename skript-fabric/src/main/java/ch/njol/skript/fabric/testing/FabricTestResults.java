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
		Path resultsPath = Paths.get(resultsProperty).toAbsolutePath().normalize();
		boolean wroteResults = false;

		try {
			Path parent = resultsPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			Set<String> succeeded = TestRegistry.getSucceededTests();
			Map<String, String> failed = TestRegistry.getFailedTests();

			// Mirror the TestResults shape used by the upstream Bukkit-based
			// test runner. For now we only distinguish succeeded vs failed
			// tests; documentation generation failures are always reported
			// as 'false' in the Fabric pipeline until doc support is added.
			boolean docsFailed = false;
			ResultsPayload results = new ResultsPayload(succeeded, failed, docsFailed);

			String json = gson.toJson(results);
			Files.writeString(resultsPath, json, StandardCharsets.UTF_8);
			logger.info("Wrote initial Skript test results to " + resultsPath);
			wroteResults = true;
		} catch (Throwable t) {
			logger.error("Failed to write Skript test results to " + resultsPath, t);
		} finally {
			// Always halt so the runner can proceed. Use halt() to avoid Fabric/Minecraft
			// shutdown hooks that can prevent System.exit(0) from terminating. Exit 0
			// so the parent only fails based on test results (it reads the JSON and
			// calls System.exit(failCount)); if we didn't write, parent will fail with
			// a different error.
			Runtime.getRuntime().halt(wroteResults ? 0 : 1);
		}
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
