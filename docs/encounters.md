# Encounters

Server-side only; clients receive nothing yet. All values come from [config](config.md).

## Lifecycle

- **Trigger** — a player attacks a hostile (`Enemy`) or a hostile attacks a player (each
  direction is a config flag). The encounter's center is fixed at the victim's position at
  trigger time. PvP does not trigger an encounter in M1.
  Detection subscribes to Critfall's `CritfallEvents.onCombatInteraction`, which fires before
  all of Critfall's own gating — a swing Critfall resolves as a miss, fumble, or cancelled hit
  still starts an encounter. It never fires for `RollService.performAttack` damage, so attacks
  Initiative itself drives through the API cannot re-trigger detection.
- **Provoked neutrals** — a mob that is not an `Enemy` starts an encounter the moment it turns
  hostile toward a player. See [the rule](#when-a-neutral-mob-counts-as-hostile) below.
- **Initial pull** — every living `Enemy` within `trigger_radius` of the center joins, and so does
  every player there: the whole party rolls initiative, not just whoever struck first.
- **Late join** — each tick, living `Enemy` entities inside `trigger_radius` that are not in an
  encounter join, as do neutral mobs inside it that are hostile toward a player participant — an
  already-angry wolf that walks into the bubble joins, the sheep beside it does not. A player who
  walks into the bubble joins the same way, rolling initiative into the running order. Spectator
  and creative players are never pulled in by the radius; attacking or being attacked still brings
  a player in from anywhere. With `pull_nearby_players` off, that is the only way a player joins.
  An entity belongs to **exactly one** encounter: one already fighting elsewhere is never pulled
  into a second, and encounters never merge.
- **Leave** — each tick a participant is dropped when it is dead, gone from the level
  (unloaded, dimension change), or farther than `leave_radius` from the center — except a
  [ranged attacker](#shooting-into-a-fight-from-outside) still shooting into it. A former neutral is
  dropped as well once it is no longer hostile toward any player in the encounter — anger that runs
  out is a leave path like fleeing the bubble, and it takes the same exit. That one exit stands down
  while a participant is **hidden**: concealment is what took the mob's target away, not the mob
  calming down (see [Hide](actions.md#hidden-also-hides-you-from-mob-ai)). A player who
  disconnects is dropped at once rather than a tick later; see [multiplayer](multiplayer.md).
- **End** — an encounter ends when it no longer has both a player participant and a hostile
  participant. Level unload and server stop end all affected encounters immediately.

## When a neutral mob counts as hostile

`Enemy` marks the mobs that are always hostile. Everything else — pandas, wolves, bees, iron
golems, llamas, cows — is neutral until something makes it fight, so Initiative reads that
transition rather than the hit that caused it. Hitting a mob is not the trigger; the mob deciding
to fight back is. Farming a cow therefore stays real-time forever: a cow has no target selector and
no anger, so it never becomes hostile toward anybody.

A neutral mob is hostile toward a player when **either** holds:

| Signal | Read from | Covers |
|---|---|---|
| Its attack target is that player | `Mob.getTarget()` | Every target-acquisition mob: a panda or llama fighting back, an iron golem defending its village, a provoked wolf, anything with a hurt-by-target goal — modded mobs included, since they all end up in `Mob.setTarget`. |
| It is angry at that player | `NeutralMob.isAngryAt(player)` | Persistent anger, which outlives the target field: wolves, bees, polar bears. Also the `universalAnger` game rule. |

The target alone would be enough to start a fight but not to stay in one: goals drop and re-pick
their target, and a mob that left the encounter on one of those ticks would flip in and out every
round. Reading anger as well holds an angry mob in place until the anger itself ends.
`isAngryAt` is vanilla's own answer, so a mob that cannot attack you (peaceful difficulty, an owner
its pet will not turn on) does not count as hostile toward you.

The transition is reported from `Mob.setTarget` — NeoForge's `LivingChangeTargetEvent`
(`MOB_TARGET` only) and, since Fabric has no such event, a `HEAD` inject with the same reach. The
report only queues the mob; the encounter forms on the next encounter tick, which reads the target
that was actually set. As with an always-hostile mob, the provoking hit itself resolves in real
time before any of this — what turns turn-based is everything after it.

Once in, a former neutral is an ordinary hostile participant: initiative roll, AI freeze off-turn,
driven d20 on its turn, `CombatSuppression`. There is no special case for it anywhere else.

Inside an encounter the attack gate has to make room for this: a swing at a neutral mob that no
encounter owns is left to vanilla rather than refused, because the swing is what provokes it. See
[the Attack action](actions.md#the-attack-action).

Set `trigger_on_provoked_neutral` to `false` to switch the whole path off — encounters then form
from `Enemy` mobs alone, exactly as they did before, within a tick of the flag changing.

## Shooting into a fight from outside

A melee mob has to close to reach you, and closing is what puts it in the bubble. A skeleton never
has to. The attack is what makes it a participant, not where it stands: an arrow, a trident or any
other ranged hit that reaches a participant pulls its shooter into that participant's encounter,
however far outside `trigger_radius` it was standing. That already followed from triggering on
Critfall's combat interaction — the arrow's owner is the attacker it reports — but the shooter was
then dropped again a tick later for standing outside `leave_radius`, so it never took a turn and
its next arrow was another real-time hit.

So the fight remembers who shot their way in. A participant marked that way is exempt from the
distance leave path while it is still hostile toward a player in the encounter, read exactly the
way the [neutral leave path](#when-a-neutral-mob-counts-as-hostile) reads it, concealment
stand-down included. Losing its target hands it straight back to the leave radius, as does turning
`hold_ranged_attackers` off, within a tick either way. Death, unloading and dimension change are
untouched exits.

Both ends of the shot have to be in the same fight: a shot into somebody else's encounter, or one
the one-encounter guard turned away, marks nobody. An arrow with no living shooter behind it — a
dispenser's, or one whose owner died in flight — has no attacker for Critfall to report and so
changes nothing. PvP is excluded here as everywhere else in M1: a player's arrow into another
player forms no hostile participant.

Once in, the shooter is an ordinary participant: initiative roll, freeze off-turn, suppression,
and its shots between participants canceled like every other out-of-turn hit
(see [actions](actions.md#initiative-is-the-sole-combat-driver)). Ranged **attack actions** are
still deferred, so a pulled-in shooter has nothing to spend its turn on until it can reach melee;
its turn ends on `mob_turn_timeout_ticks`.

## Critfall handoff

Every participant gets Critfall's `CombatSuppression` flag on join (`RollService.suppress`), so
Critfall's automatic real-time d20 pipeline stands down for it. The flag is released on every
exit path listed above. Membership is tracked by UUID, so flags are released even when the
entity object is already gone — no leaked flags.

## Combat behavior inside the bubble

Attacks between suppressed entities resolve as plain vanilla damage: Critfall stands down and
Initiative does not yet drive rolls. Turn-taking and API-driven attacks arrive in M2/M3.
