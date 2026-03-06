package ch.njol.skript.core;

import ch.njol.skript.core.config.ScriptConfig;
import ch.njol.skript.core.config.ScriptEntryNode;
import ch.njol.skript.core.config.ScriptNode;
import ch.njol.skript.core.config.ScriptSectionNode;
import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.condition.CondFalse;
import ch.njol.skript.core.condition.CondTrue;
import ch.njol.skript.core.syntax.SyntaxRegistry;
import ch.njol.skript.core.types.ParseContext;
import ch.njol.skript.core.types.ParseContextHolder;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.lang.StatementParser;
import ch.njol.skript.core.lang.trigger.ConditionalTriggerItem;
import ch.njol.skript.core.lang.trigger.CoreTriggerItem;
import ch.njol.skript.core.lang.trigger.StatementTriggerItem;
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

    // Matches section keys like "on join", "On load" (without trailing colon).
    private static final Pattern EVENT_HEADER_KEY =
        Pattern.compile("^on\\s+([a-zA-Z0-9 _-]+)\\s*$", Pattern.CASE_INSENSITIVE);

    // Matches simple test declarations like: test "My test name": or test "My test name"
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

    /**
     * Parses from a config tree root (from {@link ScriptConfig#getRoot()}).
     * Iterates top-level nodes: sections with key "on &lt;event&gt;" become event handlers,
     * entries matching "test \"name\"" are registered as tests.
     */
    ScriptFile parse(Path path, ScriptSectionNode root) {
        ParseContextHolder.set(ParseContext.DEFAULT);
        try {
            return parseInner(path, root);
        } finally {
            ParseContextHolder.clear();
        }
    }

    private ScriptFile parseInner(Path path, ScriptSectionNode root) {
        List<ScriptEventHandler> handlers = new ArrayList<>();
        List<String> testNames = new ArrayList<>();

        for (ScriptNode node : root.getChildren()) {
            if (node instanceof ScriptEntryNode entry) {
                Matcher testMatcher = TEST_HEADER.matcher(entry.getKey());
                if (testMatcher.matches()) {
                    String testName = testMatcher.group(1).trim();
                    TestRegistry.registerTest(testName);
                    testNames.add(testName);
                }
                continue;
            }
            if (node instanceof ScriptSectionNode section) {
                Matcher testMatcher = TEST_HEADER.matcher(section.getKey());
                if (testMatcher.matches()) {
                    String testName = testMatcher.group(1).trim();
                    TestRegistry.registerTest(testName);
                    testNames.add(testName);
                    CoreTriggerItem chain = buildBodyChain(section);
                    handlers.add(new ScriptEventHandler("tests", chain, testName));
                    continue;
                }
                Matcher m = EVENT_HEADER_KEY.matcher(section.getKey());
                if (m.matches()) {
                    String eventName = m.group(1).toLowerCase(Locale.ROOT).trim();
                    CoreTriggerItem chain = buildBodyChain(section);
                    handlers.add(new ScriptEventHandler(eventName, chain));
                }
            }
        }

        logger.info("Parsed " + handlers.size() + " event handler(s) from " + path.getFileName());
        return new ScriptFile(path, handlers, testNames);
    }

    /**
     * Parses from a flat list of lines (e.g. for tests). Preserves legacy behaviour:
     * scans for "on &lt;event&gt;:" and collects body lines until the next event header.
     */
    ScriptFile parse(Path path, List<String> lines) {
        ParseContextHolder.set(ParseContext.DEFAULT);
        try {
            return parseInner(path, lines);
        } finally {
            ParseContextHolder.clear();
        }
    }

    private ScriptFile parseInner(Path path, List<String> lines) {
        List<ScriptEventHandler> handlers = new ArrayList<>();
        List<String> testNames = new ArrayList<>();

        String currentEvent = null;
        List<String> currentBody = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.replace("\t", "    ");
            String trimmed = line.trim();

            Matcher testMatcher = TEST_HEADER.matcher(trimmed);
            if (testMatcher.matches()) {
                String testName = testMatcher.group(1).trim();
                TestRegistry.registerTest(testName);
                testNames.add(testName);
            }

            String key = trimmed.endsWith(":") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
            Matcher m = EVENT_HEADER_KEY.matcher(key);
            if (trimmed.endsWith(":") && m.matches()) {
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
            Statement st = parseLineWithRegistry(line);
            if (st != null) {
                statements.add(st);
            }
        }
        return statements;
    }

    /** Try pattern registry first, then legacy StatementParser. */
    private static Statement parseLineWithRegistry(String line) {
        if (line == null) return null;
        String trimmed = line.stripLeading();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null;
        Statement st = SyntaxRegistry.get().parseStatement(trimmed);
        if (st != null) return st;
        st = SyntaxRegistry.get().parseEffect(trimmed);
        if (st != null) return st;
        return StatementParser.parseLine(line);
    }

    /**
     * Builds a trigger chain from a section's children (event body). Supports "if true:", "if false:", and "else:".
     */
    private static CoreTriggerItem buildBodyChain(ScriptSectionNode bodySection) {
        return buildChain(bodySection.getChildren(), null);
    }

    private static CoreTriggerItem buildChain(List<ScriptNode> nodes, CoreTriggerItem nextAfter) {
        if (nodes.isEmpty()) return nextAfter;
        ScriptNode first = nodes.get(0);
        if (first instanceof ScriptEntryNode entry) {
            Statement st = parseLineWithRegistry(entry.getKey());
            CoreTriggerItem next = buildChain(nodes.subList(1, nodes.size()), nextAfter);
            if (st != null) return new StatementTriggerItem(st, next);
            return next;
        }
        if (first instanceof ScriptSectionNode section) {
            String key = section.getKey().trim();
            String keyLower = key.toLowerCase(Locale.ROOT);
            Condition cond = null;
            if (keyLower.startsWith("if ")) {
                String condPart = key.substring(2).trim();
                cond = SyntaxRegistry.get().parseCondition(condPart);
                if (cond == null && ("true".equals(condPart) || "false".equals(condPart))) {
                    cond = "true".equals(condPart) ? CondTrue.INSTANCE : CondFalse.INSTANCE;
                }
            }
            if (cond != null) {
                List<ScriptNode> thenNodes = section.getChildren();
                List<ScriptNode> elseNodes = new ArrayList<>();
                int consumed = 1;
                if (nodes.size() > 1 && nodes.get(1) instanceof ScriptSectionNode elseSec
                    && "else".equals(elseSec.getKey().trim().toLowerCase(Locale.ROOT))) {
                    elseNodes = elseSec.getChildren();
                    consumed = 2;
                }
                CoreTriggerItem nextAfterThis = buildChain(nodes.subList(consumed, nodes.size()), nextAfter);
                CoreTriggerItem thenChain = buildChain(thenNodes, nextAfterThis);
                CoreTriggerItem elseChain = buildChain(elseNodes, nextAfterThis);
                return new ConditionalTriggerItem(cond, thenChain, elseChain);
            }
            if ("else".equals(keyLower)) {
                return buildChain(nodes.subList(1, nodes.size()), nextAfter);
            }
            Statement st = StatementParser.parseLine(key + ":");
            CoreTriggerItem next = buildChain(nodes.subList(1, nodes.size()), nextAfter);
            if (st != null) return new StatementTriggerItem(st, next);
            return next;
        }
        return nextAfter;
    }
}

