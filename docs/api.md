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
Optional<JsonObject> settings();         // this action's datapack file
```

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
6. the cost's resource is available — else `NO_ACTION` / `NO_BONUS_ACTION` / `NO_REACTION` /
   `NO_MOVEMENT`;
7. targeting is satisfied — else `INVALID_TARGET` / `OUT_OF_REACH`;
8. **your effect runs**;
9. the cost is charged, only if the result is `PERFORMED`.

Invoke one yourself with:

```java
ActionResult result = ActionRegistry.invoke(level, actor, TAUNT, ActionRequest.of(target));
```

`ActionRequest.none()`, `ActionRequest.of(entity)` and `ActionRequest.at(pos)` cover the three
targeting shapes.

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
- No custom UI for your action: it gets the same button every built-in gets, not a widget of its own.
