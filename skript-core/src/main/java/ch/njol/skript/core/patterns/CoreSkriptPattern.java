package ch.njol.skript.core.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal, engine-local variant of {@code ch.njol.skript.patterns.SkriptPattern}
 * that is independent from the legacy Bukkit plugin. This will gradually
 * replace direct dependencies on the old patterns package as the core parser
 * is migrated into {@code skript-core}.
 */
public final class CoreSkriptPattern {

    private final CorePatternElement first;
    private final int expressionAmount;

    public CoreSkriptPattern(CorePatternElement first, int expressionAmount) {
        this.first = first;
        this.expressionAmount = expressionAmount;
    }

    public CoreMatchResult match(String expr) {
        String trimmed = expr == null ? "" : expr.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        CoreMatchResult result = new CoreMatchResult();
        result.source = this;
        result.expr = trimmed;
        result.expressions = new Object[expressionAmount];

        return first.match(trimmed, result);
    }

    int countTypes() {
        return expressionAmount;
    }

    @Override
    public String toString() {
        return first.toFullString();
    }

    /**
     * Very small, local pattern element hierarchy. This is intentionally
     * limited to what the provisional core parser needs; richer semantics
     * will be adapted from the legacy {@code ch.njol.skript.patterns}
     * package over time.
     */
    public abstract static class CorePatternElement {

        CorePatternElement next;

        abstract CoreMatchResult match(String expr, CoreMatchResult result);

        abstract String toFullString();
    }

    public static final class CoreLiteralElement extends CorePatternElement {

        private final String literal;

        public CoreLiteralElement(String literal) {
            this.literal = literal;
        }

        @Override
        CoreMatchResult match(String expr, CoreMatchResult result) {
            String lower = expr.toLowerCase(Locale.ROOT);
            if (!lower.contains(literal.toLowerCase(Locale.ROOT))) {
                return null;
            }
            return next != null ? next.match(expr, result) : result;
        }

        @Override
        String toFullString() {
            return literal;
        }
    }

    public static final class CoreTypeElement extends CorePatternElement {

        private final int index;

        public CoreTypeElement(int index) {
            this.index = index;
        }

        @Override
        CoreMatchResult match(String expr, CoreMatchResult result) {
            // For now, treat the entire remaining expression as the value
            // for this type slot. This will be tightened once the real
            // type system is migrated into skript-core.
            if (index >= 0 && index < result.expressions.length) {
                result.expressions[index] = expr;
            }
            return next != null ? next.match(expr, result) : result;
        }

        @Override
        String toFullString() {
            return "%type" + index + '%';
        }
    }

    public static final class CoreMatchResult {

        CoreSkriptPattern source;
        String expr;
        Object[] expressions;

        List<String> asDebugList() {
            List<String> values = new ArrayList<>();
            for (Object o : expressions) {
                values.add(o == null ? "<null>" : String.valueOf(o));
            }
            return values;
        }
    }
}

