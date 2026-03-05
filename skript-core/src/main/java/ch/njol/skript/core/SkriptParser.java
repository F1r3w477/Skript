package ch.njol.skript.core;

import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.lang.StatementParser;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.core.patterns.CoreSkriptPattern;
import ch.njol.skript.platform.SkriptLogger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses .sk file contents into {@link ScriptFile} with {@link ScriptEventHandler}s.
 * Recognises "on &lt;event&gt;:" headers and "test \"name\":" declarations; body lines
 * are passed through to the runtime. Used internally by {@link SkriptEngine}; platforms
 * do not call this directly.
 */
final class SkriptParser {

    // Matches lines like "on join:", "On load :", etc.
    private static final Pattern EVENT_HEADER =
        Pattern.compile("^on\\s+([a-zA-Z0-9 _-]+)\\s*:\\s*$", Pattern.CASE_INSENSITIVE);

    // Matches simple test declarations like: test "My test name":
    private static final Pattern TEST_HEADER =
        Pattern.compile("^test\\s+\"(.+?)\"\\s*:?.*$", Pattern.CASE_INSENSITIVE);

    private final SkriptLogger logger;
    @SuppressWarnings("unused")
    private final CoreSkriptPattern.CoreMatchResult scratchMatchResult;

    SkriptParser(SkriptLogger logger) {
        this.logger = logger;
        // Prepare a tiny core pattern instance that we can grow over time
        // as more of the real language parser is migrated into skript-core.
        this.scratchMatchResult = new CoreSkriptPattern(
            new CoreSkriptPattern.CoreLiteralElement("on"),
            1
        ).match("on load:");
    }

    ScriptFile parse(Path path, List<String> lines) {
        List<ScriptEventHandler> handlers = new ArrayList<>();
        List<String> testNames = new ArrayList<>();

        String currentEvent = null;
        List<String> currentBody = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.replace("\t", "    "); // normalise tabs

            String trimmed = line.trim();

            // Discover test declarations regardless of indentation level.
            Matcher testMatcher = TEST_HEADER.matcher(trimmed);
            if (testMatcher.matches()) {
                String testName = testMatcher.group(1).trim();
                TestRegistry.registerTest(testName);
                testNames.add(testName);
            }

            Matcher m = EVENT_HEADER.matcher(trimmed);
            if (m.matches()) {
                // Close previous handler, if any
                if (currentEvent != null) {
                    List<Statement> statements = parseBodyToStatements(currentBody);
                    handlers.add(new ScriptEventHandler(currentEvent, statements));
                    currentBody = new ArrayList<>();
                }

                currentEvent = m.group(1).toLowerCase(Locale.ROOT).trim();
                continue;
            }

            if (currentEvent != null) {
                currentBody.add(line);
            }
        }

        // Flush final handler
        if (currentEvent != null) {
            List<Statement> statements = parseBodyToStatements(currentBody);
            handlers.add(new ScriptEventHandler(currentEvent, statements));
        }

        logger.info("Parsed " + handlers.size() + " event handler(s) from " + path.getFileName());
        return new ScriptFile(path, handlers, testNames);
    }

    private static List<Statement> parseBodyToStatements(List<String> bodyLines) {
        List<Statement> statements = new ArrayList<>();
        for (String line : bodyLines) {
            Statement st = StatementParser.parseLine(line);
            if (st != null) {
                statements.add(st);
            }
        }
        return statements;
    }
}

