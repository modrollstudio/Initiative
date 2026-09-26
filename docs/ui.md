# The turn UI

On your turn Initiative shows a bar of buttons — one per action you can take — with what is left of
your turn above it and an End Turn button in it. The bar is built from the
[action registry](api.md): whatever is registered and enabled appears, whoever registered it, and a
mod's action is clicked exactly like Attack is.

Behind the flag `action_ui.enabled` (default on). Off restores the M7 flow exactly: no bar, no
clicks accepted, `/initiative <action>` only.

## What is on screen

- **The buttons.** One per registered action that exists in this world, in registration order.
  Reactions are never shown — they are triggered, not clicked.
- **Greyed buttons.** A button is greyed with the reason the server would refuse it: your action is
  spent, nothing is in reach, the item is missing. The greyed reason is literally the status
  `ActionRegistry.invoke` would return, so what the bar says and what a click does can never
  disagree.
- **Absent buttons.** An action turned off in config or by a datapack is not greyed, it is gone: it
  does not exist in this world. So is one that does not apply to you at all — Escape is only ever on
  the bar of a participant that is actually grappled, because the action declares that with
  `availableTo`.
- **The budget line.** Action / Bonus / Reaction in green while unspent, grey once spent, plus
  movement left over the turn's budget.
- **The clock.** The `turns.turn_timeout_ticks` timeout counting down in seconds. It is a safety net
  so an away player cannot stall the table — not how a turn is meant to end.
- **End Turn.** `initiative:end_turn` is an ordinary registered action, so it sits in the bar like
  the rest, in its own colour.

## Clicking

Press the action-panel key (default **R**) to free the cursor and click a button. The panel does not
pause the game or dim the world — combat behind it carries on.

What a click does next is read from the action's declared `targeting()`, never from a list of known
actions:

| Targeting | What happens |
|---|---|
| `none`, `self` | Runs immediately. |
| `entity` | The panel closes and you enter targeting: look at a target, **left click** to confirm, **right click** to cancel. Only the targets the server offered are pickable, so anything you can aim at is one the gate accepts. |
| `position` | The panel closes and you aim at a block; **left click** confirms, **right click** cancels. |

While targeting, the attack and use clicks belong to the UI and never swing or place a block. The
prompt above the bar names the target you are on, in green, or tells you to keep looking.

Every click goes to the server as an invocation request and runs through the same
`ActionRegistry.invoke` the command does. The client decides nothing: it can only ask.

## Turn end and auto-end

With an explicit End Turn button, ending a turn is a decision, not something that happens to you.
So `actions.end_turn_when_spent` — which used to end a player's turn the moment its action and
movement were gone — now **defaults off**, and a turn you have spent stays yours until you end it.
Turn it back on if you prefer the old convenience.

Mobs have no bar to click, so `actions.mob_end_turn_when_spent` (default **on**) keeps ending their
turn once their action is spent. Turning it off leaves mobs sitting out the timeout, which is
rarely what you want.

## Naming an action in the UI

The label on a button is the translation key `action.<namespace>.<path>` — so `initiative:shove` is
`action.initiative.shove`, and a pack's `examplemod:taunt` is `action.examplemod.taunt`:

```json
{
  "action.examplemod.taunt": "Taunt"
}
```

Without a lang entry the button falls back to the id's path with underscores as spaces, so an
unlabelled action is still usable — just plain.

## Multiplayer

Each player's bar is built and sent for that player alone; a player who is not acting is sent an
empty one and their bar clears, so one player's spent action never shows on another's readout. The
click path is server-authoritative, and a waiting player's click is refused as `NOT_YOUR_TURN`.

Only the acting player's bar carries the turn clock — everyone else reads it off the turn-order
strip. The rest of the multiplayer rules (shared rolls, disconnects, sides) are in
[multiplayer](multiplayer.md).

## Config

```json
"action_ui": {
  "enabled": true
}
```

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for the turn UI. Off = no snapshots are sent, clicks are refused, and `/initiative <action>` is the only front end (the pre-M8 behaviour). |

The UI also stands down whenever `turns.enabled` or `actions.enabled` is off — there is no turn to
show actions for.
