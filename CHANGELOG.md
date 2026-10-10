# Changelog

## [0.2.0] - Unreleased

- **Shove, Grapple, Escape and Hide roll real skills with Critfall: Checks.** Their contests used
  flat config bonuses, the same for every participant. With Checks 0.1.0 or newer installed they now
  roll each side's own skills through it. Shove and Grapple are your Athletics against the target's
  Athletics or Acrobatics, whichever is higher. Escape is your Athletics or Acrobatics, whichever is
  higher, against the grappler's Athletics. Hide is no longer a contest: you roll Stealth, you are
  hidden, and the total is kept. On each enemy's turn its passive Perception is checked against that
  total, and if it meets or beats it you are revealed. Attacking, being hit and leaving still reveal
  you as before. The Stealth roll gets its own check animation, showing both dice when Checks rolls
  it with advantage or disadvantage, and the contests animate as before. If another mod cancels one
  of these rolls, the action fails without showing any dice. Checks stays optional: without it,
  nothing changes and none of its classes load. New `checks.enabled` (default on); off restores the
  flat bonuses even with Checks installed. Reel keeps its flat bonuses.

- **Initiative adds your Dexterity with Critfall: Checks.** Everyone's initiative bonus came from
  their movement speed. With Checks installed it is now each participant's Dexterity modifier, so a
  clumsy creature can roll below its d20. Without Checks, or with `checks.enabled` off, initiative
  still uses movement speed.

- **Minecraft 1.21.1 only.** Both loaders now require exactly Minecraft 1.21.1, the only version
  Initiative is built and tested on; the Fabric jar used to accept any later 1.21 release.

- **Requires Critfall 0.2.10 or newer**, up from 0.2.6: the same minimum Critfall: Checks requires.

## [0.1.1] - 2026-09-27

- **Contested roll names no longer run together.** In a contested roll each side's name is centered
  under its dice, so two long names met in the gap between the sides and read as one word. A long
  name is now pushed outward instead of past the middle of that gap, keeping the two names apart.

## [0.1.0] - 2026-09-26

- **First public release.** The loader jars are versioned `0.1.0+neoforge` and `0.1.0+fabric`, so
  each loader's Modrinth upload has its own version number and maven coordinate. Both loaders'
  metadata now link the source repository and issue tracker at
  [modrollstudio/Initiative](https://github.com/modrollstudio/Initiative). The release jars no
  longer carry GameTest registration or its structure template, so another mod's GameTest run with
  Initiative installed no longer fails on missing test bodies; the build now refuses a jar that does.

- **The whole party rolls initiative.** When one player started a fight, a friend standing a block
  away stayed in real time and never joined: players only entered an encounter by attacking or being
  attacked, so a group had to take turns hitting the mob before the table was complete. Every player
  within `trigger_radius` of the fight now joins it as it forms and rolls initiative, and a player
  who walks into a running encounter joins mid-fight, rolled into the order the same way a late
  hostile is. Spectator and creative players are never pulled in, and a player already fighting
  elsewhere stays there. New `encounters.pull_nearby_players` (default on); off restores joining by
  attack only.

- **Skeletons can no longer plink at you from outside the fight.** A melee mob has to walk into the
  encounter to reach you, and walking in is what puts it in the turn order; an archer never has to.
  Its first arrow did pull it in — the shot's owner is the attacker Critfall reports — but the
  encounter dropped it again a tick later for standing outside `leave_radius`, so it never took a
  turn and every arrow after that was another real-time hit while you waited for yours. A
  participant that shot its way into a fight is now held in it: the distance leave path no longer
  applies to it while it still has someone in the fight in its sights, so it rolls initiative,
  freezes off-turn and shoots on its turn like everybody else. Losing its target hands it back to
  the leave radius, as do death, unloading and dimension change. A shot into somebody else's fight
  holds nobody, an arrow with no living shooter behind it (a dispenser's, or one whose owner died
  in flight) changes nothing, and PvP stays excluded. New `encounters.hold_ranged_attackers`
  (default on); off restores the plain distance rule exactly.

- **Hiding now actually hides you from mobs.** Hide used to change only the dice — advantage on your
  next attack, disadvantage on attacks against you — while every mob went on seeing you, pathing to
  you and hitting you, which made the action feel like it did nothing. A hidden participant can no
  longer be targeted: a mob trying to acquire you is refused, and a mob that already had you loses
  you the moment you hide, so it stops chasing and stops swinging and burns its turn finding nobody.
  It does not walk to your last known position — a mob with no target idles, and faking that memory
  would mean writing AI. The moment hidden ends (you attack, you are hit, you leave, the fight ends)
  targeting is back to normal on the next tick; the state is a server-side set that no restart can
  leave behind. Covers every mob that sets its target the vanilla way, modded custom goals included,
  plus brain-driven mobs (piglin, hoglin, warden) on NeoForge; on Fabric those brain mobs are
  cleared each tick rather than refused, so they can flicker back for a tick, and fully custom AI
  that never sets a target is not covered at all. A former neutral is no longer dropped from the
  fight for losing a target that hiding took from it. New `actions.hide_suppresses_targeting`
  (default on); off restores the previous Hide exactly, roll effects only.

- **Provoking a neutral mob now starts initiative.** Hit a panda, a wolf, a bee or an iron golem
  and it turns on you — but until now nothing turned turn-based, because only always-hostile mobs
  could start an encounter and that check had already passed at the moment of the hit. You ended up
  fighting an angry panda in real time while the mod looked broken. The trigger is now the mob
  turning hostile rather than the mob being hit: the moment it has you as its attack target, or is
  angry at you, it forms an encounter or joins the one you are in, and fights through the turn
  order like anything else. Mobs that never turn hostile are untouched — hitting a cow, a sheep or
  a chicken stays real-time, so farming never drags you into a duel with livestock. A former
  neutral whose anger runs out leaves the encounter cleanly, the same way fleeing the bubble does.
  Once you are in a fight you can still swing at the neutral mobs that are not in it: only
  participants can be attacked through the turn economy, so a swing at an unclaimed neutral falls
  back to a plain vanilla hit instead of being refused — otherwise the hit that provokes a second
  panda could never land, and you could not touch a cow until the fight ended. Nothing is driven
  and no action is spent by that swing, and the turn gate still applies, so it is not a way to act
  out of turn. How hostile mobs trigger has not changed. New
  `encounters.trigger_on_provoked_neutral` (default on); off restores `Enemy`-only triggering, and
  the old targeting gate, within a tick.

- A behaviour-preserving clean-code pass ahead of release. Nothing a player or a datapack can
  observe changes. Internally: the second type named `ActionStatus` is gone — `ActionEconomy`'s
  coarse standard-action result is now `ActionEconomy.ActionOutcome`, so `ActionStatus` always means
  the public API enum; the builder's `Action.BuiltAction` record moved off the public `Action`
  interface and is now package-private, leaving only the intended surface; the dead
  `TurnBudget.tryUseAttack` (superseded when the attack's cost moved into the action pipeline) and
  its two unit tests are removed; `package-info.java` files describe the architecture and the action
  API for a first-time reader; and comments that had gone stale — a turn HUD "that comes later", a
  bonus action "nothing consumes yet", commands as a "stand-in until the turn UI arrives" — now say
  what the code does.

- The GameTest suites run in their own game directory (`run-gametest/`) on both loaders, so mods
  dropped into the playtest `run/mods` for a session no longer load into the tests. A mob-behaviour
  mod left there was knocking the mock players around, charging the movement they were pushed
  against their turn budget and failing the movement, dash and turn-bar tests on roughly one run in
  three.

- **You can no longer run around during someone else's turn.** A player who is in an encounter but
  not acting is held still: no walking, no breaking or placing blocks, no using items, no
  interacting with blocks or entities (attacking was already refused). It is what the turn UI has
  shown since M8, now true of the world as well. The hold is a `-100%` movement-speed modifier the
  client itself obeys — so nothing fights your movement packets and there is no rubberbanding —
  plus a server-side pull-back for anything the attribute misses, such as jump momentum or
  knockback; falling stays free, but standing in water or lava no longer means sinking through the
  fight — in a fluid the hold keeps your height too. Your camera is untouched: vanilla scales the
  field of view by movement speed, which would have zoomed a held player all the way in, so the
  view is pinned to
  the one you have standing still. Players not in the encounter are untouched, and on your own turn
  everything works as before. Both halves are transient server state that no crash or restart can
  leave behind, and every exit path releases: the turn arriving, the encounter ending, death,
  disconnect, leaving the bubble, unload, server stop. New `turns.restrict_players_off_turn`
  (default on); off restores the old free-roaming behavior within a tick.

- **A more generous turn clock.** `turns.turn_timeout_ticks` now defaults to 600 ticks (30 seconds)
  instead of 300, and `turns.mob_turn_timeout_ticks` to 120 (6 seconds) instead of 40. Since M8 a
  turn ends by clicking End Turn, so the clock only ever catches a player who is not deciding at
  all — there is nothing to gain from hurrying one who is. Mobs act instantly through their AI and
  end their turn on the spot, so their timeout only covers a mob that cannot act.

- **An update no longer resets your config.** `initiative.json` is read key by key against the
  built-in defaults instead of all-or-nothing, so a file written against an older Initiative keeps
  every value it does specify. A key the file does not have takes its default and is named in the
  log; a key Initiative does not have is ignored with a warning; a value that fails its check —
  wrong type, out of range, or not finite — costs that one setting and nothing else. Two settings
  that contradict each other (`leave_radius` under `trigger_radius`, the two cover thresholds
  crossed, `break_distance_blocks` under `reach_blocks`) fall back as a pair, since only the
  built-in pair is known to agree. Range checks and the audit's finite guard are unchanged; only the
  blast radius of a failure is. The file is never rewritten, so a downgrade cannot strip keys a
  newer build wrote. New optional `format_version`: a file that declares one newer than the running
  build still loads, with a warning that settings this build does not have are ignored.
  Closes the audit's B6, which shipped this problem twice — M9 added
  `turns.mob_turn_timeout_ticks` and `roll_animation.shared_visibility`, and both silently reverted
  every other setting in the file.

- **Pre-release hardening.** A full audit of leaked state, crash vectors, malformed input, thread
  safety and per-tick cost, written up in [docs/audit-pre-release.md](docs/audit-pre-release.md).
  No gameplay or balance value changed. The audit found no state leak — every per-entity and
  per-encounter collection is evicted on death, unload, dimension change, disconnect, encounter end
  and server stop — and confirmed both loaders' packet handlers run on the server thread. What it
  did find:

  - **A malformed action-settings datapack can no longer crash the server.**
    `ActionSettings.enabled` is read once per tick while its owner is acting, and an `enabled` value
    that was not a boolean — `null` is the easy typo — threw out of the level tick. It now logs and
    leaves the action at the fallback its author declared.
  - **Config numbers must be finite.** Every range check in the config records is one-sided, so a
    literal too large for a double (`1e400`, an extra row of zeroes) overflowed to infinity and was
    accepted, giving the encounter scan an infinite box and the turn a budget no movement could
    exhaust. Decimal config values are now rejected unless finite.
  - **The action-invoke packet's position is validated.** A crafted packet could carry `NaN` or
    infinite coordinates straight through to a position-targeted action; a non-finite position is
    now treated as no position at all.
  - **The AI-freeze set is concurrent.** It is read from every mob's AI step server-wide, and a
    plain `HashSet` gave no guarantee against a mod that ticks entities off the main thread.
  - **The documented config examples were stale** — both `docs/examples/initiative.json` and the
    inline block in `docs/config.md` were missing keys, and config parsing was strict at the time,
    so a user who copied either had their whole config rejected. Both now match the built-in
    defaults exactly, and a test loads the example file and fails if it ever drifts again.

  Findings not fixed, with reasons, are in [docs/deferred-issues.md](docs/deferred-issues.md).

- **Critfall is resolved from the Modrinth maven.** `https://api.modrinth.com/maven` is now a
  declared repository and the dependency reads `maven.modrinth:critfall` — the same artifacts a
  player's launcher installs — instead of a `studio.modroll` build published to the local Maven
  repo. Critfall uploads a separate Modrinth version per loader under one version number, so the
  NeoForge jar is `maven.modrinth:critfall:${critfall_version}` while the Fabric jar is only
  addressable by its Modrinth version id, pinned as `critfall_fabric_modrinth_id`.
  `mavenLocal()` stays declared as a fallback. Critfall
  ships no loader-agnostic artifact on Modrinth, so the `common` module compiles against the
  Mojang-mapped NeoForge jar, which bundles the whole `studio.modroll.critfall.api` package. No
  build step outside this repo is needed any more.

- **M9: multiplayer.** Encounters now work properly with several humans in them. Turns stay
  sequential — side-based simultaneous player turns are not built.

  - **Every player in the encounter watches every roll.** Attacks, contests and the initiative d20s
    are broadcast to all player participants rather than the one at the exchange; players outside the
    encounter are sent nothing. Each side of the animation names its roller, drawn under that side's
    dice, so a mob's opportunity attack against one player reads to the other. New
    `roll_animation.shared_visibility` (default on); off restores the M6 audience exactly.
  - **The turn clock is on the turn-order strip.** The acting participant's cell carries the
    countdown, so a waiting player sees whose turn it is and how long they have — the acting player's
    own bar is unchanged. The strip resends at most once a second while nothing else changes.
  - **A disconnect no longer waits a tick.** Logging out removes the player from their encounter
    immediately through the one shared exit path: suppression released, freeze thawed, every
    condition they held dropped (including a grapple in either direction) and the turn passed on.
    Reconnecting puts you back outside the encounter — the client's HUD, bar and targeting are
    already cleared on logout, so there is no half-state.
  - **One encounter per entity.** A hostile already fighting is no longer pulled into a second
    encounter by someone attacking it from another one; encounters never merge. A player attacking
    into an existing encounter joins it and is inserted into the running order by their own roll.
  - **PvP inside an encounter stays excluded**, now documented as the rule it always was: both
    players are on the `PLAYER` side, so Help reaches an ally while Attack, Shove, Grapple and Reel
    refuse one, and vanilla damage between participants is cancelled.
  - **The turn timeout is now split by participant type.** `turns.turn_timeout_ticks` (default 300 =
    15s) applies to players only; new `turns.mob_turn_timeout_ticks` (default 40 = 2s) is a
    stall-breaker for mobs. A mob with an action to take already ends its turn the moment it spends
    it, so the mob timeout only ever covers one that *cannot* act — and the table no longer waits a
    player's length for it. The countdown the clients display is read off the same rule, so the timer
    that fires and the timer on screen are always the same number.
  - New [docs/multiplayer.md](docs/multiplayer.md).
  - 12 new GameTests per loader (136 total, both loaders), including the cross-encounter check that a
    mob freed when a disconnect ends its encounter can go on to join another player's.

- **M8 fixes: attack reach and a real Grapple.** Two corrections from playtesting M8.

  - **Attack now has a reach.** `actions.attack_reach_blocks` (default 4, horizontal
    centre-to-centre, the same rule Shove and the opportunity attack use) gates every driven attack:
    a target further away is refused as `OUT_OF_REACH` without a roll, and the turn UI does not offer
    it at all. A melee swing no longer reaches an enderman across the field.
  - **The fishing-rod pull is now `initiative:reel`.** Same behaviour — rod required, contested, drags
    the target toward you, one durability — under its own name and its own config keys
    (`fishing_rod_reel`, `reel_pull_strength`, `reel_attacker_bonus`, `reel_defender_bonus`). It was
    never the 5e Grapple; it was holding the name.
  - **`initiative:grapple` is the 5e grapple.** A free hand (else `HANDS_FULL`, a new
    `ActionStatus`), a contest, and on a win the target is **grappled**: its turn starts with no
    movement budget at all. New `initiative:escape` costs the held participant's action and contests
    against the grappler; a win clears the hold and hands the movement back. A hold also breaks when
    either side dies or leaves the encounter or when the two drift past
    `grapple.break_distance_blocks`, reconciled every tick so no condition is left on a corpse.
  - Escape only exists for someone who is actually held, through a new `Action.availableTo(actor)`
    seam that the gate and the UI both read — the turn UI shows the button to nobody else, and any
    other invocation is `DISABLED`. Any mod's action can now declare a per-actor condition the same
    way.
  - New `grapple` config group (its own object, as `cover` has): `enabled`, `reach_blocks`,
    `attacker_bonus`, `defender_bonus`, `break_distance_blocks`, `escape`, `escape_attacker_bonus`,
    `escape_defender_bonus`.
  - 11 new GameTests per loader (124 total, both loaders).

- **M8: the in-combat action UI.** On your turn a bar of buttons appears — one per registered
  action — with what is left of your turn above it and an End Turn button in it. The bar is built
  from the action registry, so a mod-registered action appears and is clickable with no UI code of
  its own, and every click runs through the same `ActionRegistry.invoke` the commands use. See
  [docs/ui.md](docs/ui.md).

  - A button is greyed with the exact status the gate would refuse it with (action spent, nothing in
    reach, item missing); an action disabled by config or datapack is absent rather than greyed.
  - Targeting follows the action's declared `targeting()`: none/self run at once, entity and
    position enter a targeting mode where left click confirms and right click cancels, and only the
    targets the server offered can be aimed at. Nothing about which action needs a target lives in
    the UI.
  - The turn's action / bonus action / reaction / movement and the turn-timeout countdown are on
    screen for the first time — the information the command flow never surfaced.
  - **Auto-end is no longer how a player's turn ends.** `actions.end_turn_when_spent` now covers
    players only and defaults **off**: a spent turn stays yours until you click End Turn. The new
    `actions.mob_end_turn_when_spent` (default on) keeps mob turns ending on their own, since a mob
    has no bar to click.
  - Behind `action_ui.enabled` (default on); off restores the M7 command-only flow exactly.
  - Per-player from the start — each player's bar is built and sent for them alone — so multiplayer
    needs no change to the UI shape.
  - 12 new GameTests per loader (113 total, both loaders) assert the data behind the buttons: the
    bar matches the registry, greying matches the gate, reach decides which targets a button offers,
    a click and a command answer alike, End Turn ends the turn, and the UI toggled off sends nothing
    and refuses clicks.

- **M7: the extensible action system.** Every action Initiative has — Attack, Dash, Disengage,
  Dodge, Help, Hide, Shove, Blink, Grapple, End Turn and the opportunity attack — now runs through
  one public registry (`studio.modroll.initiative.api`), declared through exactly the API another
  mod calls. Nothing in the registry or its invocation pipeline branches on whether an action is
  built-in, so a mod can add its own cost, targeting, gating and effect and have Initiative's turn
  engine run it, charge it and animate its roll with no changes to Initiative. See
  [docs/api.md](docs/api.md).

  Behaviour is unchanged: all 96 pre-existing GameTests pass **unedited** on both loaders, which is
  the proof. Five new ones register a test action in a foreign namespace and assert it is gated,
  targeted, charged, disabled and animated exactly like a built-in.

  - Actions declare a cost (`ACTION` / `BONUS_ACTION` / `MOVEMENT` / `REACTION` / `FREE`) and a
    targeting model (none / self / position / entity with a side and optional reach). The pipeline
    checks the cost up front but charges it only after a performed action — which is why an attack
    into total cover and a blink without a pearl still keep the turn's action, while a lost Shove or
    Hide contest spends it.
  - `/initiative <action>` commands are generated from the registry instead of hand-written; every
    existing literal and message is preserved, and a registered mod action gets its command free.
  - New optional datapack tunables per action at `data/<namespace>/initiative/actions/<path>.json`,
    reloaded with `/reload`.
  - Movement stays a per-tick reconcile rather than becoming a fake invoked action; the budget it
    draws on is exposed as the `MOVEMENT` cost for actions that want to spend it.
  - Rejection lines added for the new statuses (`no_bonus_action`, `no_reaction`, `no_movement`).

- M6 fix: the **result no longer beats the dice to the screen**. Critfall sends its action-bar
  readout in the same tick the roll resolves, which announced the outcome while the die was still
  tumbling. An action-bar line arriving mid-tumble is now held client-side and re-shown the moment
  the dice settle (mixin on vanilla's `Gui.setOverlayMessage` — neither loader has an event for it).
  Nothing server-side waits: damage, turn state and the readout packet are unchanged, only the
  moment the client paints the line moves. New `roll_animation.hold_readout` toggle (default true).

- M6 polish: the **readout no longer collides with the dice**. Vanilla draws the action bar at
  `guiHeight - 68` and the dice sat right on top of it; they now sit clear above it. Initiative's
  own lines are trimmed to one short clause each (`Action already spent`, `Out of reach`, `You
  dodge`), and rejections join confirmations in that single action-bar slot, in red — nothing
  Initiative says goes to chat any more.

- M6 polish: the **turn-order bar wraps** instead of crowding one line. Cells are packed into as
  many centered rows as the screen width needs (`TurnOrderLayout`, unit-tested), and names too long
  for a cell are cut with an ellipsis, so a big encounter stays readable and never runs off the
  edges. The current-actor highlight still follows the entity id, wherever it lands.

- M6 polish: the die's face triangle is gone. It competed with the number instead of reading as a
  d20 face; the die is now the hexagon body, the coloured edge and the number alone.

- Turns now give real thinking time: `turn_timeout_ticks` default raised from 100 (5 seconds) to
  **300 (15 seconds)** — long enough to read the turn bar and type an action command. Unchanged
  otherwise, and still config-driven.

- M6 fix: **one readout line, not a column**. Nothing Initiative emits duplicates Critfall's roll
  readout (the Part-A removal held), but every action confirmation went to chat and piled up — the
  Hide line alone was a two-clause sentence. Confirmations now go to the action bar, the same single
  line Critfall's readout uses, so each replaces the last; the Hide lines are down to `You slip out
  of sight` / `You fail to hide`. Rejections still go to chat, where they can be read at leisure.

- M6 fix: **opportunity attacks (and mob turn attacks) now animate**. The animation only went to the
  roller, and those rolls are made by the mob, so the player being attacked saw nothing. A roll now
  goes to the one player in the exchange — the roller, or the player rolled against when a mob
  rolled. Two mobs rolling at each other still render for nobody, and this is still a single
  recipient, not the M7 broadcast.

- M6 fix: the die now **looks like a die**. It was a plain rectangle with a number passing through
  it; it is now drawn as a hexagon — the silhouette of an icosahedron — with the face triangle
  outlined behind the number, a coloured edge (white, gold for a crit, red for a fumble, grey when
  dropped) and a darker body.

- M6: the **d20 roll animation**. Every roll Initiative drives — attacks (Attack action, mob turns,
  opportunity attacks), contests (Hide, Shove, grapple) and the initiative roll on joining — is sent
  to the player who made it and drawn above the hotbar: the dice tumble through faces, then settle
  on the natural Critfall already rolled. Advantage and disadvantage show both naturals from
  `RollDetail` with the discarded die greyed out; a crit or fumble gets a coloured border and a
  one-pixel jitter, following Critfall's `AttackOutcome` for attacks and the die's own extremes
  elsewhere. The modifier, AC and hit/miss stay in Critfall's text readout — the dice never repeat
  it. Purely presentational: the roll has already resolved and applied before anything is sent, and
  combat is identical with the animation off. Only the acting player sees it; shared roll visibility
  is M7. New `roll_animation` config object (`enabled`, `tumble_ticks`, `hold_ticks`,
  `face_change_ticks`, ~1 second by default) — off restores the text-only behaviour. Documented in
  `docs/rolls.md` and `docs/config.md`.

- Bump the Critfall dependency to 0.2.6, whose results now carry a `RollDetail` (roll mode, both
  natural dice when advantage or disadvantage was used, the kept one) and whose readout shows the
  roll mode and the cover AC split natively. Initiative's duplicate action-bar lines — the
  `(adv)`/`(disadv)` hint and `Cover: +2 (AC 14 → 16)` — and their `adv_disadv_hint` toggle are
  removed; Critfall carries both. Contest feedback stays Initiative's, since Critfall has no
  contest readout.

- M5b: dynamic line-of-sight **cover**. Every driven attack (Attack action, mob turns, opportunity
  attacks) raycasts eight rays from the attacker's eye to the corners of the target's hitbox and
  derives a 5e-style tier from the obstructed fraction: half cover (`half_cover_ac_bonus`, default
  +2) at/above `half_cover_threshold` (0.5), three-quarters (`three_quarter_cover_ac_bonus`, default
  +5) at/above `three_quarter_cover_threshold` (0.75), and total cover when every ray is blocked. A
  block counts when it stops Minecraft's own collision clip (`ClipContext.Block.COLLIDER`) — no
  hardcoded block list; entities never grant cover (deferred). The tier's AC bonus is fed to
  Critfall via `AttackContext.withDefenderAcBonus`; Initiative never reimplements AC math. Total
  cover rejects the attack with `TOTAL_COVER` and spends nothing by default
  (`total_cover_blocks_attack`); off, it degrades to the three-quarter bonus. When cover applies and
  `adv_disadv_hint` is on, an action-bar line reads e.g. `Cover: +2 (AC 14 → 16)`. Cover recomputes
  per attack from live geometry, never cached. New `cover` config object; `enabled` off restores
  exact pre-M5b attacks. Documented in `docs/actions.md` and `docs/config.md`.

- M5b: **building for cover**. While a participant is on its turn, placing a block spends movement
  budget (`block_placement_movement_cost`, default 1); with no movement left the placement is
  prevented. Toggled by `block_placement_costs_movement` (off = free). Only participants on their own
  turn are affected — non-participants and out-of-encounter placement are untouched. NeoForge hooks
  `BlockEvent.EntityPlaceEvent`; Fabric mixes into `BlockItem.place` (no official cancelable Fabric
  block-place event).

- Bump the Critfall dependency to 0.2.5 and adopt its per-attack defender AC modifier
  (`AttackContext.withDefenderAcBonus`, `AttackResult.armorClass`/`defenderAcBonus`/`baseArmorClass`)
  for cover.

- M5a: Minecraft-native actions. **Shove** (`/initiative shove <target>`) — a contested roll via
  Critfall's `RollService.contest`; on a win the target is knocked back away from the shover with
  vanilla knockback so falls/lava/cliffs resolve naturally, on a loss the action is still spent.
  Reach `shove_reach_blocks` (default 3), `shove_knockback_strength` (default 1.0), flat contest
  bonuses `shove_attacker_bonus`/`shove_defender_bonus`. **Ender-pearl blink** (`/initiative blink`)
  — consumes a pearl and teleports you up to `blink_max_blocks` (default 8) along your horizontal
  facing, costing the action. **Fishing-rod grapple** (`/initiative grapple <target>`) — a contested
  roll that on a win pulls the target toward you (`grapple_pull_strength`, default 1.0); win or lose
  the rod takes one durability and the action is spent. All three are turn- and action-gated like
  Attack and individually toggleable (`shove`, `ender_pearl_blink`, `fishing_rod_grapple`). Contest
  bonuses are flat placeholders until the future Checks mod. Documented in `docs/actions.md` and
  `docs/config.md`.

- Bump the Critfall dependency to 0.2.4 and adopt its contested-roll API (`RollService.contest`,
  `ContestContext`, `ContestResult`).

- Hide is now a real contested roll: the hider rolls `d20 + hide_stealth_bonus` against the nearest
  hostile observer's `d20 + hide_observer_perception_bonus` via Critfall's `RollService.contest`,
  hidden only on a strict win (ties go to the observer), auto-hidden when no hostile observer is
  present. The M4b static-DC fallback (base + observer save bonus) is deleted, closing the
  contested-roll API gap. Config key `hide_passive_perception_base` is renamed to
  `hide_observer_perception_bonus` (now a flat observer Perception bonus, default 0). Hide semantics
  are otherwise unchanged. Documented in `docs/actions.md` and `docs/config.md`.

- M4b: reactions and opportunity attacks — each participant has one reaction per round, refreshed
  at that participant's own turn start (not round start). Opportunity attacks fire when a hostile
  leaves reach (`opportunity_attack_reach_blocks`, default 3 blocks ≈ 15 ft, horizontal distance);
  only opposite-side participants provoke. Disengage suppresses opportunity attacks for the
  disengager's turn. A reaction resolves without freezing the reactor and never advances the turn.
  New `actions` config fields: `opportunity_attack` (toggle), `opportunity_attack_reach_blocks`.
  Documented in `docs/actions.md` and `docs/config.md`.

- M4b: Hide action — consumes the action; rolls `d20 + hide_stealth_bonus` vs DC of
  `hide_passive_perception_base + best hostile observer's save bonus`; success grants advantage on
  your next attack **and** disadvantage on attacks against you (reusing the Dodge disadvantage path;
  Dodge and hidden do not stack beyond a single disadvantage). Hidden is a persistent condition
  (unlike `dodging`/`disengaged`, it does not lapse at the hider's own next turn start) and breaks
  only on attacking, being hit, or leaving the encounter. No sneak attack bonus in M4b, and hidden
  does not yet suppress mob AI targeting (a mob still sees and targets a hidden participant —
  intervening in vanilla targeting is a later milestone). New `actions` config fields: `hide`
  (toggle), `hide_passive_perception_base` (default 10), `hide_stealth_bonus` (default 0).
  Documented in `docs/actions.md` and `docs/config.md`.

- M4b: advantage/disadvantage hint — Initiative shows `(adv)` or `(disadv)` on the action bar when
  a driven attack uses Critfall's advantage or disadvantage roll mode (`adv_disadv_hint` toggle).
  Critfall's roll feedback payload carries no roll mode, a documented API gap. Full animation is M6.
  New `actions` config field: `adv_disadv_hint` (default true). Documented in `docs/actions.md`.

- Critfall API gap: no contested-roll / passive-Perception seam. M4b's Hide mechanic uses a base
  DC (Passive Perception approximation) plus the observer's bonus, working around Critfall's
  missing contested-roll support where one party rolls and the other's DC is static. Documented in
  `docs/actions.md` and tracked in this changelog.

- Critfall API gap: roll mode not included in feedback payload. Critfall's `RollResult` (the
  feedback passed to action-bar callbacks) does not include the `RollMode` used by the roll,
  blocking full automation of advantage/disadvantage feedback. M4b works around this with a
  text-only hint. Documented in `docs/actions.md`.

- Bump Critfall dependency to 0.2.3 and adopt its two new API additions, removing the M3
  consumer-side workarounds. Participant-damage cancel now reads Critfall's public
  `RollService.isDrivenDamage` instead of a mirrored thread-local guard, and the driven-attack
  i-frame reset in the M3 GameTests is gone — 0.2.3 makes driven damage bypass invulnerability
  frames in Critfall. A new focus-fire GameTest proves two attackers can both land full damage on
  one target in a round. No Critfall internals are referenced.

- M3: action economy and driven d20 attacks — every turn grants a movement budget (default 6
  blocks), one action, one bonus action and a reaction (the last two tracked but unused until
  M4). Attacking on your turn cancels the vanilla swing and drives Critfall's
  `RollService.performAttack` — the real turn-ordered d20 with hit/miss/crit/fumble, damage and
  feedback — consuming the action (also on a miss); out-of-turn, action-less or invalid-target
  attacks are rejected with an actionbar message. Movement is clamped per tick with a hard stop
  at the budget's edge (vertical free, off-turn movement unrestricted). Mob turns are simple:
  attack the nearest enemy in reach through the API, freeze once movement is gone. Turns end via
  `/initiative endturn`, auto-end when spent (configurable), or the M2a timeout. While the
  economy is active, vanilla damage between participants of one encounter is canceled (membership
  read as of the previous tick so the encounter-forming hit still lands); Initiative's driven
  damage is exempted via Critfall's `RollService.isDrivenDamage`. New `actions` config section (`enabled`,
  `movement_budget_blocks`, `single_attack_per_turn`, `end_turn_when_spent`) — existing config
  files need it added. Documented in `docs/actions.md` and `docs/config.md`.

- M2c: turn-order HUD — player participants see a client-side initiative bar (icon + name per
  participant, current actor highlighted, dead/fled entries removed) synced from the server as
  immutable snapshots keyed by encounter id, sent only when turn state changes and cleared on
  encounter end, leave, or disconnect. The HUD is display-only and holds no authority. New
  `hud` config section with `enabled` — existing config files need it added. Documented in
  `docs/turns.md` and `docs/config.md`.

- M2b: AI freeze/thaw — mob participants are frozen off-turn (a `Mob.serverAiStep` HEAD mixin on
  both loaders cancels the AI step and clears movement inputs; the rest of the entity tick —
  physics, status effects, timers, damage — runs untouched) and thaw for the length of their own
  turn. Frozen mobs stay fully targetable and damageable. Every encounter exit path thaws;
  freeze state is transient and server-side only, so a restart can never leave a statue. Entity
  types can be excluded via the `#initiative:no_freeze` tag or `turns.no_freeze_types` config
  (they keep their turn slot but act in real time). New `turns` fields `freeze_enabled`,
  `acting_marker_enabled` (debug particle on the acting entity) and `no_freeze_types` — existing
  config files need the new fields added. Documented in `docs/turns.md`.

- M2a: initiative and turn order — encounter participants roll `1d20 + floor(movementSpeed ×
  initiative_bonus_per_speed)` (clamped) through Critfall's `RollService` on join, are sorted into
  a per-encounter turn order (ties: higher bonus, then join order), and a server-side state
  machine advances turns and rounds via `endTurn` or a configurable per-turn timeout. Late
  joiners insert into the running order; removals keep the current turn consistent. AI is not
  frozen yet (M2b). New `turns` config section, documented in `docs/turns.md`.

- Bump Critfall dependency to 0.2.2 and detect encounters via `CritfallEvents.onCombatInteraction`
  instead of racing Critfall on the raw damage events (drops the Fabric pre-default event phase
  and the NeoForge `HIGHEST` + `receiveCanceled` workarounds). A swing Critfall would miss or
  cancel still triggers, and Initiative-driven API attacks never re-trigger detection.

- Bump Critfall dependency to 0.2.1 (relocated `api.*` packages, test-scope RNG seam).
- M0 smoke tests force exact rolls through `RollService.setRoller` instead of asserting invariants.
- M1: encounter bubbles — a player attacking or being attacked by a hostile forms a server-side
  encounter that pulls in hostiles within a configurable radius, suppresses Critfall's real-time
  pipeline for members, and releases the flag on every exit path (flee, death, unload, dimension
  change, level unload, server stop). Configurable via `config/initiative.json`; feature can be
  disabled entirely.
