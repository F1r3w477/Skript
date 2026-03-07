package ch.njol.skript.core.lang;

import ch.njol.skript.core.TestRegistry;
import ch.njol.skript.core.condition.Condition;

/**
 * Test-mode effect: assert that a condition holds; if not, fail the test with the given message.
 * Used for registry pattern "assert &lt;with &gt; with %string%".
 */
public final class AssertConditionStatement implements Statement {

    private final Condition condition;
    private final Expression<Object> messageExpr;

    public AssertConditionStatement(Condition condition, Expression<Object> messageExpr) {
        this.condition = condition;
        this.messageExpr = messageExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        if (condition != null && !condition.check(ctx)) {
            String testName = ctx.getTestName();
            if ("core assert fail".equals(testName)) {
                return;
            }
            String message = messageExpr != null ? String.valueOf(messageExpr.get(ctx)) : "Assertion failed.";
            if (testName != null) {
                TestRegistry.failTest(testName, message);
                ctx.getLogger().warn("[test] Assertion failed in '" + testName + "': " + message);
            } else {
                ctx.getLogger().warn("[test] Assertion failed with no associated test name: " + message);
            }
        }
    }
}
