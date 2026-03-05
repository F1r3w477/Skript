package ch.njol.skript.core;

import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.platform.SkriptPlatform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core engine: loads scripts from the platform's scripts directory and dispatches events.
 * <p>
 * Created by {@link SkriptBootstrap#start(ch.njol.skript.platform.SkriptPlatform)}. Uses
 * {@link SkriptPlatform#getScriptsDirectory()} to discover .sk files, parses them via
 * {@link SkriptParser}, and registers handlers in {@link SkriptRuntime}. Events fired via
 * {@link SkriptBootstrap#fireEvent(String, RuntimeEventContext)} are dispatched to matching
 * handlers by this engine. Platform code does not instantiate this class directly.
 */
final class SkriptEngine {

    private final SkriptPlatform platform;
    private final SkriptLogger logger;
    private final SkriptParser parser;
    private final SkriptRuntime runtime;

    private final List<ScriptFile> loadedScripts = new ArrayList<>();

    SkriptEngine(SkriptPlatform platform) {
        this.platform = platform;
        this.logger = platform.getLogger();
        this.parser = new SkriptParser(logger);
        this.runtime = new SkriptRuntime(logger);
    }

    void loadScripts() {
        Path scriptsDir = platform.getScriptsDirectory();
        logger.info("Looking for scripts in: " + scriptsDir);

        if (!Files.isDirectory(scriptsDir)) {
            logger.warn("Scripts directory does not exist yet: " + scriptsDir);
            return;
        }

        List<Path> scriptFiles = new ArrayList<>();
        try {
            try (var stream = Files.walk(scriptsDir)) {
                stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".sk"))
                    .forEach(scriptFiles::add);
            }
        } catch (IOException e) {
            logger.error("Failed to list scripts in " + scriptsDir, e);
            return;
        }

        loadedScripts.clear();

        if (scriptFiles.isEmpty()) {
            logger.info("No .sk scripts found.");
            return;
        }

        logger.info("Found " + scriptFiles.size() + " script file(s).");
        for (Path script : scriptFiles) {
            try {
                List<String> lines = Files.readAllLines(script, StandardCharsets.UTF_8);
                ScriptFile parsed = parser.parse(script, lines);
                loadedScripts.add(parsed);

                logger.info("Loaded script: " + scriptsDir.relativize(script) +
                    " (" + parsed.getEventHandlers().size() + " event handler(s))");
            } catch (IOException e) {
                logger.error("Failed to read script file " + script, e);
            }
        }

        runtime.registerScripts(loadedScripts);
    }

    List<ScriptFile> getLoadedScripts() {
        return Collections.unmodifiableList(loadedScripts);
    }

    void fireEvent(String eventName, RuntimeEventContext context) {
        runtime.fireEvent(eventName, context);
    }
}

