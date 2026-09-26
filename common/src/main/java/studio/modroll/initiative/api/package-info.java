/**
 * Initiative's public API: register a turn action and Initiative treats it exactly like a built-in.
 *
 * <p>Build one with {@link studio.modroll.initiative.api.Action#builder} and hand it to
 * {@link studio.modroll.initiative.api.ActionRegistry#register} during mod construction. An
 * {@link studio.modroll.initiative.api.Action} declares what it costs
 * ({@link studio.modroll.initiative.api.ActionCost}), what it needs pointed at it
 * ({@link studio.modroll.initiative.api.ActionTargeting}) and what it does — everything else is the
 * pipeline's job. One invocation path serves commands, UI clicks and reactions alike: it checks the
 * economy, the action's own toggle, the encounter, the turn, the cost and the target, runs the
 * effect, and charges the cost only if the effect performed. The same check greys the UI button, so
 * the reason shown and the reason refused are always the same
 * {@link studio.modroll.initiative.api.ActionStatus}.
 *
 * <p>An effect sees only {@link studio.modroll.initiative.api.ActionContext} — no Initiative
 * internals — and returns an {@link studio.modroll.initiative.api.ActionResult}: performed (whether
 * or not its roll landed) or rejected. Numbers and the enabled toggle can come from a datapack;
 * see {@code docs/api.md}.
 */
package studio.modroll.initiative.api;
