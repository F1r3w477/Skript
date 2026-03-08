package ch.njol.skript.fabric.core;

import ch.njol.skript.platform.SkriptLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Prepares a scripts directory by copying .sk files from a source directory into a work
 * directory and rewriting "on command \"...\":" to "command /...:" so the core parser
 * can load them. Call this before passing the scripts path to the core (and on each
 * getScriptsDirectory() if reload should see updated content).
 */
public final class FabricScriptsDirectory {

    private FabricScriptsDirectory() {}

    /**
     * Copies all .sk files from sourceDir into workDir, preprocessing each so that
     * "on command \"name\":" becomes "command /name:". Creates workDir and any
     * needed parent directories. Returns workDir so the platform can use it as
     * getScriptsDirectory().
     *
     * @param sourceDir directory containing original .sk files
     * @param workDir   directory to write preprocessed .sk files into (e.g. a temp dir)
     * @param logger    for reporting errors
     * @return workDir
     */
    public static Path prepareScriptsDirectory(Path sourceDir, Path workDir, SkriptLogger logger) {
        if (!Files.isDirectory(sourceDir)) {
            return sourceDir;
        }
        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            logger.error("Failed to create work directory " + workDir, e);
            return sourceDir;
        }
        List<Path> scriptFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".sk"))
                .forEach(scriptFiles::add);
        } catch (IOException e) {
            logger.error("Failed to list scripts in " + sourceDir, e);
            return sourceDir;
        }
        for (Path script : scriptFiles) {
            Path relative = sourceDir.relativize(script);
            Path target = workDir.resolve(relative);
            try {
                Files.createDirectories(target.getParent());
                String content = Files.readString(script, StandardCharsets.UTF_8);
                String rewritten = FabricScriptPreprocessor.preprocess(content);
                Files.writeString(target, rewritten, StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error("Failed to preprocess script " + script + " -> " + target, e);
            }
        }
        return workDir;
    }
}
