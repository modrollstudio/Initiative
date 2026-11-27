# The action API

Initiative is an engine, not a content mod: it owns turns, the action economy and roll presentation,
and every action — its own included — is an ordinary entry in one registry. This page is how another
mod or a datapack adds theirs.

Everything here lives in `studio.modroll.initiative.api`. Nothing in that package exposes an
Initiative internal type, so you compile against the API alone.

## Registering an action

Register during mod construction, before a world loads:

```java
public static final ResourceLocation TAUNT =
        ResourceLocation.fromNamespaceAndPath("examplemod", "taunt");

ActionRegistry.register(Action.builder(TAUNT)
        .cost(ActionCost.ACTION)
        .targeting(ActionTargeting.entity(Side.ENEMY, () -> 6.0))
        .enabled(() -> ActionSettings.enabled(TAUNT, true))
        .messages("examplemod.action.taunt.done", "examplemod.action.taunt.shrugged")
        .effect(ExampleActions::taunt)
        .build());

private static ActionResult taunt(ActionContext context) {
    LivingEntity target = context.target().orElseThrow();
    ContestResult result = RollService.contest(context.actor(), target, ContestContext.of(2, 0));
    context.showRoll(result, target);
    if (result.initiatorWins()) {
        // your own state, your own rules
        Taunts.mark(target);
    }
    return ActionResult.performed(result.initiatorWins());
}
```

That is the whole contract. Initiative gates it, targets it, charges it, animates its roll and gives
it a command — the same treatment its own Attack and Shove get.

Ids are unique: registering an id twice throws rather than quietly replacing someone else's action.

An action your own controls start, such as a spell cast from your own wheel, can stay off the bar and the
commands with `.listed(false)` (since 0.3.3). It is only ever taken through `ActionRegistry.invoke`.

## The contract, field by field

| Field | Meaning |
|---|---|
| `id()` | Namespaced id. Use your own namespace. |
| `cost()` | Which turn resource it spends — queried **per invocation**, so it may depend on config. |
| `targeting()` | What must be pointed at it, checked before your effect runs. |
| `enabled()` | Queried per invocation. False makes only this action unavailable. |
| `availableTo(actor)` | Queried per invocation and per UI build. False hides it from that actor alone — a condition, not a resource (Escape uses it: only a grappled participant has one). |
| `perform(context)` | Your effect. |
| `movementCost()` | Blocks charged when the cost is `MOVEMENT`. |
| `successKey()` / `failureKey()` | Translation keys for the command's action-bar line. |
| `commandLiteral()` | Overrides the generated command literal. |

### Costs

| Cost | Resource | Turn-gated |
|---|---|---|
| `ACTION` | the one action per turn | yes |
| `BONUS_ACTION` | the one bonus action per turn | yes |
| `MOVEMENT` | `movementCost()` blocks of the movement budget | yes |
| `REACTION` | the one reaction per round, refreshed at its owner's turn start | **no** |
| `FREE` | nothing | yes |
| `ATTACK` | one attack of the turn's Attack action | yes |

`ATTACK` is how several attacks share one action. The first `ATTACK` spends the action and opens the
rest of the Attack action's attacks ([`Attacks`](#several-attacks-attacks)); each later one spends one
of those. With one attack per action it behaves exactly like `ACTION`. Initiative's own Attack and a
bow, crossbow or trident shot cost `ATTACK` (`FREE` with `single_attack_per_turn` off), so a mod's
`ATTACK` action can be the second attack after a swing, or the other way round.

`REACTION` is the only cost taken off-turn. Initiative does not yet expose a public trigger seam, so
a reaction you register can only be invoked by code you control calling `ActionRegistry.invoke`; a
trigger API is a later milestone.

### Targeting

```java
ActionTargeting.none()                       // nothing to point at
ActionTargeting.self()                       // the actor is the subject
ActionTargeting.position()                   // a Vec3
ActionTargeting.entity(Side.ENEMY)           // an enemy participant, any distance
ActionTargeting.entity(Side.ALLY, () -> 5.0) // an ally within 5 blocks (horizontal)
```

`Side` is `ENEMY`, `ALLY` or `ANY`, resolved against the encounter's sides. The reach supplier is
read per invocation, so it can come from config. An entity target is rejected as `INVALID_TARGET`
when it is absent, dead, outside the encounter or on the wrong side, and `OUT_OF_REACH` when a
declared reach is exceeded — your effect never sees any of those.

### `ActionContext`

What your effect may touch:

```java
ServerLevel level();
LivingEntity actor();
Optional<LivingEntity> target();
Optional<Vec3> position();
boolean isParticipant(LivingEntity entity);
boolean areEnemies(LivingEntity a, LivingEntity b);
List<LivingEntity> participants();       // living participants of this encounter
boolean spendMovement(double blocks);    // false if unaffordable; nothing is spent
void grantMovement(double blocks);       // Dash and its kin
void showRoll(AttackResult result, LivingEntity opponent);
void showRoll(ContestResult result, LivingEntity opponent);
AttackResult attack(LivingEntity target, AttackContext attack);
Optional<AttackContext> prepareAttack(LivingEntity target, AttackContext attack);   // since 0.3.3
void teleport(LivingEntity entity, Vec3 to);                                       // since 0.3.3
void launch(LivingEntity target, Vec3 motion);
Optional<JsonObject> settings();         // this action's datapack file
```

`attack` rolls an attack through Critfall the way Initiative's own Attack does and shows it: Help and
hiding give advantage (and are spent), Dodge and a hidden target give disadvantage, cover adds to the
target's AC, and a hit finds a hidden target. Your context's own mode joins in the 5e way, so
advantage and disadvantage still cancel. A projectile or thrown context also gets the ranged rules:
disadvantage past `ranged_normal_range_blocks` or with a hostile in reach, and past
`ranged_long_range_blocks` nothing is rolled and it misses. Into total cover that blocks attacks it
misses with nothing rolled as well. The dice are Critfall's: your damage dice, your mode, its roll.

`prepareAttack` is `attack` for a hit that lands later, such as a spell's bolt: the same modes and cover on the
context, nothing rolled. Roll it yourself with Critfall's `RollService.rollAttack` and land it on arrival. It is
empty where `attack` would miss with nothing rolled. Nothing is shown and a hidden target stays hidden.

`teleport` moves `entity` to `to`, as Misty Step does: it spends no movement and provokes no opportunity
attack, and a player moved off their turn is held where they land.

`launch` throws `target` with `motion`. The flight is not the target's own movement: it spends none
of its budget and provokes no opportunity attack, and a player thrown off their turn flies instead of
being pulled back, then is held where they land.

Use `showRoll` rather than reaching for Initiative's animation sync — it is what makes your roll
appear with the same drama as a built-in's, and it respects the player's animation config.

Per-encounter state of your own is your business: keep it in your own store, the way Initiative
keeps `dodging` and `hidden` in its own.

### `ActionResult`

```java
ActionResult.performed();               // it ran and landed
ActionResult.performed(won);            // it ran; `won` says whether it landed
ActionResult.attacked(attackResult);    // it ran; landed == the attack hit
ActionResult.rejected(status);          // it refused itself; nothing is charged
```

The distinction that matters: **`PERFORMED` always charges the cost, even when `succeeded` is
false.** A lost contest still spends your action — you committed it, the dice decided. Only
`rejected(...)` leaves the turn untouched, which is how Attack declines into a wall
(`TOTAL_COVER`) and Blink declines without a pearl (`MISSING_ITEM`) while keeping the action.

## The gate

Every invocation runs the same ordered checks. The first one that fails is the status you get back:

1. the action economy is on — else `INACTIVE`;
2. `enabled()` — else `DISABLED`;
3. the actor is in an encounter — else `NOT_IN_ENCOUNTER`;
4. `availableTo(actor)` — else `DISABLED`;
5. it is the actor's turn (skipped for `REACTION`) — else `NOT_YOUR_TURN`;
6. no hit on its way holds the fight (skipped for `REACTION`, see [Holding the fight for a
   hit](#holding-the-fight-for-a-hit)) — else `WAITING`;
7. the cost's resource is available — else `NO_ACTION` / `NO_BONUS_ACTION` / `NO_REACTION` /
   `NO_MOVEMENT`;
8. targeting is satisfied — else `INVALID_TARGET` / `OUT_OF_REACH`;
9. **your effect runs**;
10. the cost is charged, only if the result is `PERFORMED`.

Invoke one yourself with:

```java
ActionResult result = ActionRegistry.invoke(level, actor, TAUNT, ActionRequest.of(target));
```

`ActionRequest.none()`, `ActionRequest.of(entity)` and `ActionRequest.at(pos)` cover the three
targeting shapes.

To take an action for a different cost than its own, pass that cost:

```java
ActionRegistry.invoke(level, mob, ResourceLocation.fromNamespaceAndPath("initiative", "disengage"),
        ActionRequest.none(), ActionCost.BONUS_ACTION);
```

The gate then checks and charges the bonus action and leaves the action alone: Disengage or Hide as
a bonus action. The rest of the gate is unchanged.

To ask the gate without performing anything, for example before something you show first:

```java
Optional<ActionStatus> refusal =
        ActionRegistry.check(level, caster, CAST, ActionRequest.none(), ActionCost.BONUS_ACTION);
```

It runs steps 1 to 8 and answers with the status `invoke` would refuse with, or empty when it would
perform. Pass `null` for the action's own cost. Since 0.3.3.

## Holding the fight for a hit

Since 0.3.2. An action whose hit takes time to land, such as a spell's bolt, can hold the fight until it
does:

```java
Turns.holdUntil(caster, () -> bolt.landed());
```

While the hold lasts, the turn neither ends nor moves on: End Turn and every other action but a
reaction are refused with `WAITING` ("Wait for the hit to land"), a mob's own turn waits, and the turn
timeout stops. Movement goes on. The supplier is asked every tick and the hold ends once it answers
true, so make it true when the hit lands and when it is dropped. A participant in no fight holds
nothing. Several holds stack: the fight goes on when the last one ends.

## Turns and fights

Since 0.3.3:

```java
Turns.onTurnStart(participant -> ...);   // its turn starts, before it acts
Turns.onTurnEnd(participant -> ...);     // its turn passed on, or the fight is over
Encounters.inFight(entity);
Encounters.engage(attacker, target);
```

- **Turn listeners** hear each turn change once, the end of the old turn first. For what lasts some rounds,
  or happens at the start or end of a turn. A listener that throws is logged and skipped. Register during
  mod construction.
- **`engage`** is for a hurt Initiative can't see, such as damage dealt through Critfall's `RollService`, which
  never starts a fight: call it before the hurt, while both are alive, and a fight starts or takes them in by
  the rules a vanilla hit follows.

## Mob turns

Initiative's own mob turn is simple: the mob walks at the nearest enemy it can see and attacks it, or
shoots once with a bow, crossbow or trident. A mod that brings its own moves and the AI that picks
them takes over its mobs' turns with these. Register them during mod construction.

### Several attacks: `Attacks`

```java
Attacks.register(creature -> creature instanceof Bugbear ? 2 : 1);
```

How many attacks one Attack action makes for a creature: a mob's Multiattack, a player's Extra
Attack. With several sources the highest answer counts, never below 1; they don't add up. Initiative's
own mob turn makes all of them on its own, `mob_attack_spacing_ticks` apart so each roll shows. A
handler invokes `initiative:attack`, or its own `ATTACK` action, once per attack.

### A turn handler: `MobTurns.register`

```java
MobTurns.register(mob -> mob instanceof Bugbear, (mob, turn) -> {
    if (turn.ticks() == 0 && inReach(mob)) {
        ActionRegistry.invoke(level, mob, DROP_STRIKE, ActionRequest.of(target(mob)));
    }
    return done(mob, turn);
});
```

A mob the predicate matches takes its turns through the handler instead of Initiative's. The first
match wins. `tick` runs once per server tick of the mob's own turn; return true to end the turn.
`turn.ticks()` is how far into the turn it is, 0 on its first tick, and `turn.movementLeft()` the
blocks it can still move.

Initiative keeps the rest: the turn, its budget, the movement clamp, the freeze on everyone else's
turns. Every action the handler takes goes through `ActionRegistry.invoke` and the same gate as a
player's, so it can't spend what it hasn't got. Movement is the mob's own AI walking it, measured
against its budget like anyone's. A turn that never returns true ends on `mob_turn_timeout_ticks`. A
handler that throws is logged and its turn ends, so a broken mod can't stall a fight.
`mob_end_turn_when_spent` only governs Initiative's own turn. With `turns.custom_mob_turns` off,
every mob takes Initiative's turn.

An action that takes time (a leap, a wind up) is split: the effect resolves inside `perform()`, and
whatever takes ticks happens between actions, in the handler's ticks, as movement or as show. Give a
mob's own actions `availableTo` so they reach only that mob, never a player's bar or command.

A bonus action is a `BONUS_ACTION` action, or an existing one invoked with
`ActionCost.BONUS_ACTION`. An action out of turn is a `REACTION` action, which isn't turn gated:
invoke it from your mob's own code when its trigger fires. It spends the mob's reaction for the
round, and a second one that round is refused `NO_REACTION`.

Built in action ids a handler may invoke, all in the `initiative` namespace: `attack`, `dash`,
`disengage`, `dodge`, `help`, `hide`, `shove`, `grapple`, `end_turn`.

### Whether handlers run: `MobTurns.enabled()`

```java
boolean ownTurns = MobTurns.enabled();
```

Whether handlers will run: true only with `turns.enabled`, `turns.custom_mob_turns` and
`actions.enabled` all on. Otherwise no handler runs. It reads the live config, so ask in play, such
as on a mob's tick, not during mod construction, when Initiative may not have loaded its config yet.

### Hidden mobs: `joinsHidden`, `ambush`, `hidden`

```java
MobTurns.joinsHidden(mob -> mob instanceof Bugbear bugbear && bugbear.lurking());
boolean started = MobTurns.ambush(bugbear, player);
boolean stillHidden = MobTurns.hidden(bugbear);
```

A mob `joinsHidden` matches comes into a fight hidden, the way the Hide action hides: with Checks on a
Stealth roll whose total enemies' passive Perception is checked against, without as it stands. Only
while `actions.hide` and `actions.hide_suppresses_targeting` are on.

`ambush` lets the mob start the fight instead of attacking in real time: the fight forms at the
target with the mob hidden, and everyone on the other side who joins as it forms is surprised (see
[turns](turns.md#surprise)). It returns false when no fight was started (encounters or turns off,
the mob already fighting, the target a spectator or in creative); carry on in real time then. A
target already in a fight: the mob joins that fight hidden, nobody is surprised, and it returns true.

A hidden mob is left off the players' turn strip, its turn shows nobody acting, and it is not on the
players' target lists. A player can still swing at where it stands, with disadvantage, and a hit
finds it. Its own first attack has advantage and ends its hiding, and with Checks a player whose
passive Perception meets its Stealth finds it on that player's turn. `hidden` says whether it is
still hidden right now.

### Rider and mount

Nothing to register. A mount steered by a mob rider in the same fight acts on its rider's turn: see
[turns](turns.md#rider-and-mount).

## Datapack settings

Behaviour is code; numbers and the toggle are data. A registered action may be tuned from any
datapack at:

```
data/<namespace>/initiative/actions/<path>.json
```

so `examplemod:taunt` reads `data/examplemod/initiative/actions/taunt.json`:

```json
{
  "enabled": true,
  "reach_blocks": 4.0,
  "knockback_strength": 1.5
}
```

Read it with `context.settings()`, or bind your toggle with
`ActionSettings.enabled(TAUNT, true)`. Values reload with `/reload`. An action with no settings file
behaves exactly as its code declares — the file is optional throughout.

`ActionSettings.enabled` never throws: `enabled` is read on every gate check and every UI rebuild,
so a file whose `enabled` is not a boolean logs a warning and leaves the action at the fallback you
passed, rather than raising out of the server tick. Everything else in the file is yours to read and
yours to validate — `context.settings()` hands you the raw `JsonObject`, and an exception thrown
from your own `enabled()`, `cost()` or `perform()` will surface wherever Initiative called it.

Initiative's own actions read their numbers from `initiative.json` instead (see
[config](config.md)); the seam is the same, only the file differs.

## Commands

Every registered action gets a command under `/initiative`, generated from the registry:

- own-namespace actions use the bare path — `/initiative shove <target>`;
- anyone else's keeps the namespace — `/initiative examplemod:taunt <target>`;
- entity targeting adds an entity argument, position targeting a coordinate argument;
- `REACTION`-cost actions get no command — they are triggered, not typed.

Feedback goes to the action bar: `successKey()` when the effect landed, `failureKey()` when it did
not, and a shared line per rejection status otherwise. The commands stay the headless front end
now that the [turn UI](ui.md) has landed — both go through `ActionRegistry.invoke`.

## The turn UI

Your action gets a button on the acting player's bar for free, with no UI code of yours: the bar is
the registry, your `cost()` and `enabled()` decide whether it is offered or greyed, and your
`targeting()` decides what clicking it prompts for. Give it a label with the translation key
`action.<namespace>.<path>`; see [the turn UI](ui.md).

## What Initiative will not do for you

- No content. Classes, feats, spells and abilities belong in your mod or your pack.
- No trigger system yet — you cannot declare "run my action when X happens".
- No mob AI beyond its own simple turn: a mod's handler decides what its mobs do.
- No custom UI for your action: it gets the same button every built-in gets, not a widget of its own.
