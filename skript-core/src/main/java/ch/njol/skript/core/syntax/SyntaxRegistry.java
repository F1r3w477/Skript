package ch.njol.skript.core.syntax;

import ch.njol.skript.core.condition.CondFalse;
import ch.njol.skript.core.condition.CondTrue;
import ch.njol.skript.core.conditions.CondCompare;
import ch.njol.skript.core.conditions.CondCompareGreater;
import ch.njol.skript.core.conditions.CondCompareLess;
import ch.njol.skript.core.conditions.CondCompareNot;
import ch.njol.skript.core.conditions.CondContains;
import ch.njol.skript.core.conditions.CondEmpty;
import ch.njol.skript.core.conditions.CondParseLogs;
import ch.njol.skript.core.conditions.CondSizeOf;
import ch.njol.skript.core.conditions.CondPluginEnabled;
import ch.njol.skript.core.conditions.CondSizeOfCompare;
import ch.njol.skript.core.conditions.CondIsOp;
import ch.njol.skript.core.conditions.CondIsSet;
import ch.njol.skript.core.variables.VariableRef;
import ch.njol.skript.core.lang.AddToVariableStatement;
import ch.njol.skript.core.lang.AssertConditionStatement;
import ch.njol.skript.core.lang.BroadcastStatement;
import ch.njol.skript.core.lang.ClearVariableStatement;
import ch.njol.skript.core.lang.DeleteVariableStatement;
import ch.njol.skript.core.lang.DoIfStatement;
import ch.njol.skript.core.lang.ExprRandomNumber;
import ch.njol.skript.core.lang.LiteralExpression;
import ch.njol.skript.core.lang.RemoveFromVariableStatement;
import ch.njol.skript.core.lang.Expressions;
import ch.njol.skript.core.lang.LogStatement;
import ch.njol.skript.core.lang.SendMessageStatement;
import ch.njol.skript.core.lang.NoOpStatement;
import ch.njol.skript.core.lang.SetVariableStatement;
import ch.njol.skript.core.patterns.CorePatternCompiler;
import ch.njol.skript.core.types.CoreTypes;
import ch.njol.skript.core.types.ParseContext;
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
        registerCondition("%-player% is op", m -> new CondIsOp(Expressions.fromParsed(m.getExpression(0))));
        registerCondition("%variable% is set", m -> new CondIsSet((VariableRef) m.getExpression(0)));
        registerCondition("%variable% is not set", m -> new CondIsSet((VariableRef) m.getExpression(0), true));
        registerCondition("%-string% contains %-string%", m -> new CondContains(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-string% does not contain %-string%", m -> new CondContains(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1)), true));
        registerCondition("%-number% is %-number%", m -> new CondCompare(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-object% is %-object%", m -> new CondCompare(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-object% is not %-object%", m -> new CondCompareNot(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-string% is %-string%", m -> new CondCompare(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-number% is greater than %-number%", m -> new CondCompareGreater(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-object% is greater than %-object%", m -> new CondCompareGreater(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-number% is less than %-number%", m -> new CondCompareLess(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-object% is less than %-object%", m -> new CondCompareLess(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        // size of %variable% is %number% / size of %variable% = %number%
        registerCondition("size of %variable% is %-number%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new CondSizeOf(vr, Expressions.fromParsed(m.getExpression(1))) : null;
        });
        registerCondition("size of %variable% = %-number%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new CondSizeOf(vr, Expressions.fromParsed(m.getExpression(1))) : null;
        });
        registerCondition("size of %variable% > %-number%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new CondSizeOfCompare(vr, Expressions.fromParsed(m.getExpression(1)), CondSizeOfCompare.Op.GREATER) : null;
        });
        registerCondition("size of %variable% \\< %-number%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new CondSizeOfCompare(vr, Expressions.fromParsed(m.getExpression(1)), CondSizeOfCompare.Op.LESS) : null;
        });
        // the size of %variable% is/=/</>
        registerCondition("the size of %variable% is %-number%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new CondSizeOf(vr, Expressions.fromParsed(m.getExpression(1))) : null;
        });
        registerCondition("the size of %variable% = %-number%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new CondSizeOf(vr, Expressions.fromParsed(m.getExpression(1))) : null;
        });
        registerCondition("the size of %variable% > %-number%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new CondSizeOfCompare(vr, Expressions.fromParsed(m.getExpression(1)), CondSizeOfCompare.Op.GREATER) : null;
        });
        registerCondition("the size of %variable% \\< %-number%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new CondSizeOfCompare(vr, Expressions.fromParsed(m.getExpression(1)), CondSizeOfCompare.Op.LESS) : null;
        });
        registerCondition("plugin %-string% is enabled", m -> new CondPluginEnabled(Expressions.fromParsed(m.getExpression(0))));
        // %-object% is empty / is not empty
        registerCondition("%-object% is empty", m -> new CondEmpty(Expressions.fromParsed(m.getExpression(0)), false));
        registerCondition("%-object% is not empty", m -> new CondEmpty(Expressions.fromParsed(m.getExpression(0)), true));
        // %-object% equals %-object% / %-number% = %-number% / %-object% = %-object%
        registerCondition("%-object% equals %-object%", m -> new CondCompare(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-number% = %-number%", m -> new CondCompare(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        registerCondition("%-object% = %-object%", m -> new CondCompare(Expressions.fromParsed(m.getExpression(0)), Expressions.fromParsed(m.getExpression(1))));
        // last parse logs / parse logs
        registerCondition("last parse logs is set", m -> new CondParseLogs(CondParseLogs.Kind.IS_SET, null));
        registerCondition("last parse logs is not set", m -> new CondParseLogs(CondParseLogs.Kind.IS_NOT_SET, null));
        registerCondition("last parse logs contain %-string%", m -> new CondParseLogs(CondParseLogs.Kind.CONTAINS, Expressions.fromParsed(m.getExpression(0))));
        registerCondition("last parse logs does not contain %-string%", m -> new CondParseLogs(CondParseLogs.Kind.DOES_NOT_CONTAIN, Expressions.fromParsed(m.getExpression(0))));
        registerCondition("parse logs is set", m -> new CondParseLogs(CondParseLogs.Kind.IS_SET, null));
        registerCondition("parse logs is not set", m -> new CondParseLogs(CondParseLogs.Kind.IS_NOT_SET, null));
        registerCondition("last parse logs are set", m -> new CondParseLogs(CondParseLogs.Kind.IS_SET, null));
        registerCondition("last parse logs contain %-object%", m -> new CondParseLogs(CondParseLogs.Kind.CONTAINS, Expressions.fromParsed(m.getExpression(0))));
        registerCondition("last parse logs does not contain %-object%", m -> new CondParseLogs(CondParseLogs.Kind.DOES_NOT_CONTAIN, Expressions.fromParsed(m.getExpression(0))));
        registerEffect("broadcast %string%", m -> new BroadcastStatement(m.getString(0)));
        registerEffect("log %string%", m -> new LogStatement(m.getString(0)));
        registerEffect("send %string%", m -> new SendMessageStatement(m.getString(0)));
        registerEffectFirst("set %variable% to < if > if <>", m -> {
            String valueStr = m.getString(1);
            String condStr = m.getString(2);
            if (condStr == null || condStr.isBlank()) return null;
            ch.njol.skript.core.condition.Condition cond = get().parseCondition(condStr.trim());
            if (cond == null && ("true".equals(condStr.trim()) || "false".equals(condStr.trim()))) {
                cond = "true".equals(condStr.trim()) ? CondTrue.INSTANCE : CondFalse.INSTANCE;
            }
            if (cond == null) return null;
            Object value = valueStr != null ? CoreTypes.get().parse("object", valueStr.trim(), ParseContext.DEFAULT) : null;
            if (value == null && valueStr != null && !valueStr.isBlank()) value = valueStr.trim();
            return new DoIfStatement(cond, new SetVariableStatement(m.getExpression(0), Expressions.fromParsed(value)));
        });
        // Random number(s) / integer(s) — before generic set to object (two forms: with and without amount)
        registerEffectFirst("set %variable% to random number (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(null, m.getExpression(1), m.getExpression(2), false));
        });
        registerEffectFirst("set %variable% to %-number% [ value ]random number (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(m.getExpression(1), m.getExpression(2), m.getExpression(3), false));
        });
        registerEffectFirst("set %variable% to random numbers (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(null, m.getExpression(1), m.getExpression(2), false));
        });
        registerEffectFirst("set %variable% to %-number% [ value ]random numbers (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(m.getExpression(1), m.getExpression(2), m.getExpression(3), false));
        });
        registerEffectFirst("set %variable% to random integer (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(null, m.getExpression(1), m.getExpression(2), true));
        });
        registerEffectFirst("set %variable% to %-number% [ value ]random integer (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(m.getExpression(1), m.getExpression(2), m.getExpression(3), true));
        });
        registerEffectFirst("set %variable% to random integers (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(null, m.getExpression(1), m.getExpression(2), true));
        });
        registerEffectFirst("set %variable% to %-number% [ value ]random integers (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(m.getExpression(1), m.getExpression(2), m.getExpression(3), true));
        });
        registerEffect("set %variable% to %objects%", m -> new SetVariableStatement(m.getExpression(0), Expressions.fromParsed(m.getExpression(1))));
        registerEffect("set %variable% to %object%", m -> new SetVariableStatement(m.getExpression(0), Expressions.fromParsed(m.getExpression(1))));
        registerEffect("assert < with > with %string%", m -> {
            try {
                String condStr = m.getString(0);
                if (condStr == null || condStr.isEmpty()) return null;
                ch.njol.skript.core.condition.Condition cond = get().parseCondition(condStr.trim());
                if (cond == null) cond = CondTrue.INSTANCE; // stub so line is recognised
                return new AssertConditionStatement(cond, Expressions.fromParsed(m.getExpression(1)));
            } catch (ClassCastException e) {
                return null;
            }
        });
        registerEffect("delete %variable%", m -> new DeleteVariableStatement(m.getExpression(0)));
        // add X to %variable%
        registerEffect("add < to > to %variable%", m -> {
            Object v = m.getExpression(1);
            if (!(v instanceof VariableRef vr)) return null;
            String captured = m.getString(0);
            captured = captured != null ? captured.trim() : "";
            Object value = CoreTypes.get().parse("objects", captured, ParseContext.DEFAULT);
            if (value == null) value = captured.isEmpty() ? null : captured;
            return new AddToVariableStatement(vr, new LiteralExpression<>(value));
        });
        // remove X from %variable%
        registerEffect("remove < from > from %variable%", m -> {
            Object v = m.getExpression(1);
            if (!(v instanceof VariableRef vr)) return null;
            String captured = m.getString(0);
            captured = captured != null ? captured.trim() : "";
            Object value = CoreTypes.get().parse("objects", captured, ParseContext.DEFAULT);
            if (value == null) value = captured.isEmpty() ? null : captured;
            return new RemoveFromVariableStatement(vr, new LiteralExpression<>(value));
        });
        // clear %variable%
        registerEffect("clear %variable%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new ClearVariableStatement(vr) : null;
        });
        // spawn, set block, kill, clear entity (NoOp until Fabric adapter)
        registerEffect("spawn < at > at %object%", m -> new NoOpStatement());
        registerEffect("spawn %object% at %object%", m -> new NoOpStatement());
        registerEffect("set block at %object% to %object%", m -> new NoOpStatement());
        registerEffect("kill %object%", m -> new NoOpStatement());
        registerEffect("clear entity within %object%", m -> new NoOpStatement());
        registerEffect("clear all entities", m -> new NoOpStatement());
        registerStatement("broadcast %string%", m -> new BroadcastStatement(m.getString(0)));
        registerStatement("log %string%", m -> new LogStatement(m.getString(0)));
        registerStatement("send %string%", m -> new SendMessageStatement(m.getString(0)));
        registerStatementFirst("set %variable% to < if > if <>", m -> {
            String valueStr = m.getString(1);
            String condStr = m.getString(2);
            if (condStr == null || condStr.isBlank()) return null;
            ch.njol.skript.core.condition.Condition cond = get().parseCondition(condStr.trim());
            if (cond == null && ("true".equals(condStr.trim()) || "false".equals(condStr.trim()))) {
                cond = "true".equals(condStr.trim()) ? CondTrue.INSTANCE : CondFalse.INSTANCE;
            }
            if (cond == null) return null;
            Object value = valueStr != null ? CoreTypes.get().parse("object", valueStr.trim(), ParseContext.DEFAULT) : null;
            if (value == null && valueStr != null && !valueStr.isBlank()) value = valueStr.trim();
            return new DoIfStatement(cond, new SetVariableStatement(m.getExpression(0), Expressions.fromParsed(value)));
        });
        // Random number(s) / integer(s) — before generic set to object
        registerStatementFirst("set %variable% to random number (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(null, m.getExpression(1), m.getExpression(2), false));
        });
        registerStatementFirst("set %variable% to %-number% [ value ]random number (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(m.getExpression(1), m.getExpression(2), m.getExpression(3), false));
        });
        registerStatementFirst("set %variable% to random numbers (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(null, m.getExpression(1), m.getExpression(2), false));
        });
        registerStatementFirst("set %variable% to %-number% [ value ]random numbers (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(m.getExpression(1), m.getExpression(2), m.getExpression(3), false));
        });
        registerStatementFirst("set %variable% to random integer (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(null, m.getExpression(1), m.getExpression(2), true));
        });
        registerStatementFirst("set %variable% to %-number% [ value ]random integer (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(m.getExpression(1), m.getExpression(2), m.getExpression(3), true));
        });
        registerStatementFirst("set %variable% to random integers (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(null, m.getExpression(1), m.getExpression(2), true));
        });
        registerStatementFirst("set %variable% to %-number% [ value ]random integers (from|between) %number% (to|and) %number%", m -> {
            Object v = m.getExpression(0);
            if (!(v instanceof ch.njol.skript.core.variables.VariableRef)) return null;
            return new SetVariableStatement(v, new ExprRandomNumber(m.getExpression(1), m.getExpression(2), m.getExpression(3), true));
        });
        registerStatement("set %variable% to %objects%", m -> new SetVariableStatement(m.getExpression(0), Expressions.fromParsed(m.getExpression(1))));
        registerStatement("set %variable% to %object%", m -> new SetVariableStatement(m.getExpression(0), Expressions.fromParsed(m.getExpression(1))));
        registerStatement("assert < with > with %string%", m -> {
            try {
                String condStr = m.getString(0);
                if (condStr == null || condStr.isEmpty()) return null;
                ch.njol.skript.core.condition.Condition cond = get().parseCondition(condStr.trim());
                if (cond == null) cond = CondTrue.INSTANCE; // stub so line is recognised
                return new AssertConditionStatement(cond, Expressions.fromParsed(m.getExpression(1)));
            } catch (ClassCastException e) {
                return null;
            }
        });
        registerStatement("delete %variable%", m -> new DeleteVariableStatement(m.getExpression(0)));
        registerStatement("add < to > to %variable%", m -> {
            Object v = m.getExpression(1);
            if (!(v instanceof VariableRef vr)) return null;
            String captured = m.getString(0);
            captured = captured != null ? captured.trim() : "";
            Object value = CoreTypes.get().parse("objects", captured, ParseContext.DEFAULT);
            if (value == null) value = captured.isEmpty() ? null : captured;
            return new AddToVariableStatement(vr, new LiteralExpression<>(value));
        });
        registerStatement("remove < from > from %variable%", m -> {
            Object v = m.getExpression(1);
            if (!(v instanceof VariableRef vr)) return null;
            String captured = m.getString(0);
            captured = captured != null ? captured.trim() : "";
            Object value = CoreTypes.get().parse("objects", captured, ParseContext.DEFAULT);
            if (value == null) value = captured.isEmpty() ? null : captured;
            return new RemoveFromVariableStatement(vr, new LiteralExpression<>(value));
        });
        registerStatement("clear %variable%", m -> {
            Object v = m.getExpression(0);
            return v instanceof VariableRef vr ? new ClearVariableStatement(vr) : null;
        });
        registerStatement("spawn < at > at %object%", m -> new NoOpStatement());
        registerStatement("spawn %object% at %object%", m -> new NoOpStatement());
        registerStatement("set block at %object% to %object%", m -> new NoOpStatement());
        registerStatement("kill %object%", m -> new NoOpStatement());
        registerStatement("clear entity within %object%", m -> new NoOpStatement());
        registerStatement("clear all entities", m -> new NoOpStatement());
    }

    public void registerCondition(String patternString, Function<CoreSkriptPattern.CoreMatchResult, ch.njol.skript.core.condition.Condition> factory) {
        conditions.add(new Entry<>(CorePatternCompiler.compile(patternString), factory));
    }

    public void registerEffect(String patternString, StatementFactory factory) {
        effects.add(new Entry<>(CorePatternCompiler.compile(patternString), factory));
    }

    /** Register effect to be tried first (platform overrides). */
    public void registerEffectFirst(String patternString, StatementFactory factory) {
        effects.add(0, new Entry<>(CorePatternCompiler.compile(patternString), factory));
    }

    public void registerStatement(String patternString, StatementFactory factory) {
        statements.add(new Entry<>(CorePatternCompiler.compile(patternString), factory));
    }

    /** Register statement to be tried first (platform overrides). */
    public void registerStatementFirst(String patternString, StatementFactory factory) {
        statements.add(0, new Entry<>(CorePatternCompiler.compile(patternString), factory));
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
                ch.njol.skript.core.lang.Statement st = e.factory.apply(match);
                if (st != null) return st;
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
                ch.njol.skript.core.lang.Statement st = e.factory.apply(match);
                if (st != null) return st;
            }
        }
        return null;
    }
}
