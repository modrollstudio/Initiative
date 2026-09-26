# Pre-release hardening audit

Scope: the bug classes that only surface on a real server with real players — leaked or unbounded
state, crash vectors, malformed input, thread safety and per-tick cost. Initiative is
feature-complete at M9; nothing here proposes new behaviour.

Audited at `beb6357` (branch `audit/pre-release`), NeoForge 1.21.1 / Fabric, Critfall 0.2.6.
Baseline `./gradlew check` was green before any change.

Severity: **Critical** = server or client dies / data corrupted, reachable by an ordinary player.
**High** = server or client dies, reachable by an admin's data or a third-party mod. **Medium** =
wrong or stuck state, or a footgun with no guard rail. **Low** = cosmetic, unreachable today, or a
recommendation.

---

## A. Leaked / unbounded state

Every collection holding entity- or encounter-derived state, and what evicts from it.

| Collection | Keyed by | Evicted by | Unbounded? |
|---|---|---|---|
| `EncounterManager.ACTIVE` (`EncounterManager.java:45`) | `ServerLevel` → `List<Encounter>` | list entry on `isOver` (`:117-121`); whole entry on `levelUnloaded` (`:138`), `serverStopping` (`:146`), and when the level's list empties (`:133`) | No |
| `Encounter.participants` / `joinedAtGameTime` (`Encounter.java:27-28`) | participant UUID | `Encounter.remove` (`:76`) from the single exit path `removeParticipant` (`EncounterManager.java:216`) and `releaseAll` (`:224`) | No |
| Critfall `CombatSuppression` (suppressed by `join`, `EncounterManager.java:170`) | UUID | `CombatSuppression.release` in `removeParticipant` (`:217`) and `releaseAll` (`:226`) | No |
| `AiFreeze.FROZEN` (`AiFreeze.java:26`) | UUID | `thaw` in `removeParticipant` (`:218`), `releaseAll` (`:227`), the per-tick reconcile `applyFreeze` (`:240-254`), and `clear()` on server stop (`:149`) | No |
| `EncounterFlags` — dodging, disengaged, help grants, reaction-used, hidden, `grappledBy` (`EncounterFlags.java:24-29`) | participant UUID | `EncounterFlags.remove` (`:119-128`), called from `Encounter.remove`; grapples are removed in **both** directions (`:126-127`) | No |
| `Encounter.lastActionUiSnapshots` (`Encounter.java:32`) | participant UUID | `Encounter.remove` (`:79`) | No |
| `Encounter.lastHudSnapshot`, `budget`, `budgetActor`, `budgetRound`, `movementAnchor` | per encounter | die with the `Encounter` | No |
| `TurnOrder.entries` (`TurnOrder.java:19`) | participant UUID | `TurnOrder.remove` (`:43`) from `Encounter.remove` | No |
| `ActionRegistry.ACTIONS` (`ActionRegistry.java:19`) | action id | never — registration-time only, one entry per registered action | No |
| `ActionSettings.settings` (`ActionSettings.java:16`) | action id | wholesale replace on every datapack reload (`:29-31`) | No |
| Client: `TurnOrderClientCache`, `ActionUiClientCache`, `RollAnimationClientCache`, `RollReadoutHold`, `ActionUiInput` pending target | single-slot statics | cleared on logout on both loaders (`InitiativeNeoForgeClient.java:69-74`, `InitiativeFabricClient.java:51-56`) | No |

**Result: no state leak found.** The M1 "one exit path" discipline holds — `removeParticipant`
(`EncounterManager.java:216-222`) is the only per-participant exit and it releases suppression, thaws
freeze, clears both client views and drops every per-encounter flag. `releaseAll` covers the
encounter-wide paths. Verified against each exit: **death** and **chunk unload** and **dimension
change** all land on `releaseInvalidMembers` (`:201-209`, entity null or not alive or out of leave
radius); **disconnect** is immediate via `playerDisconnected` (`:98-100`); **encounter end** is
`isOver` (`:117`); **server stop** is `serverStopping` (`:146-150`).

"No leak found" is now pinned rather than asserted. `EncounterFlagsTest`
`removeClearsEveryFlagInBothDirections` raises every flag the class can hold — including a grapple in
each direction — and `EncounterTest.removingAParticipantDropsEveryTraceOfIt` covers membership, join
time, turn-order slot, action-UI snapshot and flags together. Both were verified to bite by
temporarily deleting lines from `EncounterFlags.remove`; a field added without a matching removal
fails them. `EncounterScenarios.everyExitPathClearsSuppression` and
`MultiplayerScenarios.aDisconnectedPlayerLeavesNothingBehind` already cover the same ground in-world.

A permanently-frozen entity — the worst failure named in the brief — is structurally prevented:
`applyFreeze` (`:240`) *reconciles* rather than reacting to transitions, so any missed exit heals on
the next tick, and any participant that stops being ticked here was already thawed on its way out.
`FROZEN` is transient memory only, so a hard kill cannot leave a statue in the save.

### A1 — `AiFreeze.FROZEN` is a plain `HashSet` read from every mob's AI step — Medium
`AiFreeze.java:26`, read at `:57` from `MobAiStepMixin` on both loaders.

Under vanilla, entity ticking and `EncounterManager.tick` are both the server main thread, so this is
correct today. But this is the one Initiative field read by *every mob on the server, every tick*, and
it has no memory-model guarantee: any mod that parallelises entity ticking gets an unsynchronised
read of a `HashSet` being mutated — at best a stale answer, at worst the classic `HashMap`
resize-during-read spin, which is a server hang, not a crash. The read is a set membership test;
making the set concurrent costs nothing measurable.

### A2 — `levelUnloaded` does not clear the clients' HUD and action bar — Low
`EncounterManager.java:138-144` and `:109`.

`releaseAll` releases suppression and thaws freeze but, unlike the `isOver` path (`:118-119`), never
calls `TurnOrderSync.encounterEnding` / `ActionUiSync.encounterEnding`. Players in the encounter keep
a stale turn strip and a stale action bar until the next encounter.

Not reachable in practice: vanilla never unloads a level with players in it while the server runs, and
the other caller — `tick`'s `!config.enabled()` branch (`:108-110`) — cannot fire in production
because `InitiativeConfig` is loaded once at mod construction with no runtime reload. Documented
rather than fixed, because the fix adds code to a path that cannot currently execute. **If a config
reload command is ever added, this becomes a real bug and must be fixed with it.**

---

## B. Crash vectors / robustness

### B1 — A malformed action-settings datapack crashes the server tick — High
`ActionSettings.java:24-27`, reached from `ActionSettingsLoader.java:26-36`.

`ActionSettings.enabled` calls `GsonHelper.getAsBoolean(json, "enabled", fallback)`, which throws
`JsonSyntaxException` when the key is present but is not a JSON primitive — `"enabled": null` and
`"enabled": {}` both throw. The loader (`:28-34`) validates only that the file's *root* is an object;
it never looks at the fields.

This is the documented way a third-party action reads its toggle (`docs/api.md`:
`.enabled(() -> ActionSettings.enabled(TAUNT, true))`), and `Action.enabled()` is queried **per tick**
from `ActionUiSnapshots.build` (`ActionUiSnapshots.java:40`) inside `EncounterManager.tick`. The
exception propagates out of the level-tick listener on both loaders and takes the server down. It is
also reachable synchronously from a client's action-invoke packet via `ActionGate.availability`
(`ActionGate.java:63`).

Precondition: a mod that registers an action this way must be installed. Once one is, the crash needs
nothing but a typo in a datapack — which is exactly the case the brief says must never happen.

### B2 — A number too large for a double becomes an infinite radius or budget — Medium
Every `fromJson` range check: `EncounterConfig.java:18` `triggerRadius <= 0`, `ActionConfig.java:38`
`movementBudget < 0`, `CoverConfig.java:36`, `GrappleConfig.java:42`, `TurnConfig.java:33`.

Every guard is a one-sided comparison, and `Double.POSITIVE_INFINITY` satisfies all of them.
`"trigger_radius": 1e400` — valid JSON, an extra row of zeroes — parses to `+Infinity` and is
accepted, giving `AABB.ofSize` at `EncounterManager.java:189` an infinite box to hand
`getEntitiesOfClass` every tick, per encounter. `"movement_budget_blocks": 1e400` makes
`movementExhausted()` never true, so a turn never ends on movement.

Measured, not assumed — bare `NaN` and `Infinity` **are** already rejected, but not by the range
checks: Gson's lenient parser reads those tokens as *strings*, so `GsonHelper.getAsDouble` refuses
them with "Expected trigger_radius to be a Double". Only the overflow path gets through. The guard
therefore has to be finiteness, applied before the range checks.

Separately, and reachable with perfectly ordinary finite numbers: `trigger_radius` has no upper
bound. `trigger_radius: 100000` is a plausible "combat everywhere" setting and makes the per-tick
`getEntitiesOfClass` scan the entire loaded world for every encounter. An upper bound is a balance
decision, so it is deferred rather than chosen here.

### B3 — Action-invoke packet does not validate its position — Medium
`ActionInvoke.java:30-37`, payload `ActionInvokePayload.java:25-35`.

The `Vec3` rides the wire as three raw doubles and is handed to the action untouched. `NaN` and
`Infinity` pass through. Nothing built in uses `ActionTargeting.Position` today, so no shipped action
is affected — but the packet boundary is exactly where CLAUDE.md says validation belongs, and a
third-party position action that teleports or spawns at the given point would corrupt an entity's
position from a crafted packet.

Everything else on this packet is sound. The action id is a `ResourceLocation` (malformed → decoder
rejection, which disconnects the sender, not the server); an unknown id is refused with `DISABLED`
(`ActionGate.java:31-34`); the target is resolved by network id within the sender's own level and then
re-checked for encounter membership, side and reach (`ActionGate.java:154-180`); the actor is always
the sending player, never a client-supplied id, so acting for someone else is not expressible; and
turn, cost and enablement are gated by the same `ActionGate.availability` the UI uses
(`ActionGate.java:59-85`), so acting out of turn or invoking a disabled action is refused.

### B4 — Enum stream codecs index a raw array — Low
`ActionUiSnapshot.java:79-84`.

`ByteBufCodecs.idMapper(index -> ActionCost.values()[index], …)` throws
`ArrayIndexOutOfBoundsException` on an out-of-range or negative index. These are server→client
payloads, so this needs a hostile or buggy server, and the failure is a decoder error on the client
connection rather than a process crash. Recorded, not fixed.

### B5 — The documented config example is stale and is rejected if used — Medium
`docs/examples/initiative.json` was missing 16 `actions` keys (`hide`, `opportunity_attack`, the
whole shove / blink / reel block, …) and `roll_animation.shared_visibility`. The inline example in
`docs/config.md` was worse: it was additionally missing the entire `cover` and `grapple` groups.

Config parsing is deliberately strict — a missing key throws (`TurnConfigTest.missingFieldThrows`
and siblings pin this) — so a user who copied either example got their *entire* config discarded and
replaced by built-in defaults, with only a log line (`InitiativeConfig.java:40`) to say so. The
per-key prose in `docs/config.md` was complete and correct throughout; only the two examples had
drifted.

### B6 — Strict config rejects an older version's file wholesale — Medium, deferred at audit time
`InitiativeConfig.java:31-43` + every `fromJson`. **Taken up after the audit**: config is now read
key by key against the built-in defaults, so this finding no longer holds. The description below is
the state at `beb6357`.

Because every key is required, an `initiative.json` written by an earlier Initiative release stops
applying entirely the first time a new key is added — as happened at M9 with
`turns.mob_turn_timeout_ticks` and `roll_animation.shared_visibility`. The user's settings revert to
defaults, the file is not migrated or rewritten, and the only signal is one log line.

This is a **deliberate, test-pinned design decision**, not an oversight, and the fix (per-key defaults
via `GsonHelper.getAsX(obj, key, default)`) would overturn it across eight records and delete a dozen
existing tests. That is the author's call, not the audit's. See `docs/deferred-issues.md`.

### B7 — Commands
`InitiativeCommands.java`. Arguments are Brigadier-typed (`EntityArgument.entity()`,
`Vec3Argument.vec3()`); a non-entity target is refused (`:76-83`); a non-player source is refused by
`getPlayerOrException` (`:89`). Selector use is already gated at permission level 2 by vanilla
`EntityArgument`. Commands go through the same `ActionRegistry.invoke` as the UI, so they cannot
bypass turn or encounter gating. **No finding.**

### B8 — `orElseThrow()` in action effects
`ActionEconomy.java:148, 255`, `NativeActions.java:75, 97, 118`, `Reactions.java:79`. Each is reached
only after `ActionGate` has proven the invariant it asserts — targeting rejection precedes the effect
(`ActionGate.java:43-47`) and a null encounter is refused before that (`:66-68`). **No finding**;
noted so a future reviewer does not have to re-derive it.

---

## C. Concurrency / server-thread safety

- **NeoForge C2S handler**: wrapped in `context.enqueueWork` (`InitiativeNeoForge.java:88-92`) — main
  thread. Correct.
- **Fabric C2S handler**: `ServerPlayNetworking.registerGlobalReceiver`
  (`InitiativeFabric.java:53-55`). Verified against the Fabric API source in the Gradle cache
  (`fabric-networking-api-v1-4.3.0`, `ServerPlayNetworking.java:46` "executes the callback in the
  server thread" and `:304` "called on the server thread"). Correct — no `server.execute` needed.
- **Tick, events, commands, reload listener**: all main thread on both loaders.
- **`InitiativeConfig` / `ActionSettings`**: `volatile` fields swapped wholesale, read-only
  afterwards. Correct.
- **`ActionRegistry.ACTIONS`**: `synchronized` on every access. Correct.
- **`AiFreeze.FROZEN`**: see A1 — the one gap.

The freeze mixin's lookup itself is as cheap as asked for: an `isEmpty()` short-circuit and one hash
probe (`AiFreeze.java:57`), with the input-clearing work done only for a mob that is actually frozen.

---

## D. Per-tick / per-attack cost

Measured as work per server tick, per level.

| Hot path | Cost | Verdict |
|---|---|---|
| Freeze mixin (`AiFreeze.holdIfFrozen`) | one `isEmpty()` + one hash probe per mob per tick | Fine |
| `pullHostilesInRadius` (`EncounterManager.java:188`) | one `getEntitiesOfClass` over a (2·`trigger_radius`)³ box per encounter per tick, with a predicate that streams every encounter in the level | **O(encounters² × entities-in-box)** — bounded at the default radius 12, unbounded with the radius (see B2) |
| `releaseInvalidMembers` | one `level.getEntity` per participant per tick | Fine |
| `applyFreeze` | one map op per participant per tick | Fine |
| `ActionUiSnapshots.build` | early-returns `INACTIVE` for every non-acting player (`ActionUiSnapshots.java:34`); for the one acting player, O(actions × participants) entity lookups | Fine — one player per encounter |
| Turn-order / action-UI sends | diffed against the last snapshot; the strip resends about once a second, the acting player's bar resends every tick they move ≥0.1 block (`ActionUiSnapshots.java:90`) | Fine — at most one player per encounter |
| Cover raycast (`Cover.java:45-61`) | 8 `level.clip` calls per driven attack, and per reactor on an opportunity attack | Fine — per action, not per tick |

Only `pullHostilesInRadius` is worth naming: it is the sole per-tick world query, its predicate does a
full encounter scan per candidate entity, and its cost is driven by a config value with no upper
bound. Nothing here is O(entities) per tick at default settings.

---

## E. Multiplayer / lifecycle edge cases

Traced, no finding on any of them — M9 closed this area:

- **Disconnect mid-turn** — `playerDisconnected` (`EncounterManager.java:98`) runs the full exit path
  immediately; `TurnOrder.remove` (`TurnOrder.java:43`) passes the turn on when the leaver held it.
  The index invariant `0 ≤ currentIndex < entries.size()` holds across every `insert`, `remove`,
  `endTurn` and `tick` branch, so `currentTurn()` cannot index out of bounds.
- **Reconnect** — the returning player is outside the encounter; the client's caches were cleared on
  logout, so there is no half-state to reconcile.
- **Logout while grappling or grappled** — `EncounterFlags.remove` drops the hold from both the
  `grappledBy` key side and the value side (`EncounterFlags.java:126-127`).
- **Logout while frozen** — cannot happen; players are never frozen (`EncounterManager.java:249`
  freezes `Mob` only).
- **Two encounters / an entity joining both** — `join` refuses an entity already in any encounter in
  the level (`EncounterManager.java:164-166`), and `onCombat` joins an existing encounter rather than
  merging (`:77-84`).
- **Chunk unload, dimension change mid-turn** — the entity stops resolving in the encounter's level,
  so `releaseInvalidMembers` evicts it on the next tick.
- **Server stop mid-encounter** — `serverStopping` releases everything and clears both statics.

---

## F. Critfall-absent / version robustness

Both loaders declare Critfall as a hard dependency, so a missing Critfall is a clean loader-level
refusal, not a `NoClassDefFoundError`:

- `neoforge.mods.toml`: `modId = "critfall"`, `type = "required"`, `versionRange = "[0.2.6,)"`,
  `ordering = "AFTER"`.
- `fabric.mod.json`: `"critfall": ">=0.2.6"`.

### F1 — No upper bound on the Critfall version range — Low
Both ranges are open-ended. A Critfall release that changes `studio.modroll.critfall.api` in a
breaking way would load happily and fail at the first call with `NoSuchMethodError` /
`NoClassDefFoundError` — the cryptic crash the dependency declaration exists to prevent. An upper
bound (`[0.2.6,0.3)` / `>=0.2.6 <0.3`) converts that into the clear loader message. Not changed here:
it constrains Modroll's own release policy across two mods, which is the maintainer's call.

### F2 — Critfall API gaps observed during this audit (informational)
- `CombatSuppression` is a plain UUID set with no ownership or refcount, so if two mods suppress the
  same entity, whichever releases first un-suppresses it for the other. Initiative is correct in
  isolation; the API cannot express shared suppression.

---

## Summary

Fixed here: Critical/High, plus the Medium findings whose fix was low-risk. Deferred with reasons in
`docs/deferred-issues.md`. Low findings are documented rather than changed, which is where C7 and
C11 sit — both are real, neither is reachable in a way that justifies touching the code paths now.

| # | Finding | Severity | Disposition |
|---|---|---|---|
| B1 | Malformed action-settings datapack crashes the server tick | High | **Fixed** + 2 tests |
| A1 | `AiFreeze.FROZEN` unsynchronised across the server-wide AI-step read | Medium | **Fixed** + test |
| B2 | Config number overflowing to infinity passes every range check | Medium | **Fixed** (finite guard) + 7 tests |
| B3 | Action-invoke packet position not validated | Medium | **Fixed** + 3 tests |
| B5 | Both documented config examples stale, rejected if used | Medium | **Fixed** (docs) |
| B6 | Strict config rejects an older version's file wholesale | Medium | Deferred at audit time; **fixed since** — per-key migration |
| — | `trigger_radius` has no upper bound | Medium | Deferred — the cap is a balance decision |
| A2 | `levelUnloaded` leaves a stale client HUD | Low | Documented — unreachable today |
| C7 | `EncounterManager.tick` iterates the live encounter list | Low | Documented |
| C11 | `BlockPlacement` bypasses the grapple movement rule | Low | Documented |
| B4 | Enum stream codecs index a raw array | Low | Documented |
| D | `pullHostilesInRadius` is the one per-tick world query | Low | Documented |
| C10 | Fishing rod damaged past its maximum, never breaks | Low | Documented |
| F1 | Critfall version range has no upper bound | Low | Documented — release-policy call |

No gameplay or balance value changed. The one behavioural change is a refusal that used to be a
crash (B1) and three inputs that used to be accepted and now are not (B2, B3).

Manual-test focus for a real modded server, in priority order:

1. **The freeze mixin.** A1 is fixed defensively, but `Mob.serverAiStep` is the busiest method
   Initiative touches. Test with a large mob-AI mod (Better Combat, an entity-tick parallelism mod)
   and confirm no mob is left immobile after an encounter ends.
2. **`pullHostilesInRadius` under load.** A raid or a mob farm inside `trigger_radius` with several
   simultaneous encounters is the shape that would expose the O(encounters²) predicate.
3. **A third-party action.** B1's crash path only exists once an API consumer is installed; the fix
   should be exercised against a real registered action with a deliberately broken settings file.
