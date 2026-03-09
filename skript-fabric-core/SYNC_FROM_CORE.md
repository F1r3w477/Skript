# Syncing from upstream skript-core

This module contains a **copy** of `skript-core` (packages `ch.njol.skript.core.*` and `ch.njol.skript.platform.*`) so Fabric can apply changes without editing upstream. When you pull changes from the main Skript repo, periodically refresh this copy:

1. Copy the core tree from upstream over this module’s copy:
   ```bash
   cp -r skript-core/src/main/java/ch/njol/skript/core skript-fabric-core/src/main/java/ch/njol/skript/
   cp -r skript-core/src/main/java/ch/njol/skript/platform skript-fabric-core/src/main/java/ch/njol/skript/
   ```
2. Re-apply these Fabric-specific edits in the **fabric-core** copy (so Fabric has a stable API without reflection):
   - **SkriptBootstrap.java**: Add `getLoadedScripts()` returning `List<ScriptFile>` (and imports for `ScriptFile`, `List`, `Collections`); delegate to `engine.getLoadedScripts()`.
   - **SkriptEngine.java**: Make `getLoadedScripts()` public.
   - **SyntaxRegistry.java**: Add `registerConditionFirst(String, Function<...>)` that does `conditions.add(0, new Entry<>(...))`.
   - **ExecutionContext.java**: Add constructor that takes `ScriptFile currentScript`; add field and getters `getCurrentScript()` and `getCurrentScriptPath()`.
   - **SkriptRuntime.java**: Add `Map<ScriptEventHandler, ScriptFile> scriptByHandler`; in `registerScripts` put each handler→script; in `executeHandler` pass `scriptByHandler.get(handler)` into `ExecutionContext`.

Fabric build uses only `skript-fabric-core` (not `skript-core`), so upstream core stays untouched for sync.
