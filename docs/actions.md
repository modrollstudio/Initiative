# Action economy

Server-side only. All values come from [config](config.md); the whole system is behind the
`actions.enabled` flag (and requires `turns.enabled`) — off restores the M2 behavior exactly: no
budgets, no attack interception, no damage cancellation, and the acting participant's turn is a
plain real-time window.

## Everything is a registered action

Since M7 every action listed on this page — Attack, Dash, Disengage, Dodge, Help, Hide, Shove,
Blink, Reel, Grapple, Escape, End Turn and the opportunity attack — is an ordinary entry in a public registry,
declared through exactly the API a third-party mod uses ([api.md](api.md)). There is no privileged
path: the registry and its invocation pipeline cannot tell a built-in from a mod's action, and the
`/initiative <action>` commands are generated from the registry rather than hand-written.

What each action declares is its cost (action / bonus action / movement / reaction / free), its
targeting (none / self / position / entity with a side and optional reach), its enable toggle and
its effect. The pipeline then gates it — economy, toggle, encounter, turn, resource, target — runs
the effect, and charges the cost **only if the action performed**. That ordering is why an attack
into total cover and a blink without a pearl keep the turn's action, while a lost Shove or Hide
contest still spends it.

**Movement is not a registered action.** It is a continuous per-tick reconcile (see
[Movement](#movement)), not something invoked, so it stays where it is; the budget it draws on is
exposed as the `MOVEMENT` cost for actions that want to spend it, which is how block placement is
charged.

## The per-turn budget

At the start of each participant's turn its budget resets to:

- a **movement budget** (`movement_budget_blocks`, default 6 blocks ≈ 30 ft),
- one **action**,
- one **bonus action**,
- one **reaction**.

The bonus action and reaction are tracked but nothing consumes them yet — bonus-action uses and
opportunity attacks arrive in M4. The budget lives on the encounter, keyed by (round, actor), and
resets lazily the moment the turn changes — there is no window where a new actor inherits stale
budget.

## The Attack action

On your turn, clicking an enemy participant performs a **driven attack**: the vanilla swing is
canceled and Initiative calls Critfall's `RollService.performAttack` with a melee `AttackContext`
(the correct player/mob damage source and held weapon). Critfall resolves the real d20 — hit,
miss, crit, fumble, damage, feedback readout — exactly as in real-time combat, but turn-ordered.
Because every participant carries Critfall's suppression flag, the automatic real-time pipeline
stays down and this driven roll is the only d20; `performAttack` applies its damage through a
re-entry guard, so the resulting `hurt` never re-rolls. Driven damage also bypasses invulnerability
frames, so two attackers can focus one target in the same round and both hits land full damage —
the second is never swallowed by the first hit's i-frames.

An attack consumes the action — **also on a miss** (you committed the action; the dice decided the
outcome). With `single_attack_per_turn` off, attacks bypass the action entirely and are limited
only by the turn itself.

**Swinging at something outside the fight.** Only participants can be driven at, so anything else
is normally refused as `INVALID_TARGET` — but a neutral mob that no encounter owns is let through
as a plain vanilla swing instead. Refusing it would deadlock provocation: the swing is what makes
the mob hostile, and until it is hostile it cannot be a participant. An always-hostile mob never
needs the exception, because it is pulled into the bubble on sight and is already a target. Nothing
is driven and no action is spent, so a cow stays farmable mid-fight and simply never joins; a panda
takes the hit, turns on you, and is in the turn order the next tick. The turn and economy gates run
first, so this is not a way to act off-turn. Off with
[`trigger_on_provoked_neutral`](config.md).

**Reach.** An attack only reaches `attack_reach_blocks` (horizontal centre-to-centre distance,
default 4 — a melee swing, not a sniper shot). Further away the attempt is refused as
`OUT_OF_REACH` without a roll, and the [turn UI](ui.md) does not offer the target at all. It is the
same reach rule Shove and the opportunity attack use, config-driven for the same reason: reach is a
balance number, so it lives in config rather than in code, and a pack that wants spear-length arms
raises it.

## Standard actions

Four more standard actions run on your turn, each consuming the one action and each individually
toggleable (`actions.dash`, `actions.disengage`, `actions.dodge`, `actions.help`). Turning one off
makes only that action unavailable — the rest, and the wider economy, are unaffected. They are
server commands for now (keybinds arrive with the turn UI): `/initiative dash`,
`/initiative disengage`, `/initiative dodge`, `/initiative help <target>`. All four are gated
exactly like the Attack action — only the current actor, only with the action still available;
out-of-turn, action-less, disabled or (for Help) bad-target attempts are rejected without effect.

| Action | Effect |
|---|---|
| **Dash** | Grants extra movement equal to the base movement budget on top of what is left (a full 6-block turn effectively becomes 12). The movement clamp still enforces the extended total — a move past it is reverted at the edge. |
| **Disengage** | Raises a per-turn `disengaged` flag meaning "does not provoke opportunity attacks this turn." **Inert in M4a** — nothing reads it yet; the opportunity-attack system that consumes it is M4b. |
| **Dodge** | Raises a per-turn `dodging` flag: attacks against this participant roll with **disadvantage**. The disadvantage is applied through Critfall's roll mode (`AttackContext.withMode(DISADVANTAGE)`), not a reimplemented roll. The 5e Dodge also grants advantage on DEX-style saves — a hook for when saves are wired into Initiative; not implemented in M4a. |
| **Help** | Targets a living ally in the encounter and grants that ally **advantage on its next attack** (applied via `AttackContext.withMode(ADVANTAGE)`). The grant is consumed by the ally's next driven attack, or expires at the helper's next turn. |

**Advantage and disadvantage together cancel** to a normal roll (5e rule): a Helped attacker
striking a Dodging target rolls one plain d20 — and the Help grant is still spent. Each source is
honored only while its own action is enabled, so toggling Dodge or Help off cleanly removes its
effect from the roll.

The `dodging`, `disengaged` and Help-advantage flags are per-turn and never leak: `dodging` and
`disengaged` clear at the owner's next turn start, a Help grant clears when consumed or at the
helper's next turn, and every flag is purged when a participant leaves the encounter.

### No Critfall API gap this milestone

Applying advantage/disadvantage through `performAttack` needed no new Critfall API: 0.2.3 already
exposes `RollMode` (`NORMAL`/`ADVANTAGE`/`DISADVANTAGE`) and `AttackContext.withMode`, and the
attack pipeline honors the context's mode when it rolls the d20. Initiative sets the mode and lets
Critfall roll — it never rolls the dice itself.

## Reactions

Each participant has **one reaction per round**, refreshed at the start of **that participant's own
turn** (not round start). A reaction is an out-of-turn triggered response — currently only opportunity
attacks — that the participant can use before the next turn arrives.

### Opportunity attacks

When a participant leaves the reach of a hostile participant in the same encounter, that hostile
can use its reaction to make an opportunity attack. Reach is measured as horizontal (xz) distance
and defaults to `opportunity_attack_reach_blocks` (3 blocks ≈ 15 ft); only participants on
opposite sides (enemies) of the encounter provoke. The Disengage action suppresses all opportunity
attacks against the disengager this turn.

A reaction resolves **without freezing the reactor** and **never advances the turn**: an AI-controlled
reactor that uses its reaction stays frozen in place for the rest of the turn, a player can use it
instantly, and the turn counter never moves. The reaction is then consumed until the reactor's next
turn start. Reactions are individually toggleable via `actions.opportunity_attack`.

### Opportunity attacks are plain attacks

D&D 5e opportunity attacks are not rolls — they are plain attacks just like any other, so M4b drives
them through the same attack system as Turn actions. (The **Hide** contest below is a different case;
it uses Critfall's `RollService.contest`, added in 0.2.4.)

## Hide

Taking the Hide action consumes your one action and triggers a **contested roll** through Critfall's
`RollService.contest`: you (the initiator) roll `d20 + hide_stealth_bonus` against the **nearest
hostile observer** (the opponent), who rolls `d20 + hide_observer_perception_bonus`. You are hidden
only if your total **strictly exceeds** the observer's — a tie means you are seen. If there is no
living hostile observer, you are automatically hidden (no one is there to see you).

`hide_stealth_bonus` and `hide_observer_perception_bonus` are flat, caller-supplied config numbers.
Initiative does not derive them from any entity attribute. With [Checks installed](#with-checks-installed)
Hide rolls Stealth instead, and enemies' passive Perception decides when you are found.

If you **succeed**, you are hidden, which cuts both ways:

- **Offense**: your next attack rolls with **advantage** (a single attack, not multiple).
- **Defense**: attacks against you roll with **disadvantage** — a hidden participant is hard to
  hit. This reuses the same `AttackContext.withMode(DISADVANTAGE)` path as Dodge; being both
  Dodging and hidden does **not** stack beyond a single disadvantage (5e), and advantage and
  disadvantage still cancel to a normal roll.

Unlike `dodging`/`disengaged`, hidden is a **persistent condition**, not turn-scoped: it survives
your own turn starting again, so the advantage is still there to spend on a later turn's attack
(Hide spends the Action, so that attack can only ever happen on a later turn). The hidden condition
ends only when you attack or when you are hit by any attack (or if you leave the encounter).

If you **fail**, the action is still spent; you are not hidden and gain neither effect.

**No sneak attack bonus in M4b**: the "advantage on the next attack" mechanic is implemented, but
the bonus damage from landing a hidden attack is a M5+ feature.

### Hidden also hides you from mob AI

While you are hidden, mobs cannot target you. A mob that tries to acquire you is refused, and a mob
that already had you loses you the moment you hide — it stops pathing to you and stops swinging,
because a mob with no target does nothing. It does not walk to your last known position; vanilla
with no target simply idles or wanders. Since a mob's turn still comes around, a mob that cannot
find anyone burns its turn on the timeout, which is what hiding buys you.

The moment hidden ends — you attack, you are hit, you leave the encounter or it ends — targeting
returns to normal on the next tick. Nothing is written to the mob or to you, so no mob can be left
unable to see a player.

What this covers, honestly:

| Targeting style | Covered? |
|---|---|
| Vanilla goal-driven mobs (zombie, skeleton, spider, wolf, iron golem …), both loaders | Yes — acquisition refused, an existing target taken away |
| Modded mobs whose custom goals set their target the normal way (`Mob.setTarget`) | Yes, the same hook |
| Brain-driven mobs (piglin, hoglin, warden, breeze) on NeoForge | Yes — the brain's attack memory is refused and cleared |
| Brain-driven mobs on Fabric | Degraded: the memory is cleared every tick, but nothing refuses the write, so such a mob can re-acquire for a tick at a time. Fabric has no hook on that path |
| Mods with fully custom AI that never sets a target, or writes the field directly | No. Nothing short of rewriting their AI would cover it |
| A projectile already in flight, a creeper already fusing | No — actions already committed still land |

Toggled by `actions.hide_suppresses_targeting` (default on). Off leaves Hide exactly as it was: the
roll effects above, and mobs that see and target you as usual.

While anyone in the encounter is hidden, a former neutral whose target was taken away by hiding is
**not** dropped from the fight for having lost it (see [encounters](encounters.md)); it would
otherwise leave the moment you hid and rejoin when you were seen again.

Rejection rules (the swing is canceled, no roll happens, the attacker sees an actionbar message):

| Attempt | Result |
|---|---|
| Not your turn | Rejected — out-of-turn attacks do nothing. |
| No action left | Rejected. |
| Target not in your encounter, dead, or on your own side | Rejected. |
| Attacker not in an encounter | Vanilla proceeds (this is how encounters form). |

## Minecraft-native actions

Three actions that make turn-based combat use the actual world. Each consumes the one action, is
turn- and action-gated exactly like Attack, and is individually toggleable. Contest bonuses are flat,
caller-supplied config numbers; with [Checks installed](#with-checks-installed) Shove rolls skills
instead. They are server commands for now (keybinds arrive with the turn UI).

### Shove (`/initiative shove <target>`, `actions.shove`)

Targets an enemy participant within `shove_reach_blocks` (horizontal distance, default 3) and resolves
as a contested roll through Critfall's `RollService.contest` — you (`shove_attacker_bonus`) vs. the
target (`shove_defender_bonus`). On a win the target is knocked back **away from you** using vanilla
knockback (`shove_knockback_strength`), so falls, lava and cliffs resolve naturally — environmental
kills are the point. On a loss nothing happens, but the action is still spent. Out-of-turn,
action-less, disabled, bad-target (`INVALID_TARGET`) and out-of-reach (`OUT_OF_REACH`) attempts are
rejected without spending anything.

### Ender-pearl blink (`/initiative blink`, `actions.ender_pearl_blink`)

Consumes one ender pearl from your inventory and teleports you up to `blink_max_blocks` along your
horizontal facing (your Y is preserved). It costs the **Action**, not movement. With no pearl the
attempt is rejected (`MISSING_ITEM`) and nothing is consumed. This is a straight-line teleport — it
does not check line of sight or cover (that is a later milestone).

### Reel (`/initiative reel <target>`, `actions.fishing_rod_reel`)

The fishing-rod pull. Requires a rod in either hand and resolves as a contested roll
(`reel_attacker_bonus` vs. `reel_defender_bonus`). On a win the target is dragged **toward you**
(`reel_pull_strength`, vanilla knockback). Win or lose, the rod takes one durability and the action
is spent. With no rod the attempt is rejected (`MISSING_ITEM`) and the rod is untouched. A rod
reaches across the encounter, so Reel has no reach gate.

This was called `initiative:grapple` before M8; it is a Minecraft-native improvised action, not the
5e Grapple, and the name now belongs to the real one below.

Knockback and pull strengths are vanilla knockback impulses (not exact block distances); vanilla
physics carries the target from there. These, and the contest bonuses, are placeholder tunables.

## Grapple and Escape

The 5e grapple: a special melee attack made with a free hand that ends with the target held rather
than hurt. Its numbers live in their own [`grapple` config group](config.md#grapple), the way cover
does — a condition with a lifetime is not one more knockback tunable. With
[Checks installed](#with-checks-installed) both contests roll skills instead of those bonuses.

### Grapple (`/initiative grapple <target>`, `grapple.enabled`)

Costs your action and needs **one free hand** (main or off); with both full the attempt is refused
as `HANDS_FULL` and nothing is rolled or spent. It targets an enemy participant within
`grapple.reach_blocks` (horizontal, default 3) and resolves as a contest — you
(`grapple.attacker_bonus`) against the target (`grapple.defender_bonus`). On a win the target is
**grappled**; on a loss nothing is applied. Either way the action is spent, like every other
contest.

A grappled participant's **turn starts with no movement budget at all** — speed 0. It may still act:
attack, dodge, or try to escape.

### Escape (`/initiative escape`, `grapple.escape`)

Only exists for a participant that is currently grappled — the [turn UI](ui.md) shows the button to
nobody else, and an invocation from anyone else is refused as `DISABLED`. It costs the escaping
participant's action and resolves as a contest against whoever is holding it
(`grapple.escape_attacker_bonus` vs. `grapple.escape_defender_bonus`). On a win the hold clears and
the movement its turn began without is handed back. On a loss the hold stays and the action is gone.

### How a hold ends

A grapple clears when any of these happens — reconciled every tick, so nothing can leak a condition
onto a corpse or an entity that walked away:

- the grappled participant **escapes**;
- either side **dies, is removed, or leaves** the encounter (including the encounter ending);
- the two end up further apart than `grapple.break_distance_blocks` (default 5) — a shove, a blink
  or plain knockback breaks the hold without anyone spending an action on it.

## With Checks installed

[Critfall: Checks](https://modroll.studio) adds ability scores and skills. Initiative works without
it; when it is installed and [`checks.enabled`](config.md#checks) is on (the default), four actions
roll the participants' own skills through Checks instead of the flat config bonuses. Checks rolls
every die through Critfall, so these rolls animate like any other.

| Action | Initiator rolls | Opponent rolls |
|---|---|---|
| Shove, Grapple | Athletics | Athletics or Acrobatics, whichever is higher |
| Escape | Athletics or Acrobatics, whichever is higher | the grappler's Athletics |

The contest rules are unchanged: the initiator wins only with a strictly higher total.

**Hide** is no longer a contest. You roll Stealth and are hidden, and your total is kept while you
stay hidden. On each enemy's turn, that enemy's **passive Perception** (`10 +` its Perception
modifier) is checked against it: if it meets or beats your total, you are revealed. Nobody rolls for
this. Attacking, being hit and leaving the encounter still reveal you as before.

Turn `checks.enabled` off, or play without Checks, and all four go back to the flat bonuses exactly.
Reel always uses its flat bonuses.

## Cover

Cover is **positional, not an action**: every driven attack — the Attack action, mob turns, and
opportunity attacks — recomputes it from the live geometry between attacker and target. Standing
behind a pillar genuinely makes you harder to hit from that angle, and stepping out drops the bonus
on the next attack (nothing is cached). It is gated by `cover.enabled`; off restores the exact
pre-M5b attack.

**Sampling.** Eight rays are cast from the attacker's eye to the eight corners of the target's
hitbox, pulled inward slightly so a target flush against a wall is sampled by its body rather than
its skin. A ray is obstructed when Minecraft's own collision clip (`ClipContext.Block.COLLIDER`)
stops it before the corner — the same physical predicate vanilla uses for line of sight and
projectile travel. So **any block with a collision shape counts** (full blocks, fences, glass,
closed doors, leaves), while shapeless blocks (tall grass, torches) and fluids do not; there is no
hand-maintained block list. Transparent-but-solid blocks like glass grant cover because an attack
still cannot pass through them. Entities never grant cover in M5b (no "shooting past your ally" —
deferred).

**Tiers.** The obstructed fraction maps to a tier, each config-tunable:

| Obstructed fraction | Tier | Defender AC |
|---|---|---|
| `>= 1.0` (all eight rays) | Total | attack rejected (see below) |
| `>= three_quarter_cover_threshold` (default 0.75) | Three-quarters | `+three_quarter_cover_ac_bonus` (default +5) |
| `>= half_cover_threshold` (default 0.5) | Half | `+half_cover_ac_bonus` (default +2) |
| below `half_cover_threshold` | None | +0 (no context change) |

**Total cover.** By default (`total_cover_blocks_attack`) an attack with no line at all is rejected
with `TOTAL_COVER` and **spends nothing** — you keep your action to reposition instead of loosing
into a wall. Set the flag off and total cover degrades to the three-quarter bonus and the attack
proceeds. Opportunity attacks against a fully-covered mover are simply suppressed (the reaction is
kept).

**Applying it.** The tier's AC bonus is fed into Critfall's own roll through
`AttackContext.withDefenderAcBonus(n)` — Initiative never reimplements AC math. Critfall echoes the
effective AC, the base AC and the bonus back on the `AttackResult` and shows the split in its own
roll readout; Initiative adds no line of its own.

## Building for cover

While it is a participant's turn, placing a block spends movement budget
(`block_placement_movement_cost`, default 1 block of movement per block placed); with too little
movement left the placement is prevented. Gated by `block_placement_costs_movement`; off restores
free placement. Only participants acting on their own turn are charged — non-participants and any
placement outside an encounter are untouched, and a participant off its turn is refused outright by
the off-turn hold before the cost is ever weighed. This is a deliberately simple first pass; a richer
build-action model comes later.

## Movement

While it is your turn, moving consumes the budget: each tick the horizontal (xz) distance from
the last in-budget position is deducted. A tick that would overshoot the remaining budget burns
what is left and reverts you to that position — a hard stop at the edge. Vertical movement
(falling, knockback) is free. Off your turn you do not move at all: see
[holding players off-turn](turns.md#holding-players-off-turn), which also refuses the block
breaking, placing and interaction described above.

## Mob turns

M3 mob turns are deliberately simple: the acting mob is unfrozen (M2b), walks under the same
movement clamp — and freezes in place once its budget is gone — and attacks the nearest living
enemy participant through the API as soon as it is within melee reach. Its action is consumed
regardless of `single_attack_per_turn` (that flag is player-facing). Full mob tactics come later.

## Turn end

A turn ends when any of these fires:

- **Explicitly**: the acting player clicks End Turn in the [turn UI](ui.md), or runs
  `/initiative endturn`. Since M8 this is the normal way a turn ends.
- **Spent**: a mob's turn ends once its action is spent (`mob_end_turn_when_spent`, default on — a
  mob has no UI to click). A player's turn does the same only with `end_turn_when_spent` on, which
  defaults **off** since M8: a spent turn stays yours until you end it.
- **Timeout**: the M2a `turn_timeout_ticks` safety net, shown as a countdown in the UI.

## Initiative is the sole combat driver

While the economy is active, vanilla damage between two participants of the same encounter is
canceled — this covers the acting mob's vanilla melee (its attacks go through the API instead)
and out-of-turn hits, including projectiles. Initiative's own driven damage is exempted through
Critfall's public `RollService.isDrivenDamage`, as is anything it synchronously recoils (thorns),
which Critfall intercepts normally. Damage from outside the encounter — environment, non-participants
— is untouched.

Two deliberate edges:

- Membership is read **as of the previous tick**, so the encounter-forming hit (which joins both
  sides mid-hurt, in loader-order-dependent listeners) always lands as plain vanilla damage on
  both loaders.
- Ranged **attack actions** are deferred (M4+): a bow shot between participants is simply
  canceled damage in M3, not an action.

## Advantage and disadvantage

Attacks driven with Critfall's advantage or disadvantage are reported by Critfall's own roll
readout, which since 0.2.6 carries the roll mode, both natural dice and the kept one on the
result's `RollDetail`. Initiative emits no hint line of its own.

## Config

```json
"actions": {
  "enabled": true,
  "movement_budget_blocks": 6.0,
  "single_attack_per_turn": true,
  "attack_reach_blocks": 4.0,
  "end_turn_when_spent": false,
  "mob_end_turn_when_spent": true,
  "dash": true,
  "disengage": true,
  "dodge": true,
  "help": true,
  "hide": true,
  "opportunity_attack": true,
  "opportunity_attack_reach_blocks": 3.0,
  "hide_observer_perception_bonus": 0,
  "hide_stealth_bonus": 0,
  "shove": true,
  "shove_reach_blocks": 3.0,
  "shove_knockback_strength": 1.0,
  "shove_attacker_bonus": 0,
  "shove_defender_bonus": 0,
  "ender_pearl_blink": true,
  "blink_max_blocks": 8.0,
  "fishing_rod_reel": true,
  "reel_pull_strength": 1.0,
  "reel_attacker_bonus": 0,
  "reel_defender_bonus": 0
}
```

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for the whole action economy. Off = M2 real-time turn windows. |
| `movement_budget_blocks` | number ≥ 0 | Blocks of movement per turn. 0 = no movement on your turn. |
| `single_attack_per_turn` | boolean | Attacks consume the one action per turn. Off = attacks don't touch the action (and turns never auto-end from attacking). |
| `attack_reach_blocks` | number ≥ 0 | How far an attack reaches (horizontal centre-to-centre, default 4). Beyond it: `OUT_OF_REACH`, and the UI offers no such target. |
| `end_turn_when_spent` | boolean | A player's turn auto-ends when spent (see above). Default off: the player ends the turn deliberately. |
| `mob_end_turn_when_spent` | boolean | A mob's turn auto-ends once its action is spent. Default on. Off = mob turns end only on the timeout. |
| `dash` | boolean | Toggle for the Dash action. Off = Dash unavailable. |
| `disengage` | boolean | Toggle for the Disengage action. Off = Disengage unavailable. |
| `dodge` | boolean | Toggle for the Dodge action. Off = Dodge unavailable and no attack rolls with dodge disadvantage. |
| `help` | boolean | Toggle for the Help action. Off = Help unavailable and no attack rolls with help advantage. |
| `hide` | boolean | Toggle for the Hide action (M4b). Off = Hide unavailable. |
| `opportunity_attack` | boolean | Toggle for opportunity attacks (M4b). Off = opportunity attacks are unavailable. |
| `opportunity_attack_reach_blocks` | number ≥ 0 | Horizontal distance (blocks) at which a hostile provokes opportunity attacks. Default 3 ≈ 15 ft. |
| `hide_observer_perception_bonus` | integer | Flat bonus added to the nearest hostile observer's Perception roll in the Hide contest (`d20 + bonus`). Unused while Checks drives Hide. Default 0. |
| `hide_stealth_bonus` | integer | Bonus added to your Stealth contest roll (`d20 + hide_stealth_bonus`). Unused while Checks drives Hide. Default 0. |
| `shove` | boolean | Toggle for the Shove action (M5a). Off = Shove unavailable. |
| `shove_reach_blocks` | number ≥ 0 | Horizontal distance at which you can shove a target (default 3). |
| `shove_knockback_strength` | number ≥ 0 | Vanilla knockback impulse applied on a won shove (default 1.0). |
| `shove_attacker_bonus` | integer | Flat bonus on your Shove contest roll. Unused while Checks drives Shove (default 0). |
| `shove_defender_bonus` | integer | Flat bonus on the target's Shove contest roll. Unused while Checks drives Shove (default 0). |
| `ender_pearl_blink` | boolean | Toggle for the ender-pearl blink (M5a). Off = unavailable. |
| `blink_max_blocks` | number ≥ 0 | Max horizontal teleport distance for the blink (default 8). |
| `fishing_rod_reel` | boolean | Toggle for the Reel action (M5a, renamed from `fishing_rod_grapple` in M8). Off = unavailable. |
| `reel_pull_strength` | number ≥ 0 | Vanilla knockback impulse pulling the target toward you on a won reel (default 1.0). |
| `reel_attacker_bonus` | integer | Flat bonus on your Reel contest roll. Placeholder (default 0). |
| `reel_defender_bonus` | integer | Flat bonus on the target's Reel contest roll. Placeholder (default 0). |

The unarmed Grapple and Escape read the separate [`grapple` config group](config.md#grapple).

All fields are required.
