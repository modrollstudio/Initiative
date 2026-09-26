# Multiplayer

Encounters with more than one human in them. Turns stay **sequential** — every participant, player
or mob, has its own slot in one initiative order. Side-based simultaneous player turns are not built.

## Shared roll visibility

Every player in an encounter watches every roll that encounter produces: attacks, contests and the
initiative d20s cast as participants join. That is the communal tabletop moment — the reason turn
order is worth having at all. Players **outside** the encounter are sent nothing.

Each side of the animation carries its roller's name, drawn under that side's dice, so a mob's
opportunity attack against one player still reads to the other. A contest names both sides; the
loser's name is drawn dimmer, matching the die.

Nothing about this is blocking: the roll is resolved and applied before a single packet is sent, so
the guarantee from M6 still holds — with `roll_animation.enabled` off, combat resolves identically.
`roll_animation.shared_visibility` off returns to the M6 audience: the roller, or, when a mob rolled,
the one player it rolled against.

## The turn timer

`turns.turn_timeout_ticks` (default 600 = 30 seconds) is the same clock for every participant, and
every client reads the same countdown off the server:

- the **acting** player sees it in the turn UI's status line, beside their budget;
- **every** player sees it on the turn-order strip, appended to the acting participant's cell — so a
  waiting player knows both whose turn it is and how long they have.

The countdown is quantised to whole seconds, so the strip resends at most once a second while the
turn state is otherwise unchanged.

When the timer runs out a player's turn ends exactly as a mob's does: the order advances, the next
participant's budget resets, and nobody is dropped.

## Disconnect and reconnect

A player who logs out is removed from their encounter **immediately** — not on the tick that would
notice their entity is gone. Removal is the one exit path every other departure uses, so it releases
Critfall's suppression, thaws any freeze, clears every per-encounter condition the player held
(dodge, disengage, hidden, a Help they granted, a grapple in either direction) and passes the turn on
if it was theirs. The encounter carries on for whoever is left, and ends normally if that leaves no
player or no hostile.

**Reconnecting puts you back in the world outside the encounter.** There is no half-state to restore:
the client's HUD, action bar and any pending targeting are cleared on logout, and a fresh attack
forms or joins an encounter again.

## Joining, and one encounter per entity

A player who attacks into an existing encounter **joins that encounter** — they do not open a second
one. They roll initiative on the spot and are inserted into the running order by that roll; a slot
that has already passed this round means they first act next round.

Encounters never merge, and **an entity belongs to exactly one encounter at a time**. A hostile
already fighting somewhere is not pulled into a second bubble, whether by a radius sweep or by
someone attacking it. It stays where it is.

## Sides and PvP

Sides are decided on join and are simply *player or not*: every player is `PLAYER`, every other
participant is `HOSTILE`. Two players in one encounter are therefore always **allies**.

PvP inside an encounter stays excluded, as it has been since M1, and this is what enforces it:

- ally-targeted actions (Help) reach another player;
- enemy-targeted actions (Attack, Shove, Grapple, Reel) refuse an ally as `INVALID_TARGET`;
- vanilla damage between two participants of the same encounter is cancelled, since Initiative is the
  sole combat driver inside the bubble.

Outside encounters PvP is untouched — Initiative never forms an encounter from a player hitting a
player.

## What is per player and what is shared

| Shared by the encounter | Per player |
|---|---|
| the initiative order and whose turn it is | the action bar and its buttons |
| the turn clock | the budget readout (only the acting player has a budget) |
| every roll animation | the targeting cursor and pending action |
| conditions (dodge, hidden, grapple, Help grants) | — |

Only the acting participant has a turn budget at all; a waiting player is sent an inactive snapshot
and their bar clears, so one player's spent action can never show on another's readout.

## AI freeze

Unchanged by player count: every mob participant except the acting one is frozen, and players are
never frozen. Two players taking turns in the same encounter leave the mobs frozen throughout both.

## Config

```json
"roll_animation": {
  "shared_visibility": true
}
```

| Field | Type | Meaning |
|---|---|---|
| `shared_visibility` | boolean | On (default): every player in the encounter watches every roll. Off: the M6 audience — the roller, or the one player a mob rolled against. |
