package ch.njol.skript.core.patterns;

import ch.njol.skript.core.types.CoreTypes;
import ch.njol.skript.core.types.ParseContextHolder;
import ch.njol.skript.core.variables.VariableRef;

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
        return remaining != null && remaining.trim().isEmpty() ? result : null;
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

        /** Set the last element in this chain to point to newNext (used for optional/choice so inner chain continues). */
        void setLastNext(CorePatternElement newNext) {
            CorePatternElement last = this;
            while (last.next != null) last = last.next;
            last.next = newNext;
        }

        /** Called when this element's next is set, so optional/choice/group can wire inner chain. Default: no-op. */
        void onNextSet(CorePatternElement next) {}
    }

    public static final class CoreLiteralElement extends CorePatternElement {

        private final String literal;

        public CoreLiteralElement(String literal) {
            this.literal = literal;
        }

        boolean isEmpty() {
            return literal.isEmpty();
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
            String afterLit = expr.substring(lit.length());
            String remaining = afterLit.trim();
            return next != null ? next.match(remaining, result) : afterLit;
        }

        @Override
        String toFullString() {
            return literal;
        }
    }

    public static final class CoreTypeElement extends CorePatternElement {

        private final int index;
        private final String typeName;
        private final boolean nullable;

        public CoreTypeElement(int index, String typeName) {
            this(index, typeName, false);
        }

        public CoreTypeElement(int index, String typeName, boolean nullable) {
            this.index = index;
            this.typeName = typeName == null || typeName.isEmpty() ? "object" : typeName;
            this.nullable = nullable;
        }

        /** Legacy constructor: type name not stored. */
        public CoreTypeElement(int index) {
            this(index, "object", false);
        }

        @Override
        String match(String expr, CoreMatchResult result) {
            if (expr.isEmpty()) {
                if (nullable) {
                    if (index >= 0 && index < result.expressions.length) result.expressions[index] = null;
                    return next != null ? next.match(expr, result) : expr;
                }
                return null;
            }
            String consumed;
            Object toStore;
            if ("string".equalsIgnoreCase(typeName)) {
                String[] pair = matchString(expr);
                if (pair == null) {
                    if (nullable) { consumed = ""; toStore = null; }
                    else return null;
                } else {
                    int start = 0;
                    while (start < expr.length() && Character.isWhitespace(expr.charAt(start))) start++;
                    consumed = expr.substring(start, start + pair[0].length());
                    toStore = pair[1];
                }
            } else if ("number".equalsIgnoreCase(typeName)) {
                String num = matchNumber(expr);
                if (num == null) {
                    if (nullable) { consumed = ""; toStore = null; }
                    else return null;
                } else {
                    int start = 0;
                    while (start < expr.length() && Character.isWhitespace(expr.charAt(start))) start++;
                    consumed = expr.substring(start, start + num.length());
                    toStore = parseNumber(num);
                }
            } else if ("variable".equalsIgnoreCase(typeName)) {
                String token = matchVariableToken(expr);
                if (token == null) {
                    if (nullable) { consumed = ""; toStore = null; }
                    else return null;
                } else {
                    int start = expr.indexOf(token);
                    if (start < 0) start = 0;
                    consumed = expr.substring(start, start + token.length());
                    Object parsed = CoreTypes.get().parse("variable", token, ParseContextHolder.get());
                    toStore = parsed != null ? parsed : new VariableRef(token);
                }
            } else {
                Object parsed = CoreTypes.get().parse(typeName, expr.trim(), ParseContextHolder.get());
                if (parsed == null) {
                    if (nullable) {
                        consumed = "";
                        toStore = null;
                    } else {
                        consumed = expr;
                        toStore = expr.trim();
                    }
                } else {
                    consumed = expr;
                    toStore = parsed;
                }
            }
            if (index >= 0 && index < result.expressions.length) {
                result.expressions[index] = toStore;
            }
            // Preserve leading space so next literal (e.g. " to ") can match
            String remaining = expr.substring(consumed.length());
            return next != null ? next.match(remaining, result) : remaining.trim();
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

        /** Consume a variable token: {name} or a single word. Prefer { } when present so " the {_x} to 5" matches {_x}. */
        private static String matchVariableToken(String expr) {
            String t = expr.trim();
            if (t.isEmpty()) return null;
            int brace = t.indexOf('{');
            if (brace >= 0) {
                int end = t.indexOf('}', brace + 1);
                if (end == -1) return null;
                return t.substring(brace, end + 1);
            }
            int space = t.indexOf(' ');
            return space == -1 ? t : t.substring(0, space);
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

    /**
     * Optional part [ ... ]. Tries inner; if it matches returns that, else continues with no consumption.
     */
    public static final class CoreOptionalElement extends CorePatternElement {

        private final CorePatternElement inner;

        public CoreOptionalElement(CorePatternElement inner) {
            this.inner = inner;
        }

        @Override
        String match(String expr, CoreMatchResult result) {
            CoreMatchResult copy = result.copy();
            String remaining = inner.match(expr, copy);
            if (remaining != null) {
                System.arraycopy(copy.expressions, 0, result.expressions, 0, Math.min(copy.expressions.length, result.expressions.length));
                return next != null ? next.match(remaining, result) : remaining;
            }
            return next != null ? next.match(expr, result) : expr;
        }

        @Override
        String toFullString() {
            return "[" + inner.toFullString() + "]";
        }

        @Override
        void onNextSet(CorePatternElement next) {
            inner.setLastNext(next);
        }
    }

    /**
     * Choice a|b|c. Tries each branch in order; first full match wins.
     */
    public static final class CoreChoiceElement extends CorePatternElement {

        private final List<CorePatternElement> branches = new ArrayList<>();

        public void addBranch(CorePatternElement branch) {
            branches.add(branch);
        }

        /** Last branch (for compiler: append subsequent elements to this branch after |). */
        public CorePatternElement getLastBranch() {
            return branches.isEmpty() ? null : branches.get(branches.size() - 1);
        }

        @Override
        String match(String expr, CoreMatchResult result) {
            for (CorePatternElement branch : branches) {
                CoreMatchResult copy = result.copy();
                String remaining = branch.match(expr, copy);
                if (remaining != null) {
                    System.arraycopy(copy.expressions, 0, result.expressions, 0, Math.min(copy.expressions.length, result.expressions.length));
                    return next != null ? next.match(remaining, result) : remaining;
                }
            }
            return null;
        }

        @Override
        String toFullString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < branches.size(); i++) {
                if (i > 0) sb.append('|');
                sb.append(branches.get(i).toFullString());
            }
            return sb.toString();
        }

        @Override
        void onNextSet(CorePatternElement next) {
            for (CorePatternElement branch : branches) {
                branch.setLastNext(next);
            }
        }
    }

    /**
     * Group ( ... ). Matches inner as one unit.
     */
    public static final class CoreGroupElement extends CorePatternElement {

        private final CorePatternElement inner;

        public CoreGroupElement(CorePatternElement inner) {
            this.inner = inner;
        }

        @Override
        String match(String expr, CoreMatchResult result) {
            String remaining = inner.match(expr, result);
            if (remaining == null) return null;
            return next != null ? next.match(remaining, result) : remaining;
        }

        @Override
        String toFullString() {
            return "(" + inner.toFullString() + ")";
        }
    }

    /**
     * Result of a successful pattern match. Expression indices correspond to %type% slots
     * in the pattern in left-to-right order (index 0 = first %type%, etc.). Use
     * {@link #getExpression(int)} to obtain the parsed value; use {@link ch.njol.skript.core.lang.Expressions#fromParsed}
     * when building conditions/effects from the match.
     */
    public static final class CoreMatchResult {

        CoreSkriptPattern source;
        String expr;
        Object[] expressions;

        /** Shallow copy for use in optional/choice branches. */
        CoreMatchResult copy() {
            CoreMatchResult c = new CoreMatchResult();
            c.source = source;
            c.expr = expr;
            c.expressions = expressions != null ? expressions.clone() : null;
            return c;
        }

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

