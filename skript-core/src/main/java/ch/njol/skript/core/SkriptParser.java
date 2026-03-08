package ch.njol.skript.core;

import ch.njol.skript.core.config.ScriptConfig;
import ch.njol.skript.core.config.ScriptEntryNode;
import ch.njol.skript.core.config.ScriptNode;
import ch.njol.skript.core.config.ScriptSectionNode;
import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.condition.CondFalse;
import ch.njol.skript.core.condition.CondTrue;
import ch.njol.skript.core.conditions.CondCompound;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.trigger.ConditionalTriggerItem;
import ch.njol.skript.core.lang.trigger.CoreTriggerItem;
import ch.njol.skript.core.lang.trigger.LoopNTimesTriggerItem;
import ch.njol.skript.core.lang.trigger.LoopOverListTriggerItem;
import ch.njol.skript.core.lang.trigger.ParseSectionTriggerItem;
import ch.njol.skript.core.lang.trigger.StatementTriggerItem;
import ch.njol.skript.core.lang.trigger.WhileTriggerItem;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.core.patterns.CoreSkriptPattern;
import ch.njol.skript.core.syntax.SyntaxRegistry;
import ch.njol.skript.core.variables.VariableScope;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.platform.SkriptPlatform;
import ch.njol.skript.core.types.CoreTypes;
import ch.njol.skript.core.types.ParseContext;
import ch.njol.skript.core.types.ParseContextHolder;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.lang.StatementParser;

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

    // Matches command section: command /name or command /name:
    private static final Pattern COMMAND_HEADER =
        Pattern.compile("^command\\s+/(\\S+)\\s*$", Pattern.CASE_INSENSITIVE);

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
                String key = section.getKey().trim();
                Matcher testMatcher = TEST_HEADER.matcher(key);
                if (testMatcher.matches()) {
                    String testName = testMatcher.group(1).trim();
                    TestRegistry.registerTest(testName);
                    testNames.add(testName);
                    CoreTriggerItem chain = buildBodyChain(section);
                    handlers.add(new ScriptEventHandler("tests", chain, testName));
                    continue;
                }
                Matcher cmdMatcher = COMMAND_HEADER.matcher(key);
                if (cmdMatcher.matches()) {
                    String cmdName = cmdMatcher.group(1).toLowerCase(Locale.ROOT).trim();
                    CoreTriggerItem chain = buildBodyChain(section);
                    handlers.add(new ScriptEventHandler("command:" + cmdName, chain));
                    continue;
                }
                Matcher m = EVENT_HEADER_KEY.matcher(key);
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

            String key = trimmed.endsWith(":") ? trimmed.substring(0, trimmed.length() - 1).trim() : trimmed.trim();
            Matcher cmdMatcher = COMMAND_HEADER.matcher(key);
            if (trimmed.endsWith(":") && cmdMatcher.matches()) {
                if (currentEvent != null) {
                    List<Statement> statements = parseBodyToStatements(currentBody);
                    handlers.add(new ScriptEventHandler(currentEvent, statements));
                    currentBody = new ArrayList<>();
                }
                currentEvent = "command:" + cmdMatcher.group(1).toLowerCase(Locale.ROOT).trim();
                continue;
            }
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
     * Builds a trigger chain from a section's children (event body). Supports "if true:", "if false:", "else:",
     * multiline "if"/"if any" with "then", "else if", "parse if", etc.
     */
    private static CoreTriggerItem buildBodyChain(ScriptSectionNode bodySection) {
        return buildChain(bodySection.getChildren(), null);
    }

    /** Evaluate condition at parse time for parse-if (skip body when false). Returns false on error or when platform null. */
    private static boolean evaluateConditionAtParseTime(Condition cond) {
        SkriptPlatform platform = SkriptBootstrap.getPlatform();
        if (platform == null || cond == null) return false;
        try {
            ExecutionContext ctx = new ExecutionContext(
                platform.getLogger(),
                new RuntimeEventContext("parse", "parse"),
                new VariableScope(),
                null,
                null
            );
            return cond.check(ctx);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Build compound condition from multiline if/if any section (each child entry = one condition line). */
    private static Condition parseMultilineConditions(ScriptSectionNode section, boolean ifAny) {
        List<Condition> list = new ArrayList<>();
        for (ScriptNode child : section.getChildren()) {
            if (child instanceof ScriptEntryNode entry) {
                String line = entry.getKey().trim();
                if (line.isEmpty()) continue;
                Condition c = SyntaxRegistry.get().parseCondition(line);
                if (c == null && ("true".equals(line) || "false".equals(line))) {
                    c = "true".equals(line) ? CondTrue.INSTANCE : CondFalse.INSTANCE;
                }
                if (c != null) list.add(c);
            }
        }
        if (list.isEmpty()) return CondFalse.INSTANCE;
        if (list.size() == 1) return list.get(0);
        return new CondCompound(list, ifAny ? CondCompound.Operator.OR : CondCompound.Operator.AND);
    }

    private static final String THEN_KEY = "then";
    private static final String THEN_RUN_KEY = "then run";

    private static boolean isThenSection(ScriptNode node) {
        if (!(node instanceof ScriptSectionNode s)) return false;
        String k = s.getKey().trim().toLowerCase(Locale.ROOT);
        return THEN_KEY.equals(k) || k.startsWith(THEN_RUN_KEY);
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

            // else: run else body then continue
            if ("else".equals(keyLower)) {
                CoreTriggerItem afterElse = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                return buildChain(section.getChildren(), afterElse);
            }

            // Multiline "if" or "if any" with "then"
            if (("if".equals(keyLower) || "if any".equals(keyLower)) && nodes.size() >= 2 && isThenSection(nodes.get(1))) {
                boolean ifAny = "if any".equals(keyLower);
                Condition compound = parseMultilineConditions(section, ifAny);
                ScriptSectionNode thenSection = (ScriptSectionNode) nodes.get(1);
                CoreTriggerItem elseChain = buildChain(nodes.subList(2, nodes.size()), nextAfter);
                CoreTriggerItem thenChain = buildChain(thenSection.getChildren(), nextAfter);
                return new ConditionalTriggerItem(compound, thenChain, elseChain);
            }

            // Multiline "else if" or "else if any" with "then"
            if ((keyLower.equals("else if") || keyLower.equals("else if any")) && nodes.size() >= 2 && isThenSection(nodes.get(1))) {
                boolean ifAny = keyLower.equals("else if any");
                Condition compound = parseMultilineConditions(section, ifAny);
                ScriptSectionNode thenSection = (ScriptSectionNode) nodes.get(1);
                CoreTriggerItem elseChain = buildChain(nodes.subList(2, nodes.size()), nextAfter);
                CoreTriggerItem thenChain = buildChain(thenSection.getChildren(), nextAfter);
                return new ConditionalTriggerItem(compound, thenChain, elseChain);
            }

            // else parse if <condition>:
            if (keyLower.startsWith("else parse if ")) {
                String condPart = key.substring("else parse if ".length()).trim();
                Condition parseIfCond = SyntaxRegistry.get().parseCondition(condPart);
                if (parseIfCond == null && ("true".equals(condPart) || "false".equals(condPart))) {
                    parseIfCond = "true".equals(condPart) ? CondTrue.INSTANCE : CondFalse.INSTANCE;
                }
                CoreTriggerItem elseChain = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                CoreTriggerItem bodyChain;
                if (parseIfCond != null && !evaluateConditionAtParseTime(parseIfCond)) {
                    bodyChain = elseChain;
                } else {
                    bodyChain = buildChain(section.getChildren(), nextAfter);
                }
                if (parseIfCond != null) {
                    return new ConditionalTriggerItem(parseIfCond, bodyChain != null ? bodyChain : nextAfter, elseChain);
                }
                return bodyChain != null ? bodyChain : elseChain;
            }

            // Single-line else if <condition>:
            if (keyLower.startsWith("else if ") && key.length() > "else if ".length()) {
                String condPart = key.substring("else if ".length()).trim();
                Condition elseIfCond = SyntaxRegistry.get().parseCondition(condPart);
                if (elseIfCond == null && ("true".equals(condPart) || "false".equals(condPart))) {
                    elseIfCond = "true".equals(condPart) ? CondTrue.INSTANCE : CondFalse.INSTANCE;
                }
                if (elseIfCond != null) {
                    CoreTriggerItem elseChain = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                    CoreTriggerItem thenChain = buildChain(section.getChildren(), nextAfter);
                    return new ConditionalTriggerItem(elseIfCond, thenChain, elseChain);
                }
            }

            // Simple if <condition>: with optional else
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
                CoreTriggerItem thenChain = buildChain(thenNodes, nextAfter);
                CoreTriggerItem elseChain = buildChain(elseNodes, nextAfterThis);
                return new ConditionalTriggerItem(cond, thenChain, elseChain);
            }
            if (keyLower.startsWith("while ")) {
                String condPart = key.substring(6).trim();
                Condition whileCond = SyntaxRegistry.get().parseCondition(condPart);
                if (whileCond == null && ("true".equals(condPart) || "false".equals(condPart))) {
                    whileCond = "true".equals(condPart) ? CondTrue.INSTANCE : CondFalse.INSTANCE;
                }
                if (whileCond != null) {
                    CoreTriggerItem nextAfterLoop = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                    WhileTriggerItem whileItem = new WhileTriggerItem(whileCond, nextAfterLoop);
                    CoreTriggerItem bodyChain = buildChain(section.getChildren(), whileItem);
                    whileItem.setBodyFirst(bodyChain);
                    return whileItem;
                }
            }
            // parse: run section body once, clear ParseLogsHolder before
            if ("parse".equals(keyLower)) {
                CoreTriggerItem nextAfterThis = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                List<ScriptNode> children = section.getChildren();
                CoreTriggerItem bodyChain = buildChain(children, nextAfterThis);
                boolean bodyEmpty = children.isEmpty() || (bodyChain == nextAfterThis);
                return new ParseSectionTriggerItem(bodyChain, nextAfterThis, bodyEmpty);
            }
            // loop N times:
            if (keyLower.matches("loop\\s+\\d+\\s+times")) {
                Matcher loopMatcher = Pattern.compile("loop\\s+(\\d+)\\s+times", Pattern.CASE_INSENSITIVE).matcher(key);
                if (loopMatcher.find()) {
                    int n = Integer.parseInt(loopMatcher.group(1));
                    CoreTriggerItem nextAfterThis = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                    LoopNTimesTriggerItem loopItem = new LoopNTimesTriggerItem(n, nextAfterThis);
                    CoreTriggerItem bodyChain = buildChain(section.getChildren(), new LoopNTimesTriggerItem.Tail(loopItem));
                    loopItem.setBodyFirst(bodyChain);
                    return loopItem;
                }
            }
            // for ... in ... / loop ... in ... / loop %variable%: run body once (silent no-op for section key)
            if ((keyLower.startsWith("for ") && key.contains(" in "))
                || (keyLower.startsWith("loop ") && key.contains(" in "))
                || (keyLower.startsWith("loop ") && key.trim().length() > 5 && !keyLower.contains(" times"))) {
                CoreTriggerItem nextAfterThis = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                CoreTriggerItem bodyChain = buildChain(section.getChildren(), nextAfterThis);
                return bodyChain != null ? bodyChain : nextAfterThis;
            }
            // parse if <condition>: run body once when condition is true; when false at parse time do not parse body
            if (keyLower.startsWith("parse if ")) {
                String condPart = key.substring("parse if ".length()).trim();
                Condition parseIfCond = SyntaxRegistry.get().parseCondition(condPart);
                if (parseIfCond == null && ("true".equals(condPart) || "false".equals(condPart))) {
                    parseIfCond = "true".equals(condPart) ? CondTrue.INSTANCE : CondFalse.INSTANCE;
                }
                CoreTriggerItem elseChain = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                CoreTriggerItem bodyChain;
                if (parseIfCond != null && !evaluateConditionAtParseTime(parseIfCond)) {
                    bodyChain = elseChain;
                } else {
                    bodyChain = buildChain(section.getChildren(), nextAfter);
                }
                if (parseIfCond != null) {
                    return new ConditionalTriggerItem(parseIfCond, bodyChain != null ? bodyChain : nextAfter, elseChain);
                }
                return bodyChain != null ? bodyChain : elseChain;
            }
            // if running minecraft "version": / running below minecraft "version" (stub: run body)
            if (keyLower.startsWith("if running minecraft ") || keyLower.contains("running below minecraft")) {
                CoreTriggerItem nextAfterThis = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                CoreTriggerItem bodyChain = buildChain(section.getChildren(), nextAfterThis);
                return bodyChain != null ? bodyChain : nextAfterThis;
            }
            // suppress [the] ... warning[s]: run body (stub)
            if (keyLower.startsWith("suppress ")) {
                CoreTriggerItem nextAfterThis = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                CoreTriggerItem bodyChain = buildChain(section.getChildren(), nextAfterThis);
                return bodyChain != null ? bodyChain : nextAfterThis;
            }
            // loop 1, 2, and 3: / loop {_x}, {_y} and {_z}: iterate with loop-value
            if (keyLower.startsWith("loop ") && key.contains(",")) {
                String listPart = key.substring(5).trim();
                Object parsed = CoreTypes.get().parse("objects", listPart, ParseContextHolder.get());
                @SuppressWarnings("unchecked")
                List<Object> elements = parsed instanceof List ? (List<Object>) parsed : List.of();
                CoreTriggerItem nextAfterThis = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                LoopOverListTriggerItem loopItem = new LoopOverListTriggerItem(elements, nextAfterThis);
                CoreTriggerItem bodyChain = buildChain(section.getChildren(), new LoopOverListTriggerItem.Tail(loopItem));
                loopItem.setBodyFirst(bodyChain);
                return loopItem;
            }
            // loop blocks within ... / loop all itemtypes / any other "loop ...:" (stub: run body once)
            if (keyLower.startsWith("loop ")) {
                CoreTriggerItem nextAfterThis = buildChain(nodes.subList(1, nodes.size()), nextAfter);
                CoreTriggerItem bodyChain = buildChain(section.getChildren(), nextAfterThis);
                return bodyChain != null ? bodyChain : nextAfterThis;
            }
            Statement st = StatementParser.parseLine(key + ":");
            CoreTriggerItem next = buildChain(nodes.subList(1, nodes.size()), nextAfter);
            if (st != null) return new StatementTriggerItem(st, next);
            return next;
        }
        return nextAfter;
    }
}

