package ch.njol.skript.core.event;

import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.variables.VariableRef;
import ch.njol.skript.core.variables.VariableScope;
import ch.njol.skript.platform.SkriptLocation;
import ch.njol.skript.platform.SkriptPlayerInfo;

/**
 * Resolves event values and variable references at runtime. Use when a parsed value
 * may be a literal, an {@link EventValue} (e.g. "event-player"), or a {@link VariableRef}.
 */
public final class EventValues {

    private EventValues() {}

    /**
     * Resolve event values and variable refs from context; otherwise return value as-is.
     */
    public static Object resolve(Object value, ExecutionContext ctx) {
        if (value instanceof EventValue ev) {
            return ev.getFrom(ctx != null ? ctx.getEventContext() : null);
        }
        if (value instanceof VariableRef ref) {
            VariableScope scope = ctx != null ? ctx.getVariableScope() : null;
            return scope != null ? scope.get(ref.getName()) : null;
        }
        return value;
    }

    /**
     * Resolve to player. Returns null if value is null, not a player, or event-player with no player in context.
     */
    public static SkriptPlayerInfo resolvePlayer(Object value, ExecutionContext ctx) {
        Object resolved = resolve(value, ctx);
        return resolved instanceof SkriptPlayerInfo ? (SkriptPlayerInfo) resolved : null;
    }

    /**
     * Resolve to location. Returns null if value is null, not a location, or event-location with no location in context.
     */
    public static SkriptLocation resolveLocation(Object value, ExecutionContext ctx) {
        Object resolved = resolve(value, ctx);
        return resolved instanceof SkriptLocation ? (SkriptLocation) resolved : null;
    }
}
