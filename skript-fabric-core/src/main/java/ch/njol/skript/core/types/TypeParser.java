package ch.njol.skript.core.types;

/**
 * Parses a string into a value of type {@code T} in a given parse context.
 * Platform-agnostic; no Bukkit types.
 */
@FunctionalInterface
public interface TypeParser<T> {

    /**
     * Parse the input string. May return null if invalid for this context or format.
     *
     * @param s       trimmed input
     * @param context parse context
     * @return parsed value or null
     */
    T parse(String s, ParseContext context);

    /**
     * Whether this parser can produce a value in the given context (e.g. no event values in CONFIG).
     * Default: true.
     */
    default boolean canParse(ParseContext context) {
        return true;
    }
}
