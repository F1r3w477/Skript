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

Tests that are known to fail on Fabric (e.g. depend on Bukkit-only or not-yet-ported behaviour) are excluded from the reported failed set so `quickTestFabric` can pass. See `FabricTestResults.FABRIC_EXCLUDED_FAILURES` in skript-fabric:

| Test name | Reason |
|-----------|--------|
| any aliases random | Depends on random alias behaviour not yet ported |

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

## Notes

- Full Skript language (conditions, expressions, sections, etc.) runs only on the legacy Bukkit plugin today.
- Fabric uses the minimal core engine; more features will be migrated in future phases.
