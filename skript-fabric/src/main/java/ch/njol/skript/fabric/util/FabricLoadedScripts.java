package ch.njol.skript.fabric.util;

import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.core.model.ScriptEventHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Access to loaded script files for Fabric script reflection (script by name, all scripts, etc.).
 * Uses the API provided by the fabric-core copy of skript-core.
 */
public final class FabricLoadedScripts {

    private FabricLoadedScripts() {}

    /**
     * Returns the list of currently loaded script files, or empty list if not bootstrapped.
     */
    public static List<ScriptFile> getLoadedScripts() {
        List<ScriptFile> list = SkriptBootstrap.getLoadedScripts();
        return list != null ? list : Collections.emptyList();
    }

    /**
     * Find a loaded script by path or name (path can be relative to scripts dir or end with .sk).
     */
    public static ScriptFile findScript(String pathOrName) {
        if (pathOrName == null || pathOrName.isBlank()) return null;
        String normalized = pathOrName.trim().replace('\\', '/');
        if (!normalized.endsWith(".sk")) normalized = normalized + ".sk";
        String fileNameOnly = normalized.contains("/") ? normalized.substring(normalized.lastIndexOf('/') + 1) : normalized;
        List<ScriptFile> scripts = getLoadedScripts();
        for (ScriptFile script : scripts) {
            Path p = script.getPath();
            String pathStr = p.toString().replace('\\', '/');
            if (pathStr.equals(normalized) || pathStr.equals(fileNameOnly)) return script;
            if (pathStr.endsWith("/" + normalized) || pathStr.endsWith(normalized)) return script;
            String scriptFileName = p.getFileName() != null ? p.getFileName().toString() : "";
            if (scriptFileName.equalsIgnoreCase(fileNameOnly) || scriptFileName.equalsIgnoreCase(normalized))
                return script;
        }
        return null;
    }

    /**
     * Return paths of all loaded scripts as strings (for loop-value, etc.).
     */
    public static List<String> getLoadedScriptPaths() {
        List<ScriptFile> scripts = getLoadedScripts();
        List<String> out = new ArrayList<>(scripts.size());
        for (ScriptFile script : scripts) {
            Path p = script.getPath();
            out.add(p.toString().replace('\\', '/'));
        }
        return out;
    }

    /**
     * Find the script file that contains the given event handler (for "the current script").
     */
    public static ScriptFile getScriptForHandler(ScriptEventHandler handler) {
        if (handler == null) return null;
        for (ScriptFile script : getLoadedScripts()) {
            if (script.getEventHandlers().contains(handler))
                return script;
        }
        return null;
    }

    /**
     * Return paths of loaded scripts under the given folder prefix.
     */
    public static List<String> getScriptPathsInFolder(String folderPrefix) {
        if (folderPrefix == null) folderPrefix = "";
        String prefix = folderPrefix.trim().replace('\\', '/');
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix = prefix + "/";
        List<String> all = getLoadedScriptPaths();
        List<String> out = new ArrayList<>();
        for (String path : all) {
            if (path.startsWith(prefix) || path.contains("/" + prefix))
                out.add(path);
        }
        return out;
    }
}
