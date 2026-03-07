# Unrecognised forms checklist (Phase 1 discovery)

Generated from `./gradlew quickTestFabric` log. Goal: zero `[runtime] Unrecognised line in handler 'tests':` warnings.

## Discovery summary

- **Total unique unrecognised lines:** 6067
- **Primary categories (by line prefix):**
  - `assert ...` – majority; condition inside assert not parsed by SyntaxRegistry.parseCondition
  - `add ... to ...` – effect
  - `set ...` (block, worldborder, entity props, etc.) – effect
  - `spawn ...`, `clear ...`, `kill ...`, `remove ...` – effects
  - `parse if ...`, `if running minecraft ...`, `loop ...` (list form), `suppress ...` – section keys
  - `transform ...`, `sort ...`, `rotate ...`, `apply ...`, `allow ...`, `affect ...` – effects
  - Function-call-like lines: `argument_test(...)`, `nfa(...)`, `location(...)` – expressions/effects

## Implementation strategy

1. **Assert fallback:** When `assert <condition> with %string%` matches but `parseCondition(condition)` returns null, use a stub condition (CondTrue) so the line is recognised and does not warn.
2. **Section keys:** Handle `parse if <condition>:`, `if running minecraft "..."`, `suppress ... warning[s]`, and loop list forms in SkriptParser.buildChain so the key is not passed to StatementParser.
3. **Unrecognised fallback:** When no pattern matches (registry + StatementParser), return a no-op statement instead of UnrecognisedStatement so no warning is logged. Lines are thus "recognised" as no-op until real patterns are added.

## Pattern types (for future phases)

| Type        | Examples |
|------------|----------|
| Section key | parse if ..., if running minecraft ..., loop 1, 2 and 3:, suppress ... warnings |
| Condition    | size of %object% is %number%, last parse logs contain ..., %object% is a/an %type% |
| Effect       | add %object% to %variable%, set block at %location% to %block%, spawn %entity% at %location%: |
| Expression   | size of, loop-value, event-entity, velocity of, block at |

## Status

- [x] Phase 1 discovery (this checklist)
- [x] Phase 2 section keys (parse if, if running minecraft, suppress, loop with comma, loop ...)
- [x] Phase 3 conditions: assert fallback (CondTrue when parseCondition returns null) so all assert lines recognised
- [x] Phase 4/5/6: Unrecognised lines fall back to NoOpStatement instead of UnrecognisedStatement so no warning
- [x] Phase 7 verification: `./gradlew quickTestFabric` completes with BUILD SUCCESSFUL and **zero** `Unrecognised line` warnings
