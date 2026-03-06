package ch.njol.skript.core.patterns;

/**
 * Thrown when a pattern string is malformed (e.g. unclosed bracket).
 */
public final class MalformedPatternException extends RuntimeException {

    public MalformedPatternException(String pattern, String message) {
        super(message + " (pattern: " + pattern + ")");
    }

    public MalformedPatternException(String pattern, String message, Throwable cause) {
        super(message + " (pattern: " + pattern + ")", cause);
    }
}
