package ch.njol.skript.core.config;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Splits a config line into value and comment. Matches legacy behaviour:
 * # starts a line comment unless inside a double-quoted string; ## in value
 * becomes single #; ### toggles block comment.
 */
final class LineSplit {

    private LineSplit() {}

    static Result splitLine(String line, AtomicBoolean inBlockComment) {
        String trimmed = line.trim();
        if (trimmed.equals("###")) {
            inBlockComment.set(!inBlockComment.get());
            return new Result("", line);
        }
        if (trimmed.startsWith("#")) {
            int idx = line.indexOf('#');
            return new Result("", idx >= 0 ? line.substring(idx) : "");
        }
        if (inBlockComment.get()) {
            return new Result("", line);
        }

        int length = line.length();
        StringBuilder value = new StringBuilder(line);
        int removed = 0;
        int state = CODE;
        int previousState = CODE;

        for (int i = 0; i < length; i++) {
            char c = line.charAt(i);
            if (c == '%' || c == '"' || c == '#') {
                if ((c != '#' || state != STRING) && i + 1 < length && line.charAt(i + 1) == c) {
                    if (c == '#') {
                        value.deleteCharAt(i - removed);
                        removed++;
                    }
                    i++;
                    continue;
                }
                int next = updateState(c, state, previousState);
                if (next == HALT) {
                    return new Result(value.substring(0, i - removed), line.substring(i));
                }
                if (c == '%' && next == CODE) {
                    previousState = state;
                }
                state = next;
            }
        }
        return new Result(value.toString(), "");
    }

    private static final int HALT = 0, CODE = 1, STRING = 2, VARIABLE = 3;

    private static int updateState(char c, int state, int previousState) {
        if (state == HALT) return HALT;
        switch (c) {
            case '%':
                return state == CODE ? previousState : CODE;
            case '"':
                return (state == CODE || state == STRING) ? (state == CODE ? STRING : CODE) : state;
            case '{':
                return state == STRING ? STRING : VARIABLE;
            case '}':
                return state == STRING ? STRING : CODE;
            case '#':
                return state == STRING ? STRING : HALT;
            default:
                return state;
        }
    }

    record Result(String value, String comment) {}
}
