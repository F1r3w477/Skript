package ch.njol.skript.core.syntax;

import ch.njol.skript.core.condition.CondFalse;
import ch.njol.skript.core.condition.CondTrue;
import ch.njol.skript.core.lang.BroadcastStatement;
import ch.njol.skript.core.lang.LogStatement;
import ch.njol.skript.core.patterns.CorePatternCompiler;
import ch.njol.skript.core.patterns.CoreSkriptPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Registry of syntax patterns (conditions, effects, sections, statements).
 * Given a line and a kind, tries registered patterns in order and returns the first match.
 * No Bukkit or plugin types in the API.
 */
public final class SyntaxRegistry {

    private static final SyntaxRegistry INSTANCE = new SyntaxRegistry();

    public static SyntaxRegistry get() {
        return INSTANCE;
    }

    private final List<Entry<ch.njol.skript.core.condition.Condition>> conditions = new ArrayList<>();
    private final List<Entry<ch.njol.skript.core.lang.Statement>> effects = new ArrayList<>();
    private final List<Entry<ch.njol.skript.core.lang.Statement>> statements = new ArrayList<>();

    /** Factory that produces a {@link ch.njol.skript.core.lang.Statement} from a match. */
    @FunctionalInterface
    public interface StatementFactory extends Function<CoreSkriptPattern.CoreMatchResult, ch.njol.skript.core.lang.Statement> {}

    private static final class Entry<T> {
        final CoreSkriptPattern pattern;
        final Function<CoreSkriptPattern.CoreMatchResult, T> factory;

        Entry(CoreSkriptPattern pattern, Function<CoreSkriptPattern.CoreMatchResult, T> factory) {
            this.pattern = pattern;
            this.factory = factory;
        }
    }

    private SyntaxRegistry() {
        registerBuiltins();
    }

    private void registerBuiltins() {
        registerCondition("true", m -> CondTrue.INSTANCE);
        registerCondition("false", m -> CondFalse.INSTANCE);
        registerEffect("broadcast %string%", m -> new BroadcastStatement(m.getString(0)));
        registerEffect("log %string%", m -> new LogStatement(m.getString(0)));
        registerStatement("broadcast %string%", m -> new BroadcastStatement(m.getString(0)));
        registerStatement("log %string%", m -> new LogStatement(m.getString(0)));
    }

    public void registerCondition(String patternString, Function<CoreSkriptPattern.CoreMatchResult, ch.njol.skript.core.condition.Condition> factory) {
        conditions.add(new Entry<>(CorePatternCompiler.compile(patternString), factory));
    }

    public void registerEffect(String patternString, StatementFactory factory) {
        effects.add(new Entry<>(CorePatternCompiler.compile(patternString), factory));
    }

    public void registerStatement(String patternString, StatementFactory factory) {
        statements.add(new Entry<>(CorePatternCompiler.compile(patternString), factory));
    }

    public ch.njol.skript.core.condition.Condition parseCondition(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        for (Entry<ch.njol.skript.core.condition.Condition> e : conditions) {
            CoreSkriptPattern.CoreMatchResult match = e.pattern.match(trimmed);
            if (match != null) {
                return e.factory.apply(match);
            }
        }
        return null;
    }

    public ch.njol.skript.core.lang.Statement parseEffect(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        for (Entry<ch.njol.skript.core.lang.Statement> e : effects) {
            CoreSkriptPattern.CoreMatchResult match = e.pattern.match(trimmed);
            if (match != null) {
                return e.factory.apply(match);
            }
        }
        return null;
    }

    public ch.njol.skript.core.lang.Statement parseStatement(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        for (Entry<ch.njol.skript.core.lang.Statement> e : statements) {
            CoreSkriptPattern.CoreMatchResult match = e.pattern.match(trimmed);
            if (match != null) {
                return e.factory.apply(match);
            }
        }
        return null;
    }
}
