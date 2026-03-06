package ch.njol.skript.core.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compiles a pattern string (e.g. "broadcast %string%", "%-player% is op") into a
 * {@link CoreSkriptPattern}. Supports literals and %type% placeholders (string, number).
 * No dependency on Bukkit or legacy patterns.
 */
public final class CorePatternCompiler {

    private CorePatternCompiler() {}

    /**
     * Compiles a single pattern string. Throws if malformed (e.g. unclosed %).
     */
    public static CoreSkriptPattern compile(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return new CoreSkriptPattern(new CoreSkriptPattern.CoreLiteralElement(""), 0);
        }
        List<CoreSkriptPattern.CorePatternElement> elements = new ArrayList<>();
        List<String> typeNames = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '%') {
                if (literal.length() > 0) {
                    elements.add(new CoreSkriptPattern.CoreLiteralElement(literal.toString()));
                    literal = new StringBuilder();
                }
                int end = pattern.indexOf('%', i + 1);
                if (end == -1) throw new IllegalArgumentException("Unclosed % in pattern: " + pattern);
                String type = pattern.substring(i + 1, end).trim();
                if (type.startsWith("-")) type = type.substring(1).trim();
                typeNames.add(type.isEmpty() ? "object" : type);
                elements.add(new CoreSkriptPattern.CoreTypeElement(typeNames.size() - 1, type));
                i = end + 1;
                continue;
            }
            literal.append(c);
            i++;
        }
        if (literal.length() > 0) {
            elements.add(new CoreSkriptPattern.CoreLiteralElement(literal.toString()));
        }
        CoreSkriptPattern.CorePatternElement first = null;
        CoreSkriptPattern.CorePatternElement prev = null;
        for (CoreSkriptPattern.CorePatternElement el : elements) {
            if (first == null) first = el;
            if (prev != null) prev.next = el;
            prev = el;
        }
        return new CoreSkriptPattern(first != null ? first : new CoreSkriptPattern.CoreLiteralElement(""), typeNames.size());
    }

}
