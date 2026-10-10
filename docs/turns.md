# Initiative & turn order

Server-side only; clients receive nothing but particles yet. All values come from
[config](config.md); the whole system is behind the `turns.enabled` flag — off restores plain M1
encounter bubbles.

## Initiative roll

When a participant joins an encounter (at formation or as a late joiner) it rolls initiative
through Critfall's `RollService`:

```
initiative = 1d20 + bonus
bonus      = floor(movementSpeed × initiative_bonus_per_speed), clamped to [0, initiative_max_bonus]
```

`movementSpeed` is the entity's movement-speed attribute, mirroring Critfall's derivation
philosophy (plausible tabletop stats from vanilla attributes). At the default scale of 20 a
player (speed 0.1) gets +2 and a zombie (speed 0.23) gets +4. With
[Checks installed](actions.md#with-checks-installed) and `checks.enabled` on, the bonus is the
participant's Dexterity modifier instead, which can be negative. The result is stored on the
encounter as an initiative entry (participant, total, bonus).

## Turn order and tie-breaking

Participants are sorted by initiative total, descending. Ties break by:

1. higher bonus first (the faster combatant wins the tie),
2. then join order — the earlier joiner acts first.

A late joiner is inserted at its sorted position. While the order is still forming (participants
joining in the trigger tick, before the machine has advanced), the first turn simply belongs to
the highest roll so far. Once turns are running, the acting participant keeps its turn; a late
joiner whose slot in the current round has already passed first acts next round.

## Turn state machine

Each encounter runs one state machine: round 1 starts with the highest initiative, `endTurn`
advances to the next participant, and a full pass over the order starts the next round. Turns also
end on their own after a timeout, and **the timeout depends on who is acting**:

- a **player** gets `turn_timeout_ticks` (default 600 = 30 seconds). Since M8 the turn ends by
  clicking End Turn, so the clock only ever catches somebody who is not deciding at all: a decisive
  player never sees it run out, and an indecisive one gets room to think;
- a **mob** gets `mob_turn_timeout_ticks` (default 120 = 6 seconds). A mob that has an action to take
  spends it and ends its turn immediately (`mob_end_turn_when_spent`), so this timeout only ever
  covers a mob that *cannot* act — nothing in reach, no path — and there is no reason to make the
  table wait a player's length for that.

The countdown the clients display is read off the same rule, so the timer that fires and the timer
on screen are always the same number. Removing a participant (death, fleeing, unload — the M1 exit
paths):

- before the acting participant: the order shrinks, the turn is unaffected;
- the acting participant itself: its turn ends immediately and the next participant acts,
  wrapping into the next round when it was last in the order;
- after the acting participant: the order shrinks, the turn is unaffected.

A **player who disconnects** is removed by the same path, immediately rather than a tick later, so
their turn passes on and nothing of theirs is left behind — see [multiplayer](multiplayer.md).

Encounter end (M1 rules) tears the turn state down with the encounter.

## AI freeze

While `freeze_enabled` is on, every mob participant that is not the acting one is **frozen**: its
AI step (`Mob.serverAiStep` — goal selectors, brains, navigation, move/look/jump controls) is
cancelled and its movement inputs cleared, so it makes no decisions and does not move under its
own power. Everything else in its tick runs normally — gravity, status effects, age and despawn
timers, damage. A frozen mob is fully targetable and damageable; it just doesn't act (and won't
retaliate until its turn).

When the turn state makes it an entity's turn, it thaws — and what that turn allows is governed
by the [action economy](actions.md): the acting mob attacks through Critfall's API, moves under a
budget, and freezes in place once the budget is gone. Players are never frozen: you cannot freeze
player input like mob AI, so a player off its turn is held by the separate rule below.

Freeze state lives in a transient server-side set keyed by UUID — never on the entity — so no
crash or restart can leave a mob permanently frozen. Every encounter exit path (death, fleeing,
unload, dimension change, encounter end, level unload, server stop) thaws, and the per-tick
reconciliation against the turn state self-heals within one tick if anything slips.

### Excluding entity types

The escape hatch for any entity that misbehaves when its AI is suppressed — no code change
needed, mirroring Critfall's exempt tags:

- datapack: add the type to the `#initiative:no_freeze` entity type tag
  (see [examples/no_freeze.json](examples/no_freeze.json)), or
- config: list the id in `turns.no_freeze_types`.

An excluded entity still rolls initiative and holds a slot in the turn order, but it is never
frozen — it keeps acting in real time on everyone else's turns.

### Acting-entity marker

`acting_marker_enabled` shows a small green particle above whichever entity's turn it is, so
freeze/thaw can be eyeballed in playtests. It is deliberately cheap and debug-grade; the
turn-order HUD below is the real presentation.

## Holding players off-turn

While `restrict_players_off_turn` is on, a player who is a participant in a running encounter but
whose turn it is **not** cannot act on the world: it may not walk, break or place a block, use an
item, or interact with a block or an entity. Attacking was already refused by the action economy.
This is what the action UI has shown since M8 — the bar greys out off-turn — made true of the world
as well. On the player's own turn everything works normally, movement inside the budget included.

A player is not an AI to switch off, so the hold is two halves:

- a **root**: a transient `-100%` movement-speed modifier. The client obeys the attribute itself,
  so the player simply cannot accelerate — nothing has to fight the movement packets it sends, and
  there is no rubberbanding. Vanilla also scales the field of view by movement speed, which would
  zoom a held player's camera all the way in, so while the root is on, both loaders pin the
  field-of-view modifier to its neutral value: you keep the view of a player standing still
  (NeoForge through `ComputeFovModifierEvent`, Fabric through a client mixin, since it has no such
  event);
- a **pull-back**: the server remembers where the hold started and undoes any horizontal drift on
  the next tick, which catches jump momentum, ice, knockback from a mod, creative flight and
  anything else the attribute alone does not cover. Vertical movement stays free, so a held player
  still falls — except in a fluid, where "free" would mean sinking or bobbing for as long as the
  hold lasts, so a player standing in water or lava is held at its height as well. The anchor
  follows a falling player down, so one that drops into water is held at the surface it entered
  rather than pulled back up to where the hold began.

Two rules are deliberately narrow. **Only participants** are held: a player who is not in the
encounter walks past a fight untouched. And **only the world** is refused — the turn UI, chat and
the action bar keep working, because waiting for your turn should not mean waiting in silence.

Like the freeze, the hold is transient server state: an anchor in a UUID-keyed map and a modifier
that is never written to the player's saved data, so no crash or restart can leave anyone rooted.
The anchor is the single record of a held player, so a release can never drop one half and keep the
other. Every exit path releases — the turn arriving, the encounter ending, death, disconnect,
leaving the bubble, level unload, server stop — and the per-tick reconciliation against the turn
state heals within one tick if anything slips, including the toggle being switched off mid-fight.

## Turn-order HUD

While `hud.enabled` is on, every player participant sees an initiative bar at the top of the
screen: one cell per participant (type icon + name) in turn order, with the current actor
highlighted. A big encounter wraps onto further rows rather than running off the screen edges, and
a name too long for its cell is cut short — the bar stays readable and centered at any party size. Players show their skin face, mobs their spawn-egg icon, and anything unresolved
falls back to name only. Dead or fled participants disappear from the bar; survivors keep their
relative order and the highlight follows the turn.

Since M9 the acting participant's cell also carries the **turn countdown**, so a player waiting for
their slot reads both whose turn it is and how long is left.

The bar is a read-only reflection of server state. Snapshots are built and diffed at the same
per-tick reconcile point that drives the freeze, and are only sent when something changed —
encounter start, turn advance, join, leave, encounter end (which clears the bar), and the countdown
ticking over a second. A player who leaves the encounter has their own bar cleared; disconnecting
clears it client-side.

## Scope

Every encounter uses one simple initiative order, players and mobs interleaved; side-based
simultaneous player turns are not built. What changes with several humans in one encounter — the
shared clock, disconnects, sides — is in [multiplayer](multiplayer.md). The action economy (movement
budgets, the Attack action, turn-end rules) is documented in [actions](actions.md).
