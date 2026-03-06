package ch.njol.skript.core.types;

/**
 * Context in which a pattern or expression is being parsed.
 * Used so syntax elements and type parsers can behave correctly
 * (e.g. no event values in CONFIG). No Bukkit dependency.
 */
public enum ParseContext {
    /** Normal script parsing (event handlers, etc.). */
    DEFAULT,
    /** Command argument parsing. */
    COMMAND,
    /** Config file parsing (e.g. options). */
    CONFIG
}
