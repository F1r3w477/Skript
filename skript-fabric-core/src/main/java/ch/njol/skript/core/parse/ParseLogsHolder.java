package ch.njol.skript.core.parse;

/**
 * Thread-local holder for last parse logs. The parse section populates this
 * when running its body; expressions like "last parse logs" read it.
 */
public final class ParseLogsHolder {

    private static final ThreadLocal<String> LAST_PARSE_LOGS = new ThreadLocal<>();

    private ParseLogsHolder() {}

    public static void set(String logs) {
        LAST_PARSE_LOGS.set(logs);
    }

    public static String get() {
        return LAST_PARSE_LOGS.get();
    }

    public static void clear() {
        LAST_PARSE_LOGS.remove();
    }
}
