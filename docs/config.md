# Configuration

Initiative reads `config/initiative.json`. If the file is missing, it is created from the
built-in defaults; if it is not valid JSON at all, the built-in defaults are used and an error is
logged.

## How a file is read

Every setting is read on its own, against the built-in default for that one key. Nothing you have
set is discarded because of something else in the file:

| In the file | What happens |
|---|---|
| A key is absent | Its built-in default is used, and a warning names the key. This is what an update that adds a setting looks like: everything you did set is kept. |
| A whole group is absent, or is not a JSON object | Every key in that group takes its default, reported as one warning for the group. |
| A key holds a value that fails its check — wrong type, out of range, not finite | That key alone falls back to its default, and an error names the key and the reason. |
| Two keys contradict each other (`leave_radius` under `trigger_radius`, `half_cover_threshold` over `three_quarter_cover_threshold`, `break_distance_blocks` under `reach_blocks`) | Both fall back together, since only the built-in pair is known to agree. |
| A key Initiative does not have | It is ignored with a warning. A typo and a setting from a newer version look the same from here. |

Numbers are still range-checked, and decimals must still be finite: a literal too large for a
double, such as `1e400`, is refused rather than becoming an infinite radius or budget. What changed
is the blast radius — one bad line costs one setting.

The file is **never rewritten**. Keys added by an update do not appear in your `initiative.json`;
they take their defaults in memory and the log says which. Copy them in from the example below if
you want to change them.

Example (see [examples/initiative.json](examples/initiative.json)):

```json
{
  "format_version": 1,
  "encounters": {
    "enabled": true,
    "trigger_on_player_attacking": true,
    "trigger_on_player_attacked": true,
    "trigger_on_provoked_neutral": true,
    "hold_ranged_attackers": true,
    "pull_nearby_players": true,
    "trigger_radius": 12.0,
    "leave_radius": 20.0
  },
  "turns": {
    "enabled": true,
    "turn_timeout_ticks": 600,
    "mob_turn_timeout_ticks": 120,
    "initiative_bonus_per_speed": 20.0,
    "initiative_max_bonus": 12,
    "freeze_enabled": true,
    "acting_marker_enabled": true,
    "restrict_players_off_turn": true,
    "no_freeze_types": []
  },
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
    "hide_suppresses_targeting": true,
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
  },
  "cover": {
    "enabled": true,
    "half_cover_ac_bonus": 2,
    "three_quarter_cover_ac_bonus": 5,
    "half_cover_threshold": 0.5,
    "three_quarter_cover_threshold": 0.75,
    "total_cover_blocks_attack": true,
    "block_placement_costs_movement": true,
    "block_placement_movement_cost": 1.0
  },
  "grapple": {
    "enabled": true,
    "reach_blocks": 3.0,
    "attacker_bonus": 0,
    "defender_bonus": 0,
    "break_distance_blocks": 5.0,
    "escape": true,
    "escape_attacker_bonus": 0,
    "escape_defender_bonus": 0
  },
  "hud": {
    "enabled": true
  },
  "action_ui": {
    "enabled": true
  },
  "roll_animation": {
    "enabled": true,
    "tumble_ticks": 10,
    "hold_ticks": 10,
    "face_change_ticks": 2,
    "hold_readout": true,
    "shared_visibility": true
  },
  "checks": {
    "enabled": true
  }
}
```

## `format_version`

| Field | Type | Meaning |
|---|---|---|
| `format_version` | integer | The config layout this file was written for. Optional: a file without it is read exactly like one that declares the current version. A file declaring a version **newer** than the running build is still read key by key, with a warning that settings this build does not have are being ignored. |

## `encounters`

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for encounter bubbles. Off = Critfall's real-time combat everywhere. |
| `trigger_on_player_attacking` | boolean | A player attacking a hostile starts an encounter. |
| `trigger_on_player_attacked` | boolean | A hostile attacking a player starts an encounter. |
| `trigger_on_provoked_neutral` | boolean | A neutral mob that turns hostile toward a player starts or joins an encounter, and leaves again when it calms down. Off restores `Enemy`-only triggering. See [encounters](encounters.md#when-a-neutral-mob-counts-as-hostile). |
| `hold_ranged_attackers` | boolean | A participant that shot its way into the fight is not measured against `leave_radius` while it still has someone in the fight in its sights. Off restores the plain distance rule, which drops a shooter that stands outside the leave radius a tick after each hit. See [encounters](encounters.md#shooting-into-a-fight-from-outside). |
| `pull_nearby_players` | boolean | Players within `trigger_radius` join the encounter when it forms, and players who walk in later join mid-encounter, rolling initiative either way. Spectator and creative players are never pulled in. Off: a player joins only by attacking or being attacked. See [encounters](encounters.md#lifecycle). |
| `trigger_radius` | number > 0 | Blocks around the trigger point; hostiles (and, with `pull_nearby_players`, players) inside are pulled into the encounter, and those entering later join. |
| `leave_radius` | number ≥ `trigger_radius` | Participants farther than this from the encounter center leave the encounter — unless `hold_ranged_attackers` is holding a shooter in. |

## `turns`

See [turns](turns.md) for the mechanics.

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for initiative and turn order. Off = plain M1 encounter bubbles. |
| `turn_timeout_ticks` | integer > 0 | Server ticks after which a **player's** turn ends on its own (default 600 = 30 seconds). It is a stall-breaker, not the way a turn is meant to end: a decisive player clicks End Turn long before it, so the generosity only costs the table when somebody has walked away. |
| `mob_turn_timeout_ticks` | integer > 0 | The same for a **mob's** turn (default 120 = 6 seconds). A mob acts instantly through its AI and ends its turn on the spot, so this is only the wait for one that cannot act at all. |
| `initiative_bonus_per_speed` | number ≥ 0 | Initiative bonus per point of movement-speed attribute. Unused while [Checks](#checks) supplies Dexterity. |
| `initiative_max_bonus` | integer ≥ 0 | Upper clamp for the derived initiative bonus. |
| `freeze_enabled` | boolean | Feature toggle for the AI freeze. Off = turn order still runs but mobs act in real time (M2a behavior). Turning it off mid-game thaws everything within a tick. |
| `acting_marker_enabled` | boolean | Debug-grade particle marker above the entity whose turn it is, for playtest verification. Not the turn HUD. |
| `restrict_players_off_turn` | boolean | Feature toggle for the off-turn hold on players (see [turns](turns.md#holding-players-off-turn)). Off = a participant player moves, builds and interacts freely while somebody else acts. Turning it off mid-fight releases every held player within a tick. |
| `no_freeze_types` | array of entity type ids | Entity types never frozen (see [turns](turns.md#excluding-entity-types)). Same escape hatch as the `#initiative:no_freeze` tag, without needing a datapack. |

## `actions`

See [actions](actions.md) for the mechanics.

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for the M3 action economy. Off = M2 behavior: turns are plain real-time windows, no budgets, no attack interception. |
| `movement_budget_blocks` | number ≥ 0 | Blocks of movement per turn (default 6 ≈ 30 ft). |
| `single_attack_per_turn` | boolean | Attacking consumes the turn's one action. Off = attacks bypass the action. |
| `attack_reach_blocks` | number ≥ 0 | How far an attack reaches (horizontal centre-to-centre, default 4 ≈ a melee swing). Beyond it the attack is `OUT_OF_REACH` and the [turn UI](ui.md) offers no such target. |
| `end_turn_when_spent` | boolean | A player's turn auto-ends once its action is used and its movement is exhausted. Default off since M8: the player ends the turn with the UI's End Turn button. |
| `mob_end_turn_when_spent` | boolean | A mob's turn auto-ends once its action is used. Default on — a mob has no [turn UI](ui.md) to end its turn with. |
| `dash` | boolean | Feature toggle for the Dash action (M4a). Off = Dash is unavailable; the other actions are unaffected. |
| `disengage` | boolean | Feature toggle for the Disengage action (M4a). Off = Disengage is unavailable. |
| `dodge` | boolean | Feature toggle for the Dodge action (M4a). Off = Dodge is unavailable and no attack is rolled with dodge disadvantage. |
| `help` | boolean | Feature toggle for the Help action (M4a). Off = Help is unavailable and no attack is rolled with help advantage. |
| `hide` | boolean | Feature toggle for the Hide action (M4b). Off = Hide is unavailable. |
| `opportunity_attack` | boolean | Feature toggle for opportunity attacks (M4b). Off = opportunity attacks are unavailable. |
| `opportunity_attack_reach_blocks` | number ≥ 0 | Horizontal distance (blocks) at which a hostile provokes opportunity attacks (default 3 ≈ 15 ft). |
| `hide_observer_perception_bonus` | integer | Flat bonus added to the nearest hostile observer's roll in the Hide contest (default 0). Unused while [Checks](#checks) drives Hide. |
| `hide_stealth_bonus` | integer | Bonus added to your Stealth check roll (`d20 + hide_stealth_bonus`, default 0). |
| `hide_suppresses_targeting` | boolean | Whether being hidden also hides you from mob AI (default on). Off = Hide keeps its roll effects only, and mobs see, path to and target a hidden participant exactly as before. |
| `shove` | boolean | Feature toggle for the Shove action (M5a). Off = Shove is unavailable. |
| `shove_reach_blocks` | number ≥ 0 | Horizontal distance at which you can shove a target (default 3). |
| `shove_knockback_strength` | number ≥ 0 | Vanilla knockback impulse applied on a won shove (default 1.0). |
| `shove_attacker_bonus` | integer | Flat bonus on your Shove contest roll (default 0). Unused while [Checks](#checks) drives Shove. |
| `shove_defender_bonus` | integer | Flat bonus on the target's Shove contest roll (default 0). Unused while Checks drives Shove. |
| `ender_pearl_blink` | boolean | Feature toggle for the ender-pearl blink (M5a). Off = unavailable. |
| `blink_max_blocks` | number ≥ 0 | Max horizontal teleport distance for the blink (default 8). |
| `fishing_rod_reel` | boolean | Feature toggle for the Reel action, the fishing-rod pull (M5a; called `fishing_rod_grapple` before M8). Off = unavailable. |
| `reel_pull_strength` | number ≥ 0 | Vanilla knockback impulse pulling the target toward you on a won reel (default 1.0). |
| `reel_attacker_bonus` | integer | Flat bonus on your Reel contest roll (default 0). Placeholder. |
| `reel_defender_bonus` | integer | Flat bonus on the target's Reel contest roll (default 0). Placeholder. |

## `cover`

Line-of-sight cover and the build economy that feeds it (M5b). See
[actions](actions.md#cover) for the raycast, tiers and total-cover rule.

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for positional cover. Off = driven attacks behave exactly as pre-M5b (no rays, no bonus). |
| `half_cover_ac_bonus` | integer ≥ 0 | Defender AC bonus for half cover (default 2). |
| `three_quarter_cover_ac_bonus` | integer ≥ 0 | Defender AC bonus for three-quarters cover, and the bonus total cover degrades to when it does not block (default 5). |
| `half_cover_threshold` | number in [0, 1] | Obstructed-ray fraction at or above which cover is at least half (default 0.5). Must not exceed `three_quarter_cover_threshold`. |
| `three_quarter_cover_threshold` | number in [0, 1] | Obstructed-ray fraction at or above which cover is three-quarters (default 0.75). |
| `total_cover_blocks_attack` | boolean | On (default): a fully-obstructed attack is rejected (`TOTAL_COVER`) and spends nothing. Off: total cover degrades to the three-quarter bonus and the attack proceeds. |
| `block_placement_costs_movement` | boolean | On (default): placing a block on your turn spends movement. Off: placement is free (pre-M5b). |
| `block_placement_movement_cost` | number ≥ 0 | Movement spent per block placed on your turn (default 1.0). |

## `grapple`

The unarmed grapple and the escape from it (M8). See [actions](actions.md#grapple-and-escape).

```json
"grapple": {
  "enabled": true,
  "reach_blocks": 3.0,
  "attacker_bonus": 0,
  "defender_bonus": 0,
  "break_distance_blocks": 5.0,
  "escape": true,
  "escape_attacker_bonus": 0,
  "escape_defender_bonus": 0
}
```

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for the Grapple action. Off = Grapple is unavailable; nothing else changes. |
| `reach_blocks` | number ≥ 0 | Horizontal distance at which you can take hold (default 3). |
| `attacker_bonus` | integer | Flat bonus on your grapple contest roll (default 0). Unused while [Checks](#checks) drives Grapple. |
| `defender_bonus` | integer | Flat bonus on the target's grapple contest roll (default 0). Unused while Checks drives Grapple. |
| `break_distance_blocks` | number ≥ `reach_blocks` | The hold breaks once the two are further apart than this (default 5). |
| `escape` | boolean | Feature toggle for the Escape action. Off = a hold can only end by breaking or by a side leaving. |
| `escape_attacker_bonus` | integer | Flat bonus on the escaping participant's contest roll (default 0). Unused while Checks drives Escape. |
| `escape_defender_bonus` | integer | Flat bonus on the grappler's contest roll (default 0). Unused while Checks drives Escape. |

## `hud`

See [turns](turns.md#turn-order-hud) for what the HUD shows.

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for the turn-order HUD sync. Off = no snapshots are sent and clients render nothing, restoring M2b behavior. Display only — turn state is unaffected either way. |

## `action_ui`

See [the turn UI](ui.md) for the bar, targeting and End Turn.

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for the in-combat action UI (M8). Off = no snapshots are sent, clicks are refused server-side, and `/initiative <action>` is the only front end. |

## `roll_animation`

See [rolls](rolls.md) for what animates and who sees it.

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | Feature toggle for the d20 roll animation (M6). Off = nothing is sent and nothing is drawn; rolls resolve identically. |
| `tumble_ticks` | integer ≥ 0 | Ticks the dice cycle faces before settling on the real result (default 10). |
| `hold_ticks` | integer ≥ 0 | Ticks the settled result stays on screen (default 10). |
| `face_change_ticks` | integer ≥ 1 | Ticks between face changes while tumbling (default 2). |
| `hold_readout` | boolean | On (default): an action-bar readout landing mid-tumble is held client-side until the dice settle, so the text cannot spoil the roll. Off = readout shows immediately. |
| `shared_visibility` | boolean | On (default, M9): every player in the encounter watches every roll, each side of the animation naming its roller. Off = the M6 audience, the one player at the exchange. |

## `checks`

The optional [Critfall: Checks](actions.md#with-checks-installed) integration.

| Field | Type | Meaning |
|---|---|---|
| `enabled` | boolean | On (default): with Checks installed, Shove, Grapple, Escape and Hide roll the participants' own skills through Checks instead of the flat config bonuses, and initiative adds Dexterity instead of the movement-speed bonus. Off, or without Checks installed: the flat bonuses and movement-speed initiative, exactly as before. |

The checks each value must pass: `trigger_radius` must be positive and `leave_radius` at least
`trigger_radius`; both turn timeouts must be positive and the two initiative values must not be
negative; `movement_budget_blocks`, the reach and strength values, the cover AC bonuses and
`block_placement_movement_cost` must not be negative; the two cover thresholds must be within
`[0, 1]` with `half_cover_threshold` not exceeding `three_quarter_cover_threshold`;
`break_distance_blocks` must be at least `reach_blocks`; the roll-animation tick counts must not be
negative and `face_change_ticks` must be at least 1; every `no_freeze_types` entry must be a valid
entity type id. A value that fails takes the built-in default for that key, as described in
[how a file is read](#how-a-file-is-read).
