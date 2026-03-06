package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.platform.SkriptPlayerInfo;

/**
 * Condition: %player% is op. Uses platform {@link ch.njol.skript.platform.SkriptPlatform#isOp(SkriptPlayerInfo)}.
 */
public final class CondIsOp implements Condition {

    private final SkriptPlayerInfo player;

    public CondIsOp(SkriptPlayerInfo player) {
        this.player = player;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        return player != null && ctx.getPlatform() != null && ctx.getPlatform().isOp(player);
    }
}
