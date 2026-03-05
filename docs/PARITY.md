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

## Commands

| Command       | Bukkit | Fabric |
|---------------|--------|--------|
| /skript reload| Yes    | Yes    |
| /skript test  | Yes    | Yes (when test mode enabled) |

## Notes

- Full Skript language (conditions, expressions, sections, etc.) runs only on the legacy Bukkit plugin today.
- Fabric uses the minimal core engine; more features will be migrated in future phases.
