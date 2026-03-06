package ch.njol.skript.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A section in a script (header line ending with ":" and nested children).
 */
public final class ScriptSectionNode implements ScriptNode {

    private final String key;
    private final int lineNum;
    private final List<ScriptNode> children;

    public ScriptSectionNode(String key, int lineNum, List<ScriptNode> children) {
        this.key = key;
        this.lineNum = lineNum;
        this.children = List.copyOf(children);
    }

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
        return true;
    }

    public List<ScriptNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Mutable builder for constructing a section before freezing.
     */
    public static final class Builder {
        private final String key;
        private final int lineNum;
        private final List<ScriptNode> children = new ArrayList<>();

        public Builder(String key, int lineNum) {
            this.key = key;
            this.lineNum = lineNum;
        }

        public void add(ScriptNode node) {
            children.add(node);
        }

        public ScriptSectionNode build() {
            return new ScriptSectionNode(key, lineNum, new ArrayList<>(children));
        }
    }
}
