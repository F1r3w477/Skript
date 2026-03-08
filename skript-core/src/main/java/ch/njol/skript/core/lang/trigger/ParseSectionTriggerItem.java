package ch.njol.skript.core.lang.trigger;

import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.parse.ParseLogsHolder;

/**
 * Wraps a parse section body. Clears ParseLogsHolder before running so
 * "last parse logs" is not set from prior runs. When the body is empty
 * (no code), sets the expected error so tests can assert on it.
 */
public final class ParseSectionTriggerItem extends CoreTriggerItem {

    private static final String EMPTY_PARSE_ERROR = "A parse section must contain code";

    private final CoreTriggerItem body;
    private final CoreTriggerItem next;
    private final boolean bodyEmpty;

    public ParseSectionTriggerItem(CoreTriggerItem body, CoreTriggerItem next) {
        this(body, next, false);
    }

    public ParseSectionTriggerItem(CoreTriggerItem body, CoreTriggerItem next, boolean bodyEmpty) {
        this.body = body;
        this.next = next;
        this.bodyEmpty = bodyEmpty;
    }

    @Override
    public CoreTriggerItem run(ExecutionContext ctx) {
        if (Boolean.getBoolean("skript.fabric.trace")) {
            ctx.getLogger().info("[FabricTrace] ParseSectionTriggerItem.run: bodyEmpty=" + bodyEmpty);
        }
        // Only clear when we will run the body; empty parse just sets the error so the next (e.g. assert) can read it
        if (!bodyEmpty) {
            ParseLogsHolder.clear();
        }
        if (bodyEmpty) {
            ParseLogsHolder.set(EMPTY_PARSE_ERROR);
            if (Boolean.getBoolean("skript.fabric.trace")) {
                ctx.getLogger().info("[FabricTrace] ParseSectionTriggerItem.run: set EMPTY_PARSE_ERROR, returning next");
            }
            return next;
        }
        if (body != null) {
            CoreTriggerItem current = body;
            while (current != null) {
                current = current.run(ctx);
            }
        }
        return next;
    }
}
