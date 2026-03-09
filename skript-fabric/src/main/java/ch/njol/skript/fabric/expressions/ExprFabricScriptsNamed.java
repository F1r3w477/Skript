package ch.njol.skript.fabric.expressions;

import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;
import ch.njol.skript.fabric.util.FabricLoadedScripts;
import ch.njol.skript.core.model.ScriptFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Expression that returns a list of script path strings for "the scripts named X and Y".
 */
public final class ExprFabricScriptsNamed implements Expression<Object> {

    private final Expression<Object> namesExpr;
    private final Expression<Object> secondNameExpr;

    public ExprFabricScriptsNamed(Expression<Object> namesExpr) {
        this.namesExpr = namesExpr;
        this.secondNameExpr = null;
    }

    /** For "scripts named %object% and %object%" - both slots evaluated at runtime. */
    public ExprFabricScriptsNamed(Expression<Object> first, Expression<Object> second) {
        this.namesExpr = first;
        this.secondNameExpr = second;
    }

    @Override
    public Object get(ExecutionContext ctx) {
        if (secondNameExpr != null) {
            String a = toPathOrName(namesExpr != null ? namesExpr.get(ctx) : null);
            String b = toPathOrName(secondNameExpr.get(ctx));
            List<String> paths = new ArrayList<>();
            if (a != null) {
                ScriptFile sa = FabricLoadedScripts.findScript(a);
                if (sa != null) paths.add(sa.getPath().toString().replace('\\', '/'));
            }
            if (b != null) {
                ScriptFile sb = FabricLoadedScripts.findScript(b);
                if (sb != null) paths.add(sb.getPath().toString().replace('\\', '/'));
            }
            return paths;
        }
        Object names = namesExpr != null ? namesExpr.get(ctx) : null;
        List<String> pathOrNames = toList(names);
        if (pathOrNames.isEmpty()) return Collections.emptyList();
        List<String> paths = new ArrayList<>();
        for (String pathOrName : pathOrNames) {
            if (pathOrName == null || pathOrName.isBlank()) continue;
            ScriptFile script = FabricLoadedScripts.findScript(pathOrName.trim());
            if (script != null)
                paths.add(script.getPath().toString().replace('\\', '/'));
        }
        return paths;
    }

    private static String toPathOrName(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static List<String> toList(Object o) {
        if (o == null) return Collections.emptyList();
        if (o instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object e : list) {
                if (e != null) out.add(e.toString().trim());
            }
            return out;
        }
        return List.of(o.toString().trim());
    }
}
