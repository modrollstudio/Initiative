/**
 * Critfall: Initiative — turn-based combat for Minecraft, built on Critfall's roll API.
 *
 * <p>The loader-agnostic half of the mod lives here; {@code neoforge/} and {@code fabric/} contain
 * only the wiring that turns each loader's events into calls on these packages. Everything is
 * server-authoritative: turn and roll state exists on the server and the client is sent snapshots to
 * draw.
 *
 * <p>The flow through the packages, in the order a fight moves through them:
 *
 * <ul>
 *   <li>{@link studio.modroll.initiative.encounter} — who is fighting whom. A hit forms an encounter
 *       bubble, mobs are pulled in and dropped out of it, and the per-tick reconcile that drives
 *       everything below runs from {@code EncounterManager.tick}.
 *   <li>{@link studio.modroll.initiative.turn} — whose turn it is. Initiative order, the per-turn
 *       timeout, and the two ways a non-acting participant is held still: {@code AiFreeze} for mobs,
 *       {@code OffTurnRestriction} for players.
 *   <li>{@link studio.modroll.initiative.action} — what the acting participant may do. The turn
 *       budget, the gate every action passes through, and Initiative's own built-in actions.
 *   <li>{@link studio.modroll.initiative.api} — the public extension point: register an action here
 *       and it gates, targets, charges, commands and draws itself like a built-in.
 *   <li>{@link studio.modroll.initiative.ui}, {@link studio.modroll.initiative.hud},
 *       {@link studio.modroll.initiative.roll} — what the players see. Each builds an immutable
 *       snapshot server-side, diffs it against the last one sent, and ships it over the wire; the
 *       {@code client} subpackages draw it and decide nothing.
 *   <li>{@link studio.modroll.initiative.config} — every gameplay number and feature toggle, read
 *       from {@code initiative.json} against the bundled defaults.
 *   <li>{@link studio.modroll.initiative.command} — the action commands, generated from the registry.
 * </ul>
 */
package studio.modroll.initiative;
