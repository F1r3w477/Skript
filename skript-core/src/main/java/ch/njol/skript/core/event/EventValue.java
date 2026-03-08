package ch.njol.skript.core.event;

/**
 * Sentinel for "event-player", "event-location" etc. When a pattern parses
 * "event-player" we store this instead of a literal; at runtime it is resolved
 * from {@link ch.njol.skript.core.RuntimeEventContext}. No Bukkit types.
 */
public enum EventValue {
    /** Resolves to context.getPlayer() */
    PLAYER,
    /** Resolves to context.getLocation() */
    LOCATION,
    /** Resolves to context.getEntity() */
    ENTITY,
    /** Resolves from ExecutionContext temporary "loop_value" */
    LOOP_VALUE;

    /**
     * Resolve this event value from the given context. Returns null if context is null
     * or the value is not set.
     */
    public Object getFrom(ch.njol.skript.core.RuntimeEventContext context) {
        if (context == null) return null;
        return switch (this) {
            case PLAYER -> context.getPlayer();
            case LOCATION -> context.getLocation();
            case ENTITY -> context.getEntity();
            case LOOP_VALUE -> null; // resolved from ExecutionContext, not RuntimeEventContext
        };
    }
}
