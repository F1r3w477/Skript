package ch.njol.skript.core.types;

import ch.njol.skript.core.event.EventValue;
import ch.njol.skript.core.variables.VariableRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of type parsers by code name (e.g. "player", "number", "string").
 * Used by the pattern parser to resolve %type% placeholders. Platforms register
 * their types via {@link ch.njol.skript.platform.SkriptPlatform#registerTypes};
 * core registers string, number, boolean, object, and variable by default.
 * <p>
 * To add a new type: register a {@link CoreClassInfo} with a code name (lowercase)
 * and a parser. Pattern slots like %type% then map 1:1 to getExpression(i) in
 * {@link ch.njol.skript.core.patterns.CoreSkriptPattern.CoreMatchResult}; use
 * {@link ch.njol.skript.core.lang.Expressions#fromParsed} when building conditions/effects.
 */
public final class CoreTypes {

    private static final CoreTypes INSTANCE = new CoreTypes();

    public static CoreTypes get() {
        return INSTANCE;
    }

    private final Map<String, CoreClassInfo<?>> byCodeName = new HashMap<>();

    private CoreTypes() {
        registerBuiltins();
    }

    @SuppressWarnings("unchecked")
    private void registerBuiltins() {
        register(new CoreClassInfo<>("string", String.class, (s, ctx) -> s));
        register(new CoreClassInfo<>("number", Number.class, (s, ctx) -> {
            if (s == null || s.isEmpty()) return null;
            try {
                if (s.contains(".")) return Double.parseDouble(s);
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }));
        register(new CoreClassInfo<>("boolean", Boolean.class, (s, ctx) -> {
            if (s == null) return null;
            String t = s.trim().toLowerCase();
            if ("true".equals(t)) return true;
            if ("false".equals(t)) return false;
            return null;
        }));
        register(new CoreClassInfo<>("object", Object.class, (s, ctx) -> {
            if (s == null) return null;
            String t = s.trim();
            if ("loop-value".equalsIgnoreCase(t)) return EventValue.LOOP_VALUE;
            if ("event-entity".equalsIgnoreCase(t)) return EventValue.ENTITY;
            if ("true".equalsIgnoreCase(t)) return true;
            if ("false".equalsIgnoreCase(t)) return false;
            return s;
        }));
        register(new CoreClassInfo<>("objects", List.class, (s, ctx) -> parseListLiteral(s)));
        register(new CoreClassInfo<>("variable", VariableRef.class, (s, ctx) -> {
            if (s == null || s.isEmpty()) return null;
            String t = s.trim();
            if (t.startsWith("{") && t.endsWith("}")) {
                t = t.substring(1, t.length() - 1).trim();
            }
            return t.isEmpty() ? null : new VariableRef(t);
        }));
    }

    /**
     * Parse a comma- and "and"-separated list literal (e.g. "1, 5, and 10").
     * Each part is parsed as number if possible, otherwise kept as string.
     */
    private static List<Object> parseListLiteral(String s) {
        if (s == null || s.trim().isEmpty()) return List.of();
        String t = s.trim();
        List<Object> out = new ArrayList<>();
        for (String part : t.split("\\s*,\\s*|\\s+and\\s+")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            Object val = parseListItem(p);
            out.add(val);
        }
        return out;
    }

    private static Object parseListItem(String p) {
        try {
            if (p.contains(".")) return Double.parseDouble(p);
            return Long.parseLong(p);
        } catch (NumberFormatException e) {
            if (p.length() >= 2 && (p.startsWith("\"") && p.endsWith("\"") || p.startsWith("'") && p.endsWith("'"))) {
                return p.substring(1, p.length() - 1).replace("\\\"", "\"").replace("\\'", "'");
            }
            return p;
        }
    }

    public <T> void register(CoreClassInfo<T> info) {
        byCodeName.put(info.getCodeName().toLowerCase(), info);
    }

    public CoreClassInfo<?> get(String codeName) {
        if (codeName == null) return null;
        return byCodeName.get(codeName.toLowerCase());
    }

    /**
     * Parse a segment as the given type. Returns the parsed value, or null if unknown type or parse failed.
     * If the type is registered and parsing succeeds, the result may be converted via {@link CoreConverters}
     * if a different target type is needed.
     */
    public Object parse(String codeName, String segment, ParseContext context) {
        CoreClassInfo<?> info = get(codeName);
        if (info == null) return null;
        return info.parse(segment, context);
    }
}
