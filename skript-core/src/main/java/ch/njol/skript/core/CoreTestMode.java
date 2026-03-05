package ch.njol.skript.core;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lightweight, platform-agnostic analogue of the legacy TestMode.
 * Reads {@code skript.testing.*} system properties so the shared core
 * can adapt its behaviour when running under the upstream test harness.
 */
public final class CoreTestMode {

	public static final String ROOT = "skript.testing.";

	public static final boolean ENABLED = "true".equalsIgnoreCase(System.getProperty(ROOT + "enabled"));
	public static final Path TEST_DIR = ENABLED && System.getProperty(ROOT + "dir") != null
		? Paths.get(System.getProperty(ROOT + "dir"))
		: null;
	public static final Path RESULTS_FILE = ENABLED && System.getProperty(ROOT + "results") != null
		? Paths.get(System.getProperty(ROOT + "results"))
		: null;
	public static final boolean DEV_MODE = ENABLED && "true".equalsIgnoreCase(System.getProperty(ROOT + "devMode"));
	public static final boolean GEN_DOCS = "true".equalsIgnoreCase(System.getProperty(ROOT + "genDocs"));
	public static final String VERBOSITY = ENABLED ? System.getProperty(ROOT + "verbosity") : null;
	public static final boolean JUNIT = "true".equalsIgnoreCase(System.getProperty(ROOT + "junit"));

	private CoreTestMode() {
	}
}
