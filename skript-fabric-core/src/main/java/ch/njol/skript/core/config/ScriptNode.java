package ch.njol.skript.core.config;

/**
 * A single node in a script config tree (section or entry).
 * Platform-agnostic; no dependency on Bukkit or Fabric.
 */
public sealed interface ScriptNode permits ScriptSectionNode, ScriptEntryNode {

    /**
     * Key: for a section, the header line without trailing colon;
     * for an entry, the trimmed line content.
     */
    String getKey();

    /**
     * 1-based line number in the source file, or -1 if unknown.
     */
    int getLineNum();

    /**
     * True if this is a section (has children); false if an entry (single line).
     */
    boolean isSection();
}
