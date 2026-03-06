package ch.njol.skript.core.types;

/**
 * Thread-local holder for current parse context. Syntax elements and type parsers
 * can read this to behave correctly (e.g. no event values in CONFIG). Slim equivalent
 * of legacy ParserInstance for context only.
 */
public final class ParseContextHolder {

    private static final ThreadLocal<ParseContext> CONTEXT = ThreadLocal.withInitial(() -> ParseContext.DEFAULT);

    public static ParseContext get() {
        return CONTEXT.get();
    }

    public static void set(ParseContext context) {
        CONTEXT.set(context == null ? ParseContext.DEFAULT : context);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
