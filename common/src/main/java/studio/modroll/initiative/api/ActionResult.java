package studio.modroll.initiative.api;

import java.util.Optional;
import studio.modroll.critfall.api.combat.AttackResult;

/**
 * The outcome of one action. {@code succeeded} means the effect landed — the contest was won, the
 * attack hit — not that the action was allowed: a lost contest is still {@code PERFORMED} and still
 * spends its cost. Only a rejected status leaves the turn untouched.
 */
public record ActionResult(ActionStatus status, boolean succeeded, Optional<AttackResult> attack) {

    public static ActionResult performed() {
        return performed(true);
    }

    public static ActionResult performed(boolean succeeded) {
        return new ActionResult(ActionStatus.PERFORMED, succeeded, Optional.empty());
    }

    public static ActionResult attacked(AttackResult result) {
        return new ActionResult(ActionStatus.PERFORMED, result.isHit(), Optional.of(result));
    }

    public static ActionResult rejected(ActionStatus status) {
        return new ActionResult(status, false, Optional.empty());
    }

    public boolean isPerformed() {
        return status == ActionStatus.PERFORMED;
    }
}
