# Deferred issues

Findings from the pre-release audit (`docs/audit-pre-release.md`) that were deliberately **not**
fixed. Each says why, and what fixing it would take.

## Taken up since the audit

- **B6** — a config file from an older Initiative version was rejected wholesale. Fixed: config is
  now read key by key against the built-in defaults, so a file missing keys a newer release added
  keeps everything it does specify. See [config](config.md#how-a-file-is-read).

## `trigger_radius` has no upper bound

**From:** audit B2 / D. **Severity:** Medium. **Where:** `EncounterConfig.java:18`,
`EncounterManager.java:188-199`.

`trigger_radius` is now required to be positive and finite, but nothing caps it.
`"trigger_radius": 100000` is a plausible "combat everywhere" setting and turns the per-tick
`getEntitiesOfClass` in `pullHostilesInRadius` into a scan of the whole loaded world, once per
encounter per tick.

**Why deferred:** picking the cap is a balance decision, and CLAUDE.md forbids inventing balance
numbers in Java. It belongs in the config as its own documented key, which makes it a small feature
rather than a hardening fix.

**If it is taken up:** add `encounters.max_trigger_radius` (or a hard documented ceiling) and reject
above it at parse time, with the reason in `docs/config.md`.

## Low findings, recorded only

Documented in `docs/audit-pre-release.md`, not fixed, per the audit's own rule that Low findings are
documented rather than changed:

- **A2** — `EncounterManager.levelUnloaded` does not clear the clients' turn strip and action bar.
  Unreachable today (vanilla never unloads a level with players in it, and there is no runtime config
  reload). **Must be fixed if a config-reload command is ever added.**
- **C7** — `EncounterManager.tick` iterates the live encounter list; a re-entrant `onCombat` during
  the tick would throw `ConcurrentModificationException`. No such re-entry exists today because
  Critfall's combat event does not fire for `RollService.performAttack` damage.
- **C11** — `BlockPlacement.onPlaceAttempt` builds the turn budget with
  `encounter.budgetFor(..., movementBudgetBlocks())` instead of the grapple-aware
  `ActionEconomy.budgetOf`. A grappled player whose first budget-touching act of the turn is placing
  a block gets a full movement budget instead of none. The window is one tick, because
  `ActionEconomy.tick` establishes the budget every tick.
- **B4** — `ActionUiSnapshot.Entry`'s enum stream codecs index `values()` directly, so a corrupt or
  hostile server→client packet throws `ArrayIndexOutOfBoundsException` in the decoder.
- **C10** — `NativeActions.useDurability` increments damage past the item's maximum, so a fishing
  rod used to Reel never breaks and can show an overfull durability bar.
- **F1** — the Critfall dependency range is open-ended on both loaders (`[0.2.6,)` / `>=0.2.6`), so a
  breaking Critfall release would load and fail at the first API call instead of being refused by the
  loader. Bounding it is a Modroll release-policy decision across two mods.
