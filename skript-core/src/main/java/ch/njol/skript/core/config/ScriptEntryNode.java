package ch.njol.skript.core.config;

import java.util.Collections;
import java.util.List;

/**
 * A single line entry in a script (no children).
 */
public record ScriptEntryNode(String key, int lineNum) implements ScriptNode {

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public int getLineNum() {
        return lineNum;
    }

    @Override
    public boolean isSection() {
        return false;
    }

    /**
     * Returns empty list (entries have no children).
     */
    public List<ScriptNode> getChildren() {
        return Collections.emptyList();
    }
}
