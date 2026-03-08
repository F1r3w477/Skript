# Fabric test failure groups

This document groups the current Fabric test failures by category and marks whether each can be fixed Fabric-only or needs core extension points. Generated from a full `./gradlew quickTestFabric` run.

**Note on exit code:** When tests fail, PlatformMain calls `System.exit(failCount)`. So exit code 139 means 139 tests failed, not a process crash (SIGSEGV). Gradle may show "139 (this value may indicate ... SIGSEGV)" but the run is stable: Fabric writes `test_results.json` and halts with 0; the runner then exits with the failure count.

---

## 1. Load / reload / script lifecycle

| Test | Message | Fix scope |
|------|---------|-----------|
| enable script | load event didn't run | **Blocked on core:** Fabric fires "script load" after reload; core gives each event handler a new VariableScope, so variables set in "on script load" (e.g. {EffScriptFile}) are not visible to the caller. |
| reload script | load event didn't run | Same scope issue as enable script. |
| unload + load script | load ran when already loaded | Fabric: script lifecycle + core scope. |
| other script is loaded | enabled script was not loaded | Fabric or core: script list/load API. |

**Summary:** Fabric implements "load script named", "reload script", and fires "script load" after reload. Tests still fail because SkriptRuntime.executeHandler() creates a new VariableScope per handler, so "on script load" handlers cannot set variables visible to the test. **Blocked on core** (would need shared/global scope or passing caller scope into fireEvent).

---

## 2. Script / config API

| Test | Message | Fix scope |
|------|---------|-----------|
| all scripts | not enough tests scripts found | Fabric or core: script list API |
| script by name | wrong number of scripts found | Fabric or core: script lookup by name |
| script name (new) | script name is wrong | Fabric or core: script name expression |
| scripts in directory | not enough tests scripts found | Fabric or core: scripts-in-dir |
| script config | wrong number of lines in this section | Fabric or core: script config nodes |
| skript config | too few nodes in config! | Fabric or core: global config API |
| config name (new) | config node path was wrong | Fabric or core: config node path |
| node name (new) | config node path was wrong | Fabric or core: node name |
| node of | first node name was wrong | Fabric or core: node-of expression |
| current script | script stringify is wrong | Fabric or core: current script expression |
| disabled script object | couldn't enable script from handle | Fabric or core: script handle enable |
| function name (new) | function name is wrong | Fabric or core: function name in registry |
| get all functions | found wrong number of functions | Fabric or core: function registry list |

**Summary:** **Blocked on core.** No platform methods for script list, script by name, script config, config nodes, or function registry. Core loads scripts from getScriptsDirectory() and does not expose these APIs.

---

## 3. Functions (round, floor, clamp, concat, factorial, etc.)

| Test | Message | Fix scope |
|------|---------|-----------|
| result of functions | function didn't return correctly | Core: function call expression + registry |
| result of external functions | function didn't return correctly | Core: external function support |
| rounding function | second assertion round failed | Core or Fabric: round() expression |
| floor function | floor(...) should be ... | Core or Fabric: floor() expression |
| clamp numbers (single) | (single) expected | Core or Fabric: clamp() expression |
| concat() function | string + number concat() failed | Core or Fabric: concat() |
| factorial function | factorial of 500 failed | Core or Fabric: factorial |
| 4988 function uuid multiple parameters | function parameters were set (3) | Core: function params |
| functions behave wrong with all default args | only one arg did not use the second default value | Core: default args |
| invalid function parameter type | invalid function parameter type failed | Core: parse-time validation |
| returns (parsing) | skript shouldn't be able to return multiple values... | Core: return parsing |

**Summary:** **Blocked on core.** No function-call expression or function registry in core. Fixing round, floor, clamp, concat, factorial, etc. would require core to support function-call expressions and a function registry.

---

## 4. Conditional / parse / do-if (core runtime)

| Test | Message | Fix scope |
|------|---------|-----------|
| SecConditional - if any else | if any did not run else when no condition was true | Fabric: trigger/context in Fabric env |
| SecConditional - if all else | if all did not run else when at least one condition was false | Fabric: same |
| do if | Do if didn't run when it should have | Fabric: do-if execution |
| parsing section | Parse section didn't return expected error | Fabric or core: parse section semantics |
| expression sections | expression section in expression section didn't run | Fabric or core: nested expr sections |
| expression sections that don't work | exprsec claimed over secConditional | Core: section precedence |

**Summary:** PARITY.md notes these pass in JVM unit tests but fail in Fabric. Conditional trigger correctly chooses else branch when no condition is true, but the subsequent assertion (e.g. `assert {_a} is false`) fails because the condition's right-hand side evaluates to null in Fabric (CondCompare sees b=null). Likely core parsing/expression path when scripts are loaded from config tree; needs further investigation. Parsing/error expectations may need core.

---

## 5. Expressions (various)

| Test | Message | Fix scope |
|------|---------|-----------|
| ExprDefaultValue | default value returning invalid type should not parse | Core: default value expression |
| ExprDifference | (6) expected '5', but got ... | Core or Fabric: difference expression |
| ExprTernary | ternary returning invalid type parsed | Core: ternary expression |
| ExprParse return type array | parsed as pattern expression did not work correctly | Core: parse expression |
| amount of objects | size of list was wrong - 2 | Core or Fabric: amount/objects |
| broadcast evaluates vstrings twice | Called func() more than once | Core: broadcast eval order |
| whether | Condition didn't evaluate correctly | Core or Fabric: whether condition |
| time since | Time since none failed (expected <none>, got ...) | Fabric: time-since expression |
| time until | Time until none failed (expected <none>, got ...) | Fabric: time-until expression |
| percent of | 50% of infinity is infinity (list) | Core or Fabric: percent expression |
| index of | all positions of string is set | Core or Fabric: index-of |
| times | thrice count was ... instead of 3 | Core or Fabric: times expression |
| custom operator priority | custom operator for testing priority expected ... | Core: custom operator precedence |
| double quote parsing | simple string with expression failed | Core: string parsing |
| comments | skript should not be able to handle hashtags in an expression... | Core: comment in variable name |
| pretty quote usage | Pretty quote %string% usage did not produce expected error | Core: pretty quote parsing |
| string literals (parsing) | the non-literal should not have been accepted | Core: literal parsing |
| expression list parsing | number and list parsing succeeded | Core: list parsing |
| midpoint type error | Midpoint between location and vector should error | Core: type error |
| damage source outside section error | Setting attribute of damage source outside of ExprSec should error | Core: error reporting |

**Summary:** Mix of core expression/parser work and Fabric-only (e.g. time since/until). Mark each test in this list as Fabric vs core when implementing.

---

## 6. Loops / sections / variables

| Test | Message | Fix scope |
|------|---------|-----------|
| for section | Expected value = 10, found ... | Core or Fabric: for-loop iteration |
| while section | do while loop ... did not run 5 times | Core or Fabric: while section |
| loop-iteration | loop-iteration-3 not equal to 12 (variables) | Core: loop variable |
| looping list of single variables | Incorrect amount of iterations | Core: loop over list |
| previous and next loop value | Looping nothing should not be more than 0 | Core: loop value |
| for each loops ending (result) | Wrong number of variables | Core: for-each variables |
| for each loops ending (start) | Wrong number of variables | Core: for-each variables |
| concurrent do while loops | Didn't run multiple do-while loops concurrently | Core: concurrent loops |
| continue effect | continue in nested loop continued outermost loop | Core: continue target |
| spawn section | value of {_new var} should be 5 | Core or Fabric: section local vars |
| local vars created in EffSecSpawn | local var ... was not properly incremented | Core: EffSecSpawn locals |
| list copy / single copy / list sizes | Didn't copy / default list size failed | Core: list copy/size |
| set list to keyed function / keyed list | should have 3 items | Core: keyed list |
| keyed set mode | something wasn't carried properly | Core: keyed set |
| removing from variables skips duplicates | didn't remove all elements | Core: variable remove |
| toggle effect | should not be toggled | Core: toggle effect |
| transform effect | failed to transform using index | Core: transform effect |
| sorted indices with children | modified children wrongly | Core: sorted indices |
| sorting | incomparable mixed list was adjusted | Core: sort semantics |

**Summary:** **Blocked on core.** Loop semantics, variable/list/toggle/transform, section locals are implemented in core; no platform API to override. No Fabric-only fix.

---

## 7. Vectors / math

| Test | Message | Fix scope |
|------|---------|-----------|
| vector between locations | vector between same locations failed | Fabric: vector-between expression |
| vector from xyz | random vector to created component equality failed | Fabric: vector from x/y/z |
| vector xyz | same | Fabric: xyz component expressions |
| vector from expressions conflict | Vector from not generating correct vector | Fabric: vector from |
| vector rotate around vector | rotate around z vector failed | Fabric: rotate expression |
| rotate around local axis | rotate vector around local z axis unexpectedly modified vector | Fabric: rotate local |
| cross product | dot product of zero vector failed | Fabric: cross/dot product |
| normalize zero vector | set zero vector's length failed | Fabric: normalize |
| blocks vector direction | Blocks vector(1,0,0) loc is not 100 | Fabric: blocks expression |
| blocks void | Blocks between loc and ... is not 11 | Fabric: blocks |
| location vector offset | no offset equality check failed | Fabric: location offset |
| axis angle | error "invalid axis angle was set" | Fabric: axis angle |
| bases | Failed conversion to/from %{_base}% | Core or Fabric: base conversion |
| long overflow, addition/multiplication, long underflow | long value did not overflow... / wrong final value | Core: long overflow semantics |
| arithmetic parse time conversion | failed to calculate experience + number | Core or Fabric: experience + number |
| formatted time | custom date format failed | Fabric: formatted time |

**Summary:** Fabric implements: `FabricVector`, `set %variable% to vector between %object% and %object%`, `set %variable% to [a new] vector from %object%, %object% [and] %object%`, and condition `%-object% is vector(%-number%, %-number%, %-number%)`. Assertions still fail because core's `%object% is %object%` matches first (no `registerConditionFirst` in core), so right-hand side is parsed as string. Long overflow and some conversions may need core.

---

## 8. Bukkit / Minecraft-specific (entities, items, world, etc.)

| Test | Message | Fix scope |
|------|---------|-----------|
| command event | testing1 or testing2 command event triggered the wrong amount of times | Fabric: command name overwrite (core COMMAND_HEADER single token) |
| spawn dropped item | failed to spawn dropped stone | Fabric: spawn dropped item effect |
| zombify villager | 5 villagers did not spawn | Fabric: zombify effect |
| whitelist | Failed to empty whitelist | Fabric: whitelist expression |
| world environment | environment of world didn't compare with a variable | Fabric: world environment |
| uuid | Player does not match uuid of player | Fabric: uuid expression |
| supported events | supported events message did not get sent correctly | Fabric: supported events |
| EffSecShoot | 1 or more projectiles did not spawn | Fabric: shoot effect |
| EffHealth item mutation fix | {_item2} was no longer a diamond | Core or Fabric: item mutation |
| StriderData - Strider Entity Data | The strider spawned using 'shivering strider' wasn't shivering | Fabric: strider shivering |
| entity invulnerability | failed to make pig invulnerable | Fabric: invulnerable |
| entity silence | failed to make pig silent | Fabric: silence |
| charge creeper nearest entity cast | a creeper should be charged... | Fabric: charged creeper |
| is charged | charging a wither skull should do exactly that | Fabric: wither skull charged |
| is conditional | making a command block unconditional... | Fabric: command block conditional |
| is enchanted | knockback 19 or lesser... | Fabric: enchant check |
| is lootable | is lootable chest failed | Fabric: lootable |
| inventory holder location | holder location differs from block location | Fabric: inventory holder |
| items in (inventory) | size of second items in failed | Fabric: items in inventory |
| item comparisons | {_inventory} still has diamonds... | Fabric: item comparison |
| replace items | replace non-existent item changed inventory | Fabric: replace items |
| leaves persistence | Failed to make leaves persist | Fabric: leaves persist |
| composter the imposter | failed to compare composter (itemtype) with a block | Fabric: composter |
| banner item | Base banner pattern type should not get an item | Fabric: banner |
| potion ambient / infinite / effect creation | potion is not ambient / infinite | Fabric: potion expressions |
| eternity | potion effect was not infinite | Fabric: eternity |
| registry | variable should have been set to sharpness enchantment | Fabric: enchantment registry |
| unbreakable | Iron Sword should be unbreakable ##2 | Fabric: unbreakable |
| loop all itemtypes | There should be at LEAST 1000 ItemTypes | Fabric: item type registry/count |
| literal specification breaks command arguments | Literal type specification broke command arguments | Core: literal in command |
| offline player function no lookup | Looked up offline player when told not to | Fabric: offline player |
| characters between | null random characters returned non-null value | Core or Fabric: random characters |
| any aliases random | The aliases were the same 50 times in a row | Fabric: alias randomness |
| filter | failed to filter on any of mod(), >0 | Core or Fabric: filter |
| except entities / except items | Function call... / Failed to exclude all | Core or Fabric: except |
| loot items | loot items set | Fabric: loot items |
| regex exceptions not handled | regex split returned a value with invalid regex... | Core: regex error handling |
| replace strings | regex replace with invalid pattern succeeded | Core: replace strings |
| dequeue queue / queue emptiness / queue polling / queue start/end | queue was not empty | Fabric: queue expressions |

**Summary:** Most are Fabric-only (Fabric equivalents for spawn, entity, item, world, potion, registry, queue, etc.). A few (literal in command, regex, filter, except) may need core.

---

## 9. Other

| Test | Message | Fix scope |
|------|---------|-----------|
| damage source outside section error | Setting attribute of damage source outside of ExprSec should error | **Blocked on core:** Core error reporting/validation. |

**Summary:** **Blocked on core.** No Fabric-only fix.

---

## Summary by fix scope

- **Fabric-only:** Many vectors/math, most Bukkit-specific (spawn, entity, item, potion, registry, queue, whitelist, world env, uuid, supported events, etc.), and conditional/do-if/parse debugging in Fabric.
- **Core or shared:** Expression parsing (comments, pretty quote, string literals, type errors), long overflow, custom operator priority.
- **Blocked on core (no Fabric hook):** Load/reload lifecycle (core gives each event handler a new VariableScope, so "on script load" vars not visible to caller); script/config/function API; full function-call syntax and function registry; loops/sections/variables (no platform hook); damage source outside section error; command name with space (command event "testing1 argument").

Use this file to prioritize Fabric-only work and to track which tests need core extension points.
