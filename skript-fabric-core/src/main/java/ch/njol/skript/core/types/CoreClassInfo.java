package ch.njol.skript.core.types;

/**
 * Minimal, platform-agnostic class info: type code name and parser.
 * Core only depends on this interface; platforms register implementations
 * (e.g. Player → SkriptPlayerInfo, Number → Double). No Bukkit types.
 *
 * @param <T> the type this info describes
 */
public final class CoreClassInfo<T> {

    private final String codeName;
    private final Class<T> type;
    private final TypeParser<T> parser;

    public CoreClassInfo(String codeName, Class<T> type, TypeParser<T> parser) {
        if (codeName == null || codeName.isEmpty()) throw new IllegalArgumentException("codeName");
        if (type == null) throw new IllegalArgumentException("type");
        if (parser == null) throw new IllegalArgumentException("parser");
        this.codeName = codeName;
        this.type = type;
        this.parser = parser;
    }

    public String getCodeName() {
        return codeName;
    }

    public Class<T> getType() {
        return type;
    }

    public T parse(String s, ParseContext context) {
        if (!parser.canParse(context)) return null;
        return parser.parse(s == null ? "" : s.trim(), context);
    }
}
