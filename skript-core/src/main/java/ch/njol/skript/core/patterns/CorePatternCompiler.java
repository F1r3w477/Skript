package ch.njol.skript.core.patterns;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compiles a pattern string (e.g. "broadcast %string%", "set [the] %variable% to %object%")
 * into a {@link CoreSkriptPattern}. Supports literals, %type%, optional [ ],
 * choice |, group ( ), and backslash escape. No dependency on Bukkit or legacy patterns.
 */
public final class CorePatternCompiler {

    private CorePatternCompiler() {}

    /**
     * Finds the closing bracket of the group starting after {@code start}.
     * Respects nesting and backslash escape.
     *
     * @param pattern the string to search in
     * @param closingBracket e.g. ']' or ')'
     * @param openingBracket e.g. '[' or '('
     * @param start index after the opening bracket (must be inside the group)
     * @param isGroup if true, require a matching close; if false, unexpected close throws
     * @return index of the closing bracket
     */
    public static int nextBracket(String pattern, char closingBracket, char openingBracket, int start, boolean isGroup) {
        int depth = 0;
        for (int i = start; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == closingBracket) {
                if (depth == 0) {
                    if (!isGroup) {
                        throw new MalformedPatternException(pattern, "Unexpected closing bracket '" + closingBracket + "'");
                    }
                    return i;
                }
                depth--;
            } else if (c == openingBracket) {
                depth++;
            }
        }
        if (isGroup) {
            throw new MalformedPatternException(pattern, "Missing closing bracket '" + closingBracket + "'");
        }
        return -1;
    }

    /**
     * Compiles a single pattern string. Throws {@link MalformedPatternException} if malformed.
     */
    public static CoreSkriptPattern compile(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return new CoreSkriptPattern(new CoreSkriptPattern.CoreLiteralElement(""), 0);
        }
        AtomicInteger expressionOffset = new AtomicInteger(0);
        try {
            CoreSkriptPattern.CorePatternElement first = compile(pattern, expressionOffset);
            return new CoreSkriptPattern(
                first != null ? first : new CoreSkriptPattern.CoreLiteralElement(""),
                expressionOffset.get()
            );
        } catch (MalformedPatternException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MalformedPatternException(pattern, "caught exception while compiling pattern", e);
        }
    }

    /**
     * Recursive compile. {@code expressionOffset} is incremented for each %type%.
     */
    static CoreSkriptPattern.CorePatternElement compile(String pattern, AtomicInteger expressionOffset) {
        CoreSkriptPattern.CorePatternElement first = null;
        CoreSkriptPattern.CorePatternElement currentEnd = null;
        StringBuilder literal = new StringBuilder();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '[') {
                if (literal.length() > 0) {
                    CoreSkriptPattern.CoreLiteralElement lit = new CoreSkriptPattern.CoreLiteralElement(literal.toString());
                    literal = new StringBuilder();
                    first = append(first, currentEnd, lit);
                    currentEnd = lit;
                }
                int end = nextBracket(pattern, ']', '[', i + 1, true);
                CoreSkriptPattern.CorePatternElement inner = compile(pattern.substring(i + 1, end), expressionOffset);
                CoreSkriptPattern.CoreOptionalElement opt = new CoreSkriptPattern.CoreOptionalElement(inner);
                first = append(first, currentEnd, opt);
                currentEnd = opt;
                i = end;
            } else if (c == '(') {
                if (literal.length() > 0) {
                    CoreSkriptPattern.CoreLiteralElement lit = new CoreSkriptPattern.CoreLiteralElement(literal.toString());
                    literal = new StringBuilder();
                    first = append(first, currentEnd, lit);
                    currentEnd = lit;
                }
                int end = nextBracket(pattern, ')', '(', i + 1, true);
                CoreSkriptPattern.CorePatternElement inner = compile(pattern.substring(i + 1, end), expressionOffset);
                CoreSkriptPattern.CoreGroupElement group = new CoreSkriptPattern.CoreGroupElement(inner);
                first = append(first, currentEnd, group);
                currentEnd = group;
                i = end;
            } else if (c == '|') {
                if (literal.length() > 0) {
                    CoreSkriptPattern.CoreLiteralElement lit = new CoreSkriptPattern.CoreLiteralElement(literal.toString());
                    literal = new StringBuilder();
                    first = append(first, currentEnd, lit);
                    currentEnd = lit;
                }
                CoreSkriptPattern.CoreChoiceElement choice;
                if (first instanceof CoreSkriptPattern.CoreChoiceElement) {
                    choice = (CoreSkriptPattern.CoreChoiceElement) first;
                    choice.addBranch(new CoreSkriptPattern.CoreLiteralElement(""));
                    currentEnd = choice.getLastBranch();
                } else {
                    choice = new CoreSkriptPattern.CoreChoiceElement();
                    choice.addBranch(first != null ? first : new CoreSkriptPattern.CoreLiteralElement(""));
                    choice.addBranch(new CoreSkriptPattern.CoreLiteralElement(""));
                    first = choice;
                    currentEnd = choice.getLastBranch();
                }
            } else if (c == '%') {
                if (literal.length() > 0) {
                    CoreSkriptPattern.CoreLiteralElement lit = new CoreSkriptPattern.CoreLiteralElement(literal.toString());
                    literal = new StringBuilder();
                    first = append(first, currentEnd, lit);
                    currentEnd = lit;
                }
                int end = pattern.indexOf('%', i + 1);
                if (end == -1) {
                    throw new MalformedPatternException(pattern, "Unclosed % at " + i);
                }
                String type = pattern.substring(i + 1, end).trim();
                boolean nullable = type.startsWith("-");
                if (nullable) type = type.substring(1).trim();
                type = type.isEmpty() ? "object" : type.toLowerCase(java.util.Locale.ROOT);
                int idx = expressionOffset.getAndIncrement();
                CoreSkriptPattern.CoreTypeElement typeEl = new CoreSkriptPattern.CoreTypeElement(idx, type, nullable);
                first = append(first, currentEnd, typeEl);
                currentEnd = typeEl;
                i = end;
            } else if (c == '\\') {
                i++;
                if (i >= pattern.length()) {
                    throw new MalformedPatternException(pattern, "Trailing backslash");
                }
                literal.append(pattern.charAt(i));
            } else {
                literal.append(c);
            }
        }

        if (literal.length() > 0) {
            CoreSkriptPattern.CoreLiteralElement lit = new CoreSkriptPattern.CoreLiteralElement(literal.toString());
            first = append(first, currentEnd, lit);
        }

        return first;
    }

    /** Appends {@code el} to the chain; if {@code currentEnd} is null we're at start. Calls onNextSet when linking. */
    private static CoreSkriptPattern.CorePatternElement append(
        CoreSkriptPattern.CorePatternElement first,
        CoreSkriptPattern.CorePatternElement currentEnd,
        CoreSkriptPattern.CorePatternElement el
    ) {
        if (first == null || (first instanceof CoreSkriptPattern.CoreLiteralElement && first.next == null && ((CoreSkriptPattern.CoreLiteralElement) first).isEmpty())) {
            return el;
        }
        if (currentEnd == null) {
            return el;
        }
        currentEnd.next = el;
        currentEnd.onNextSet(el);
        return first;
    }
}
