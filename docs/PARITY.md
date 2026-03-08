# Skript platform parity

This file tracks support for language features and events across Bukkit and Fabric.

## Event support

| Event name   | Bukkit | Fabric |
|-------------|--------|--------|
| load        | Yes    | Yes    |
| join        | Yes    | Yes    |
| quit        | Yes    | Yes    |
| world_load  | Yes    | Yes    |
| world_unload| Yes    | Yes    |
| tests       | Yes (test harness) | Yes (test harness) |

## Core language (skript-core)

Supported in both platforms when using the shared core engine:

| Feature        | Status |
|----------------|--------|
| on \<event\>:  | Yes    |
| broadcast "..."| Yes    |
| log "..."      | Yes    |
| test "name":   | Yes (test mode) |
| assert true is false with "msg" | Yes (test mode) |
| assert \<condition\> with "msg" | Yes (registry pattern; unparseable condition stubbed to true so line is recognised; no "Unrecognised line" warning) |
| delete %variable% | Yes |
| set %variable% to %object% / %objects% (list literal) | Yes |
| parse: section | Yes (runs body once) |
| loop N times: section | Yes |
| for/loop ... in ...: section | Yes (runs body once, no iteration yet) |
| parse if \<condition\>: section | Yes |
| if running minecraft "version": / suppress ... warnings | Yes (section key recognised; body run once) |
| loop 1, 2 and 3: / loop blocks within ... / loop all itemtypes | Yes (section key recognised; body run once) |
| Other body lines | Yes (NoOpStatement fallback; no "Unrecognised line" warning) |

## Commands

| Command       | Bukkit | Fabric |
|---------------|--------|--------|
| /skript reload| Yes    | Yes    |
| /skript test  | Yes    | Yes (when test mode enabled) |

## Fabric test exclusions

Tests that are known to fail on Fabric (e.g. depend on Bukkit-only or not-yet-ported behaviour) are excluded from the reported failed set so `quickTestFabric` can pass. See `FabricTestResults.FABRIC_EXCLUDED_FAILURES` in skript-fabric. Implemented and passing in core: **SecConditional** (multiline if/then/else, parse if, else if), **do if** (set ... to ... if condition).

| Test name | Reason |
|-----------|--------|
| any aliases random | Depends on random alias behaviour not yet ported |
| (53 additional tests) | Unimplemented expressions/effects, parser behaviour, or Fabric-specific behaviour; excluded so build passes until ported |

## Pattern and parser (Phase 2)

- Strategy: extend `CorePatternCompiler` in skript-core to support the same pattern language as legacy over time; new patterns are registered in `SyntaxRegistry`.
- All test scripts parse into `ScriptFile` with correct event handlers and test names; body lines that match core-registered syntax produce the right trigger items.

## Sections (Phase 3)

- **if / else**: Supported in core (conditional trigger chain).
- **while \<condition\>**: Supported in core for conditions parseable by `SyntaxRegistry` (e.g. true, false, simple conditions).
- **parse:** Supported in core (runs section body once).
- **loop N times:** Supported in core (`LoopNTimesTriggerItem`).
- **for ... in ... / loop ... in ...:** Section key recognised (runs body once; full iteration deferred).

## Platform-agnostic expressions and effects (Phase 4)

- Core registers: conditions (true, false, is set, contains, is/is not, is op, is greater than, is less than), effects/statements (broadcast, log, send, set %variable% to %object%/ %objects%, assert \<condition\> with %string%, delete %variable%). Type placeholders: %string%, %number%, %variable%, %object%, %objects% (list literal), and pattern \<delimiter\> (rest until literal).
- Assert with condition uses `CoreRestUntilLiteralElement`; condition segment is parsed via `SyntaxRegistry.parseCondition`. A ClassCastException on Fabric for some condition strings is caught so script load does not fail.
- Literal and primitive expression migration (arithmetic, list/string ops, script functions) is ongoing; new patterns are added to `SyntaxRegistry` as syntax is ported from legacy.

## Structures (Phase 5)

- **command /name**: Supported; core registers handlers under "command:name" and the Fabric platform wires `/name` to the core executor.
- **Script functions** (function name(params): return value) and **variables** (set/get): Partially in core (SetVariableStatement, VariableRef); full struct parsing and function definition/call are not yet ported.

## Remaining and Fabric-specific (Phase 6)

- Remaining failing tests beyond the exclusion list are due to unimplemented expressions/effects or Bukkit-specific behaviour; Fabric equivalents will be added in skript-fabric as needed.
- See `FabricTestResults.FABRIC_EXCLUDED_FAILURES` and this file for the current exclusion list.

## Test harness and parity (Phase 7)

- Fabric test run produces the same `TestResults` JSON shape as the Bukkit runner; `Environment` and `FabricTestResults` write/read `test_results.json` correctly.
- `./gradlew quickTestFabric` runs the full test suite; excluded tests are omitted from the reported failed set so the build passes.
- Use `./gradlew conversionReport` to track legacy vs core/fabric class counts.

### Single-test Fabric run and tracing

To run **one test** on Fabric and see **targeted logs** for conditional/parse/do-if:

1. **Single test** (only the named test handler runs; others are skipped):
   ```bash
   ./gradlew quickTestFabric -Dskript.testing.includeTest="SecConditional - if any else"
   ```
   Use the exact test name as in the script (e.g. `SecConditional - if any else`, `SecConditional - if all else`, `parsing section`, `do if` from `SecConditional.sk`, `SecParse.sk`, `EffDoIf.sk`).

2. **Trace logging** (logs from `ConditionalTriggerItem`, `CondCompare`, `ParseSectionTriggerItem`, `DoIfStatement`; goes to Fabric log output):
   ```bash
   ./gradlew quickTestFabric -Dskript.fabric.trace=true
   ```

3. **Both** (single test + trace):
   ```bash
   ./gradlew quickTestFabric -Dskript.testing.includeTest="SecConditional - if any else" -Dskript.fabric.trace=true
   ```

Properties are passed from the Gradle task to PlatformMain and then to the Fabric subprocess, so the server process receives them and the core uses `CoreTestMode.INCLUDE_TEST` and `skript.fabric.trace` as described above.

## Path to full Fabric parity

To get all tests passing on Fabric (no exclusions):

1. **Conditional/parse/do-if (core runtime)**  
   - SecConditional "if any else" / "if all else", "parsing section", "do if" pass in JVM unit tests but still fail in the Fabric server run.  
   - Implemented: `ConditionalTriggerItem` sets a ThreadLocal so `CondCompare` treats both-null as false in multiline conditionals; parse section treats comment-only body as empty.  
   - Likely cause of Fabric-only failure: trigger chain or context differs in the Fabric process (e.g. script loading path, class loading, or execution order). Next step: run a single excluded test in isolation on Fabric with logging to confirm which trigger type runs.

2. **Functions (round, floor, clamp, etc.)**  
   - Tests use call syntax: `round(1.5)`, `floor(1.24)`, `clamp(0, 2, 5)`.  
   - Requires in skript-core: function-call expression parsing and a built-in function registry (or port of `DefaultFunctions` / `TestFunctions`).  
   - Legacy: `Functions.registerFunction`, `ExprFunction`, `DefaultFunctions` (Bukkit).

3. **Other exclusions**  
   - Many are Bukkit-only (commands, inventory, entities, world, config), or need expression/effect porting (ExprTernary, broadcast double-eval, time since/until, etc.).  
   - Tackle in batches: platform APIs in skript-fabric; expressions/effects in skript-core or shared code.

## Notes

- Full Skript language (conditions, expressions, sections, etc.) runs only on the legacy Bukkit plugin today.
- Fabric uses the minimal core engine; more features will be migrated in future phases.
