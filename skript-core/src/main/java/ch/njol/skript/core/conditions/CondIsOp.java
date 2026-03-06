package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;
import ch.njol.skript.platform.SkriptPlayerInfo;

/**
 * Condition: %player% is op (supports "event-player"). Uses platform
 * {@link ch.njol.skript.platform.SkriptPlatform#isOp(SkriptPlayerInfo)}.
 */
public final class CondIsOp implements Condition {

    private final Expression<Object> playerExpr;

    public CondIsOp(Expression<Object> playerExpr) {
        this.playerExpr = playerExpr;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object value = playerExpr != null ? playerExpr.get(ctx) : null;
        SkriptPlayerInfo player = value instanceof SkriptPlayerInfo ? (SkriptPlayerInfo) value : null;
        return player != null && ctx.getPlatform() != null && ctx.getPlatform().isOp(player);
    }
}
