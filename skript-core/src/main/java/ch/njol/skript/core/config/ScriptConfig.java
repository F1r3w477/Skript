package ch.njol.skript.core.config;

import ch.njol.skript.platform.SkriptLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads a .sk file into a tree of {@link ScriptNode}s (sections and entries).
 * Indentation rules match the legacy Config: first indented line sets indent
 * (spaces or tabs only); nesting is defined by indent level. Simple mode:
 * lines ending with ":" are sections, others are single-line entries.
 * Platform-agnostic; no Bukkit or JavaPlugin dependency.
 */
public final class ScriptConfig {

    private final ScriptSectionNode root;
    private final String indentation;
    private final SkriptLogger logger;

    private ScriptConfig(ScriptSectionNode root, String indentation, SkriptLogger logger) {
        this.root = root;
        this.indentation = indentation;
        this.logger = logger;
    }

    /**
     * Loads a script file from the given path. Uses UTF-8. Logs errors via the given logger.
     *
     * @param path   path to the .sk file
     * @param logger platform logger for parse errors
     * @return config with root section, or null if the file could not be read
     */
    public static ScriptConfig load(Path path, SkriptLogger logger) {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in, path.getFileName().toString(), logger);
        } catch (IOException e) {
            logger.error("Failed to read script file " + path, e);
            return null;
        }
    }

    /**
     * Loads a script from an input stream (e.g. for tests). Uses UTF-8.
     *
     * @param in       source
     * @param fileName name used in error messages
     * @param logger   platform logger
     * @return config with root section
     */
    public static ScriptConfig load(InputStream in, String fileName, SkriptLogger logger) throws IOException {
        ScriptConfigReader r = new ScriptConfigReader(in);
        IndentState indentState = new IndentState();
        ScriptSectionNode.Builder rootBuilder = new ScriptSectionNode.Builder("", 0);
        loadSection(r, 0, rootBuilder, indentState, fileName);
        String indent = indentState.indent != null ? indentState.indent : "\t";
        return new ScriptConfig(rootBuilder.build(), indent, logger);
    }

    public ScriptSectionNode getRoot() {
        return root;
    }

    public String getIndentation() {
        return indentation;
    }

    private static final class IndentState {
        String indent;
        boolean set;
    }

    private static void loadSection(
        ScriptConfigReader r,
        int level,
        ScriptSectionNode.Builder section,
        IndentState indentState,
        String fileName
    ) throws IOException {
        AtomicBoolean inBlockComment = new AtomicBoolean(false);
        String fullLine;
        while ((fullLine = r.readLine()) != null) {
            LineSplit.Result split = LineSplit.splitLine(fullLine, inBlockComment);
            String value = split.value();

            // Set indentation from first indented line (one level below root)
            if (!indentState.set && level == 1 && !value.isEmpty() && hasLeadingWhitespace(value)) {
                String leading = value.replaceFirst("\\S.*$", "");
                if (leading.matches(" +") || leading.matches("\t+")) {
                    indentState.indent = leading;
                    indentState.set = true;
                } else {
                    section.add(new ScriptEntryNode(value.trim(), r.getLineNum()));
                    continue;
                }
            }

            String indent = indentState.indent != null ? indentState.indent : "\t";
            // Legacy: line belongs here iff blank or exactly (indent^level) + non-space + rest
            String levelIndentPattern = "^(" + regexEscape(indent) + "){" + level + "}\\S.*";
            if (!value.matches("\\s*") && !value.matches(levelIndentPattern)) {
                // Too much indent (level indents then space) -> invalid, skip line
                String tooMuchPattern = "^(" + regexEscape(indent) + "){" + level + "}\\s.*";
                if (value.matches(tooMuchPattern)) {
                    continue;
                }
                // Less indent: put line back and return to parent
                r.reset();
                return;
            }

            value = value.trim();
            if (value.isEmpty()) {
                continue;
            }

            // Section: ends with ":" (and in simple mode we don't have separator)
            if (value.endsWith(":")) {
                String key = value.substring(0, value.length() - 1);
                ScriptSectionNode.Builder childBuilder = new ScriptSectionNode.Builder(key, r.getLineNum());
                loadSection(r, level + 1, childBuilder, indentState, fileName);
                section.add(childBuilder.build());
                continue;
            }

            section.add(new ScriptEntryNode(value, r.getLineNum()));
        }
    }

    private static boolean hasLeadingWhitespace(String s) {
        return !s.isEmpty() && (s.charAt(0) == ' ' || s.charAt(0) == '\t');
    }

    private static String regexEscape(String s) {
        return Pattern.quote(s);
    }

    /**
     * Flattens a section's children into a list of body "lines" (trimmed).
     * For entries: one line = key. For sections: line = key + ":", then nested lines.
     * Used to feed the existing statement parser so behaviour is preserved.
     */
    public static List<String> flattenBody(ScriptSectionNode section) {
        List<String> out = new ArrayList<>();
        flattenBody(section, 0, "", out);
        return out;
    }

    private static final String BODY_INDENT = "    ";

    private static void flattenBody(ScriptSectionNode section, int level, String indent, List<String> out) {
        for (ScriptNode child : section.getChildren()) {
            if (child instanceof ScriptEntryNode e) {
                out.add(indent + e.getKey());
            } else if (child instanceof ScriptSectionNode s) {
                out.add(indent + s.getKey() + ":");
                flattenBody(s, level + 1, indent + BODY_INDENT, out);
            }
        }
    }
}
