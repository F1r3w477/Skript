package ch.njol.skript.core.lang;

import ch.njol.skript.core.CoreTestMode;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parses a single script line into a {@link Statement}.
 * Used by the core parser when building event handlers.
 */
public final class StatementParser {

    private static final Pattern SIMPLE_ASSERT = Pattern.compile(
        "^assert\\s+(true|false)\\s+is\\s+(true|false)\\s+with\\s+\"(.*)\"\\s*$",
        Pattern.CASE_INSENSITIVE);

    private StatementParser() {
    }

    /**
     * Parse one body line into a statement. Returns BroadcastStatement, LogStatement,
     * AssertStatement (only when {@link CoreTestMode#ENABLED}), or UnrecognisedStatement.
     */
    public static Statement parseLine(String line) {
        String trimmed = line.stripLeading();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null; // skip; caller will not add to list
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("broadcast ")) {
            String message = extractQuotedString(trimmed.substring("broadcast".length()));
            return new BroadcastStatement(message);
        }
        if (lower.startsWith("log ")) {
            String message = extractQuotedString(trimmed.substring("log".length()));
            return new LogStatement(message);
        }
        if (CoreTestMode.ENABLED) {
            var m = SIMPLE_ASSERT.matcher(trimmed);
            if (m.matches()) {
                boolean left = "true".equals(m.group(1).toLowerCase(Locale.ROOT));
                boolean right = "true".equals(m.group(2).toLowerCase(Locale.ROOT));
                String message = m.group(3);
                return new AssertStatement(left, right, message);
            }
        }
        return new UnrecognisedStatement(trimmed);
    }

    private static String extractQuotedString(String text) {
        String t = text.trim();
        if (t.length() < 2) return null;
        if ((t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"')
            || (t.charAt(0) == '\'' && t.charAt(t.length() - 1) == '\'')) {
            return t.substring(1, t.length() - 1);
        }
        return null;
    }
}
