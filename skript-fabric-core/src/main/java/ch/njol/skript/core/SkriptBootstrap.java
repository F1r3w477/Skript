package ch.njol.skript.core;

import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.platform.SkriptPlatform;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entry point for bootstrapping the shared Skript core on any platform.
 * <p>
 * <b>How platforms use the core</b>:
 * <ol>
 *   <li>Implement {@link ch.njol.skript.platform.SkriptPlatform} (logger, scheduler,
 *       {@link ch.njol.skript.platform.SkriptPlatform#getConfigDirectory() config}
 *       and {@link ch.njol.skript.platform.SkriptPlatform#getScriptsDirectory() scripts}
 *       directories, and optionally command registration / player listing).</li>
 *   <li>Call {@link #start(SkriptPlatform)} once after the server and config are ready.
 *       This discovers .sk files in the scripts directory, parses them, and registers
 *       event handlers.</li>
 *   <li>Trigger script logic by calling {@link #fireEvent(String, RuntimeEventContext)}
 *       with short lower-case event names (e.g. "load", "join") and an optional context.</li>
 *   <li>Reload scripts with {@link #reloadScripts()} when the user requests it (e.g. /skript reload).</li>
 * </ol>
 * Subsequent calls to {@link #start(SkriptPlatform)} are ignored. Use {@link #getPlatform()}
 * to obtain the current platform instance.
 */
public final class SkriptBootstrap {

    private static volatile SkriptPlatform platform;
    private static volatile SkriptEngine engine;

    private SkriptBootstrap() {
    }

    /**
     * Initialise the Skript core on the given platform. Safe to call once;
     * subsequent calls are ignored.
     */
    public static synchronized void start(SkriptPlatform platform) {
        Objects.requireNonNull(platform, "platform");
        if (SkriptBootstrap.platform != null) {
            return;
        }

        if (CoreTestMode.ENABLED) {
            TestRegistry.clear();
        }

        SkriptBootstrap.platform = platform;
        if (CoreTestMode.ENABLED) {
            platform.getLogger().info("Skript core bootstrap starting in TEST mode on platform '" + platform.getName() + "'. "
                + "(dir=" + CoreTestMode.TEST_DIR + ", results=" + CoreTestMode.RESULTS_FILE + ")");
        } else {
            platform.getLogger().info("Skript core bootstrap starting on platform '" + platform.getName() + "'.");
        }

        engine = new SkriptEngine(platform);
        engine.loadScripts();
    }

    /**
     * Shut down the Skript core, if it was started.
     */
    public static synchronized void shutdown() {
        if (platform != null) {
            platform.getLogger().info("Skript core shutting down.");
            platform = null;
            engine = null;
        }
    }

    /**
     * @return The platform instance the core was started with, or {@code null}
     * if the core has not been bootstrapped yet.
     */
    public static SkriptPlatform getPlatform() {
        return platform;
    }

    /**
     * Reload all scripts from the platform's scripts directory, if the core
     * has been bootstrapped.
     */
    public static synchronized void reloadScripts() {
        if (platform == null || engine == null) {
            return;
        }
        platform.getLogger().info("Reloading Skript scripts.");
        engine.loadScripts();
    }

    /**
     * Fire a named event into the core runtime. Platforms should use short,
     * lower-case event identifiers such as \"load\" or \"join\".
     */
    public static void fireEvent(String eventName, RuntimeEventContext context) {
        SkriptEngine currentEngine = engine;
        if (currentEngine == null) {
            return;
        }
        currentEngine.fireEvent(eventName, context);
    }

    /**
     * Return the list of currently loaded script files. Used by platforms (e.g. Fabric)
     * for script reflection (script by name, all scripts, current script, etc.).
     *
     * @return unmodifiable list of loaded scripts, or empty list if not bootstrapped
     */
    public static List<ScriptFile> getLoadedScripts() {
        SkriptEngine currentEngine = engine;
        return currentEngine != null ? currentEngine.getLoadedScripts() : Collections.emptyList();
    }
}

