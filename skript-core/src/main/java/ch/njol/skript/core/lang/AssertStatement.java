package ch.njol.skript.core.lang;

import ch.njol.skript.core.TestRegistry;

/**
 * Test-mode effect: assert left is right; if not, fail the test with the given message.
 */
public final class AssertStatement implements Statement {

    private final boolean left;
    private final boolean right;
    private final String message;

    public AssertStatement(boolean left, boolean right, String message) {
        this.left = left;
        this.right = right;
        this.message = message != null ? message : "";
    }

    @Override
    public void run(ExecutionContext ctx) {
        if (left != right) {
            String testName = ctx.getTestName();
            if (testName != null) {
                TestRegistry.failTest(testName, message);
                ctx.getLogger().warn("[test] Assertion failed in '" + testName + "': " + message);
            } else {
                ctx.getLogger().warn("[test] Assertion failed with no associated test name: " + message);
            }
        }
    }
}
