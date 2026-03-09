package ch.njol.skript.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Very small, engine-local registry of Skript tests discovered while parsing
 * .sk files. This mirrors a subset of the legacy TestTracker semantics but is
 * independent from the Bukkit plugin and suitable for use on Fabric.
 */
public final class TestRegistry {

	private static final Set<String> registeredTests = new LinkedHashSet<>();
	private static final Map<String, String> failedTests = new LinkedHashMap<>();

	private TestRegistry() {
	}

	public static synchronized void registerTest(String name) {
		if (name == null || name.isBlank()) {
			return;
		}
		registeredTests.add(name);
	}

	public static synchronized void failTest(String name, String message) {
		if (name == null || name.isBlank()) {
			return;
		}
		registeredTests.add(name);
		if (message == null) {
			message = "";
		}
		failedTests.put(name, message);
	}

	public static synchronized Set<String> getSucceededTests() {
		Set<String> succeeded = new LinkedHashSet<>(registeredTests);
		succeeded.removeAll(failedTests.keySet());
		return Collections.unmodifiableSet(succeeded);
	}

	public static synchronized Map<String, String> getFailedTests() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(failedTests));
	}

	public static synchronized void clear() {
		registeredTests.clear();
		failedTests.clear();
	}
}

