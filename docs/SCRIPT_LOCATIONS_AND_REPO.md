# Script locations, project hierarchy, and repo strategy

## 1. Where script files are used

### Production (Fabric server, no test mode)

- **Config root:** `FabricLoader.getConfigDir()/skript` (e.g. `config/skript` under the server run dir).
- **Scripts directory:** `config/skript/scripts` — user `.sk` files go here.
- **What the engine loads:** Scripts are preprocessed from `scripts` into `config/skript/scripts-preprocessed`, and the engine loads from `scripts-preprocessed` (see `FabricSkriptPlatform.getScriptsDirectory()` and `FabricScriptsDirectory.prepareScriptsDirectory()`).

So in production, only the preprocessed work dir is used for loading; the “real” location is `config/skript/scripts`.

### Tests (`./gradlew quickTestFabric`)

- **Test runner** (JVM that runs `PlatformMain`):  
  - `args[1]` = `src/test/skript/tests` (or `src/test/skript/junit` for JUnit).  
  - Turned into an **absolute** path: `testsRoot = Paths.get(args[1]).toAbsolutePath()`.
- **Fabric subprocess** is started with:
  - `-Dskript.testing.enabled=true`
  - `-Dskript.testing.dir=<absolute path to src/test/skript/tests>`
  - Working directory: `build/test_runners/fabric-1.21.11` (or the env name).
- **Fabric side** (`FabricSkriptPlatform`):
  - If `skript.testing.enabled` and `skript.testing.dir` are set, `realScriptsPath` = that absolute path (so the repo’s **src/test/skript/tests**).
  - `getScriptsDirectory()` copies from `realScriptsPath` into `config/skript/scripts-preprocessed` under the Fabric run dir and returns that work dir.
- So the **source of truth** for test scripts is **src/test/skript/tests/** in the repo. The Fabric test run uses those files at that location (via the absolute path in `skript.testing.dir`). No need to copy tests into the Fabric server tree; the runner passes the path and Fabric reads from it.

**Summary:** Script files are used from:

- **Production:** `config/skript/scripts` (then preprocessed into `config/skript/scripts-preprocessed`).
- **Tests:** Repo path **src/test/skript/tests** (passed as absolute path in `skript.testing.dir`).

So yes, we are using the Skript files at the right locations.

---

## 2. Project hierarchy (file layout and roles)

```
skript-fabric/                    (repo root; same as main Skript layout + Fabric)
├── build.gradle                  (root build; defines test tasks, nightlyRelease, etc.)
├── settings.gradle
├── src/main/java/                (Bukkit/root Skript + test runner)
│   └── ch/njol/skript/           (Skript main, test platform, etc.)
├── src/test/skript/
│   ├── tests/                    (all .sk test scripts — used when tests run)
│   ├── environments/             (e.g. fabric-1.21.11.json)
│   └── ...
├── skript-core/                  (upstream core — do not edit for Fabric)
│   └── src/main/java/ch/njol/skript/core|platform/
├── skript-bukkit/                (Bukkit/Paper implementation)
├── skript-fabric-core/           (Fabric “core”: copy of core + Fabric-only code)
│   ├── src/main/java/
│   │   ├── ch/njol/skript/
│   │   │   ├── core/             (copy of skript-core; edit here for Fabric)
│   │   │   ├── platform/         (copy of skript-core platform)
│   │   │   └── fabric/core/      (FabricScriptsDirectory, FabricScriptPreprocessor)
│   │   └── ...
│   ├── build.gradle              (no dependency on skript-core)
│   └── SYNC_FROM_CORE.md         (how to refresh copy from upstream)
└── skript-fabric/                (Fabric mod; depends only on skript-fabric-core)
    └── src/main/java/ch/njol/skript/fabric/
```

- **skript-core:** Upstream; used by Bukkit/root. For Fabric we don’t depend on it; we use the copy inside fabric-core.
- **skript-fabric-core:** Contains the full copy of `ch.njol.skript.core` and `ch.njol.skript.platform` plus `ch.njol.skript.fabric.core`. All Fabric-specific “core” changes (e.g. `getLoadedScripts()`, `registerConditionFirst`, current script in context) live in this copy. See **SYNC_FROM_CORE.md** for syncing that copy from upstream.
- **skript-fabric:** The Fabric mod; implementation only in `ch.njol.skript.fabric.*`, depends only on `skript-fabric-core`.

So the hierarchy is correct: one place for “core” used by Fabric (fabric-core), and one place for test scripts (src/test/skript/tests).

---

## 3. Syncing to a repo: fork vs something else

You have a lot of commits and want to sync to a remote. Two main options:

### Option A: Fork the main Skript repo (recommended)

- **Create a fork** of the official Skript repo (e.g. on GitHub/GitLab).
- **Push your branch** (with fabric-core copy, skript-fabric, test runner using `skript.testing.dir`, etc.) to your fork.
- **Upstream sync:** When main Skript gets updates, pull (or merge) from `upstream` into your fork. That updates:
  - `skript-core/`
  - `skript-bukkit/`
  - root `src/main/java/`
  - etc.
- **Fabric “core” copy:** Those updates do **not** change `skript-fabric-core`’s copy of core. So after each upstream pull you:
  1. Re-copy core into fabric-core (see **skript-fabric-core/SYNC_FROM_CORE.md**).
  2. Re-apply the Fabric-specific edits listed there (or keep them as a patch and re-apply).

**Pros:** One repo, one clone, clear “we’re Skript + Fabric.”  
**Cons:** You maintain the copy in fabric-core and re-apply a small set of patches after each upstream sync.

### Option B: Separate repo (e.g. “skript-fabric” only)

- **One repo** that only has Fabric-related code: e.g. `skript-fabric/`, `skript-fabric-core/` (with the copy of core inside it), and maybe a minimal test runner + test scripts.
- **Skript as dependency:** Treat main Skript as an external dependency (e.g. Git submodule pointing at the main repo, or a published artifact). Your build would depend on that for anything that still needs “vanilla” core (e.g. if you had a hybrid setup). With the current “full copy in fabric-core” setup, you don’t need skript-core at build time for Fabric; you only need to periodically refresh the copy inside fabric-core from the main Skript repo (e.g. pull main Skript in a submodule or separate clone, then copy into fabric-core and re-apply patches).

**Pros:** Repo is Fabric-only; no mixing with Bukkit/root.  
**Cons:** Syncing upstream = updating the copy in fabric-core from another repo (submodule or manual copy); slightly more manual than a single fork.

**Recommendation:** **Fork the main Skript repo** and push your current branch there. Keep script locations and hierarchy as they are (they’re correct). When you pull from upstream, follow **SYNC_FROM_CORE.md** to refresh the fabric-core copy and re-apply the Fabric-only core edits.
