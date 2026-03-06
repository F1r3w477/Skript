package ch.njol.skript.core.patterns;

import ch.njol.skript.core.types.CoreTypes;
import ch.njol.skript.core.types.ParseContextHolder;

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

        String remaining = first.match(trimmed, result);
        return remaining != null && remaining.isEmpty() ? result : null;
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

        /** Match this element against expr; fill result; return remaining string or null if no match. */
        abstract String match(String expr, CoreMatchResult result);

        abstract String toFullString();
    }

    public static final class CoreLiteralElement extends CorePatternElement {

        private final String literal;

        public CoreLiteralElement(String literal) {
            this.literal = literal;
        }

        @Override
        String match(String expr, CoreMatchResult result) {
            String lit = literal;
            if (lit.isEmpty()) {
                return next != null ? next.match(expr, result) : expr;
            }
            String lower = expr.toLowerCase(Locale.ROOT);
            String litLower = lit.toLowerCase(Locale.ROOT);
            if (!lower.startsWith(litLower)) {
                return null;
            }
            String remaining = expr.substring(lit.length()).trim();
            return next != null ? next.match(remaining, result) : remaining;
        }

        @Override
        String toFullString() {
            return literal;
        }
    }

    public static final class CoreTypeElement extends CorePatternElement {

        private final int index;
        private final String typeName;

        public CoreTypeElement(int index, String typeName) {
            this.index = index;
            this.typeName = typeName == null ? "object" : typeName;
        }

        /** Legacy constructor: type name not stored. */
        public CoreTypeElement(int index) {
            this(index, "object");
        }

        @Override
        String match(String expr, CoreMatchResult result) {
            if (expr.isEmpty()) return null;
            String consumed;
            Object toStore;
            if ("string".equalsIgnoreCase(typeName)) {
                String[] pair = matchString(expr);
                if (pair == null) return null;
                consumed = pair[0];
                toStore = pair[1];
            } else if ("number".equalsIgnoreCase(typeName)) {
                String num = matchNumber(expr);
                if (num == null) return null;
                consumed = num;
                toStore = parseNumber(num);
            } else {
                Object parsed = CoreTypes.get().parse(typeName, expr.trim(), ParseContextHolder.get());
                if (parsed == null) {
                    consumed = expr;
                    toStore = expr.trim();
                } else {
                    consumed = expr;
                    toStore = parsed;
                }
            }
            if (index >= 0 && index < result.expressions.length) {
                result.expressions[index] = toStore;
            }
            String remaining = expr.substring(consumed.length()).trim();
            return next != null ? next.match(remaining, result) : remaining;
        }

        /** Returns { consumed substring, unquoted value } or null. */
        private static String[] matchString(String expr) {
            String t = expr.trim();
            if (t.length() >= 2 && (t.startsWith("\"") || t.startsWith("'"))) {
                char q = t.charAt(0);
                int end = 1;
                while (end < t.length()) {
                    if (t.charAt(end) == '\\' && end + 1 < t.length()) { end += 2; continue; }
                    if (t.charAt(end) == q) {
                        String consumed = t.substring(0, end + 1);
                        String value = t.substring(1, end).replace("\\\"", "\"").replace("\\'", "'");
                        return new String[] { consumed, value };
                    }
                    end++;
                }
            }
            return new String[] { t, t };
        }

        private static String matchNumber(String expr) {
            String t = expr.trim();
            int i = 0;
            if (t.startsWith("-") || t.startsWith("+")) i = 1;
            while (i < t.length() && (Character.isDigit(t.charAt(i)) || t.charAt(i) == '.')) i++;
            return i > 0 ? t.substring(0, i) : null;
        }

        private static Object parseNumber(String s) {
            try {
                if (s.contains(".")) return Double.parseDouble(s);
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return s;
            }
        }

        @Override
        String toFullString() {
            return "%" + typeName + "%";
        }
    }

    public static final class CoreMatchResult {

        CoreSkriptPattern source;
        String expr;
        Object[] expressions;

        @SuppressWarnings("unchecked")
        public <T> T getExpression(int index) {
            if (expressions == null || index < 0 || index >= expressions.length) return null;
            return (T) expressions[index];
        }

        public String getString(int index) {
            Object o = getExpression(index);
            return o == null ? null : String.valueOf(o);
        }

        List<String> asDebugList() {
            List<String> values = new ArrayList<>();
            for (Object o : expressions) {
                values.add(o == null ? "<null>" : String.valueOf(o));
            }
            return values;
        }
    }
}

