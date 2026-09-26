# Roll presentation

The die you rolled, shown rolling. Every d20 Initiative drives is already decided and applied
server-side before anything is drawn — the animation is theatre played beside the resolution, never
in front of it. Turning it off changes nothing but what you see.

## What animates

| Roll | Trigger | Dice shown |
|---|---|---|
| Attack | the Attack action, a mob's turn attack, an opportunity attack | the attacker's die |
| Contest | Hide, Shove, grapple | both sides — the initiator and the opponent |
| Initiative | joining an encounter | the joiner's die |

Advantage and disadvantage show **two** dice: both naturals come from Critfall's `RollDetail`, the
kept one is drawn normally and the discarded one is greyed out. A normal roll shows one die.

Each die is a plain hexagon — the icosahedron silhouette — with its number and nothing else, so the
face stays readable at HUD size.

## Emphasis

A crit or a fumble gets a coloured border and a one-pixel jitter while it holds. For attacks the
flag follows Critfall's own `AttackOutcome`, so a house rule that widens the crit range stays
Critfall's call. Contests and initiative rolls have no Critfall outcome to read, so there the
die's own extremes — natural 20 and natural 1 — carry the drama.

## What it does not show

The modifier, the target's AC and the hit-or-miss verdict stay in Critfall's own text readout,
which since 0.2.6 also reports the roll mode and the cover AC split. Initiative draws dice and
nothing else, so the two never repeat each other.

## Reveal timing

Critfall resolves the attack and sends its action-bar readout in the same server tick the animation
starts, so left alone the text would announce `HIT — 7 damage` while the die was still cycling
faces. Initiative coordinates the reveal **on the client**: an action-bar line that arrives while
dice are tumbling is parked and re-shown the instant they settle (`hold_readout`, on by default).

Nothing server-side waits: damage, turn state and the readout packet are all unchanged and on time
— only the moment the client paints the line moves, by at most `tumble_ticks`. Critfall has no API
to defer or suppress its own readout, and the roll's own value cannot be known before it resolves,
so holding the line client-side is the only coordination point that keeps combat non-blocking. It
is implemented as a mixin on vanilla's `Gui.setOverlayMessage`, since neither loader has an event
for an action-bar message being set. Turn `hold_readout` off to get the old behaviour (text first,
dice still settling).

## One line at a time

Everything Initiative says about an action — `You slip out of sight`, `You shove them back`, and
rejections in red — goes to the **action bar**, the same single line Critfall's roll readout uses,
so each message replaces the last and chat stays empty. Every line is one short clause. Initiative
says nothing at all when an attack resolves: that line is Critfall's, and since 0.2.6 it already
carries the roll mode and the cover AC split.

The dice are drawn well above that line — vanilla puts the action bar at `guiHeight - 68`, so they
sit clear of it and of its backdrop, and text never lands on a die. (Critfall's optional *flavor*
line goes to chat, separately from the readout; it has its own toggle in Critfall's client config.)

## Who sees it

**Everyone in the encounter** (M9) — the communal tabletop moment. Players outside the encounter are
sent nothing, and a roll that belongs to no encounter renders only for the player at the exchange.

Because the audience is wider than the exchange, each side of the animation carries its roller's
name, drawn under that side's dice: a mob's opportunity attack against one player still reads to the
other. Turning `shared_visibility` off narrows the audience back to M6's — the roller, or, when a mob
rolled, the player it rolled against. See [multiplayer](multiplayer.md).

## Data flow

The value is read off the result Initiative already gets back from Critfall (`AttackResult.roll()`,
`ContestResult.initiatorRoll()`/`opponentRoll()`), packed into a `roll_animation` payload and sent
to each watching player. The client stores the newest roll, renders frames from it and forgets it
when the animation ends; a new roll restarts the animation.

## Config

```json
"roll_animation": {
  "enabled": true,
  "tumble_ticks": 10,
  "hold_ticks": 10,
  "face_change_ticks": 2,
  "hold_readout": true,
  "shared_visibility": true
}
```

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle. Off = nothing is sent and nothing is drawn; combat resolves exactly as before, text readout included. |
| `tumble_ticks` | integer ≥ 0 | How long the dice cycle faces before settling (default 10 = half a second). 0 settles instantly. |
| `hold_ticks` | integer ≥ 0 | How long the settled result stays on screen (default 10). |
| `face_change_ticks` | integer ≥ 1 | Ticks between face changes while tumbling (default 2). Higher is slower. |
| `hold_readout` | boolean | On (default): an action-bar readout arriving mid-tumble is held until the dice settle. Off: the text shows immediately and can spoil the roll. |
| `shared_visibility` | boolean | On (default): every player in the encounter watches every roll. Off: only the player at the exchange, as in M6. |

`tumble_ticks + hold_ticks` is the whole animation — one second on the defaults. The server decides
whether a roll is sent; the timings are read on the client, so each player can pace their own dice.
