package studio.modroll.initiative.action;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * The one path every action takes, built-in or third-party: economy, own toggle, encounter, turn,
 * cost, targeting, effect, charge. The cost is checked before the effect but charged after it, which
 * is what lets an effect refuse itself — total cover, a missing item — without spending the turn,
 * while an effect that ran and merely lost its roll still pays.
 */
public final class ActionGate {

    private ActionGate() {}

    public static ActionResult invoke(
            ServerLevel level, LivingEntity actor, ResourceLocation id, ActionRequest request) {
        Action action = ActionRegistry.get(id).orElse(null);
        if (action == null) {
            return ActionResult.rejected(ActionStatus.DISABLED);
        }
        Encounter encounter =
                EncounterManager.encounterContaining(level, actor.getUUID()).orElse(null);
        ActionStatus rejection = availability(encounter, actor, action);
        if (rejection != null) {
            return ActionResult.rejected(rejection);
        }
        ActionCost cost = action.cost();
        TurnBudget budget = turnGated(cost) ? actingBudget(encounter, actor) : null;
        ActionStatus badTarget = targetRejection(action.targeting(), encounter, actor, request);
        if (badTarget != null) {
            return ActionResult.rejected(badTarget);
        }
        ActionResult result = action.perform(new EncounterActionContext(level, encounter, actor, request, id, budget));
        if (result.isPerformed()) {
            spend(encounter, actor, budget, cost, action.movementCost());
        }
        return result;
    }

    /**
     * The gate up to targeting — economy, own toggle, encounter, turn, cost — as a status, or null
     * when the action may be attempted. The UI asks the same question to grey a button that
     * {@link #invoke} asks to refuse one, so the two can never disagree.
     */
    public static ActionStatus availability(Encounter encounter, LivingEntity actor, Action action) {
        if (!ActionEconomy.active()) {
            return ActionStatus.INACTIVE;
        }
        if (!action.enabled()) {
            return ActionStatus.DISABLED;
        }
        if (encounter == null) {
            return ActionStatus.NOT_IN_ENCOUNTER;
        }
        if (!action.availableTo(actor)) {
            return ActionStatus.DISABLED;
        }
        ActionCost cost = action.cost();
        boolean turnGated = turnGated(cost);
        if (turnGated && !isCurrentTurn(encounter, actor)) {
            return ActionStatus.NOT_YOUR_TURN;
        }
        TurnBudget budget = turnGated ? actingBudget(encounter, actor) : null;
        return resourceRejection(
                cost,
                budget != null && budget.actionAvailable(),
                budget != null && budget.bonusActionAvailable(),
                encounter.flags().hasReaction(actor.getUUID()),
                budget == null ? 0.0 : budget.movementRemaining(),
                action.movementCost());
    }

    /** Every participant this entity-targeting action would accept right now, side and reach applied. */
    public static List<LivingEntity> validTargets(
            ServerLevel level, Encounter encounter, LivingEntity actor, ActionTargeting.Entity targeting) {
        double reach = targeting.reachBlocks().getAsDouble();
        List<LivingEntity> targets = new ArrayList<>();
        for (UUID participant : encounter.participants()) {
            if (!(level.getEntity(participant) instanceof LivingEntity candidate)) {
                continue;
            }
            if (!sideMatches(targeting.side(), encounter, actor, candidate)) {
                continue;
            }
            if (withinReach(reach, actor, candidate)) {
                targets.add(candidate);
            }
        }
        return List.copyOf(targets);
    }

    static boolean turnGated(ActionCost cost) {
        return cost != ActionCost.REACTION;
    }

    /** Null means the resource is there; the returned status is the shortage for that cost. */
    static ActionStatus resourceRejection(
            ActionCost cost,
            boolean actionAvailable,
            boolean bonusAvailable,
            boolean reactionAvailable,
            double movementRemaining,
            double movementCost) {
        return switch (cost) {
            case ACTION -> actionAvailable ? null : ActionStatus.NO_ACTION;
            case BONUS_ACTION -> bonusAvailable ? null : ActionStatus.NO_BONUS_ACTION;
            case REACTION -> reactionAvailable ? null : ActionStatus.NO_REACTION;
            case MOVEMENT -> movementRemaining >= movementCost ? null : ActionStatus.NO_MOVEMENT;
            case FREE -> null;
        };
    }

    /** Null means the request satisfies the targeting; {@code distanceSqr} is horizontal. */
    static ActionStatus targetRejection(
            ActionTargeting targeting,
            boolean targetPresent,
            boolean targetValid,
            boolean positionPresent,
            double distanceSqr) {
        return switch (targeting) {
            case ActionTargeting.Entity entity -> entityRejection(entity, targetPresent, targetValid, distanceSqr);
            case ActionTargeting.Position ignored -> positionPresent ? null : ActionStatus.INVALID_TARGET;
            case ActionTargeting.None ignored -> null;
            case ActionTargeting.Self ignored -> null;
        };
    }

    private static ActionStatus entityRejection(
            ActionTargeting.Entity entity, boolean targetPresent, boolean targetValid, double distanceSqr) {
        if (!targetPresent || !targetValid) {
            return ActionStatus.INVALID_TARGET;
        }
        double reach = entity.reachBlocks().getAsDouble();
        if (Double.isNaN(reach) || reach < 0.0) {
            return null;
        }
        return distanceSqr > reach * reach ? ActionStatus.OUT_OF_REACH : null;
    }

    private static ActionStatus targetRejection(
            ActionTargeting targeting, Encounter encounter, LivingEntity actor, ActionRequest request) {
        LivingEntity target = request.target().orElse(null);
        boolean valid = targeting instanceof ActionTargeting.Entity entity
                && target != null
                && sideMatches(entity.side(), encounter, actor, target);
        return targetRejection(
                targeting,
                target != null,
                valid,
                request.position().isPresent(),
                target == null ? 0.0 : horizontalDistanceSqr(actor, target));
    }

    private static boolean sideMatches(
            ActionTargeting.Side side, Encounter encounter, LivingEntity actor, LivingEntity target) {
        if (!target.isAlive() || !encounter.contains(target.getUUID())) {
            return false;
        }
        return switch (side) {
            case ENEMY -> encounter.side(actor.getUUID()) != encounter.side(target.getUUID());
            case ALLY ->
                !actor.getUUID().equals(target.getUUID())
                        && encounter.side(actor.getUUID()) == encounter.side(target.getUUID());
            case ANY -> true;
        };
    }

    private static boolean withinReach(double reach, LivingEntity actor, LivingEntity target) {
        if (Double.isNaN(reach) || reach < 0.0) {
            return true;
        }
        return horizontalDistanceSqr(actor, target) <= reach * reach;
    }

    private static double horizontalDistanceSqr(LivingEntity actor, LivingEntity target) {
        double dx = actor.getX() - target.getX();
        double dz = actor.getZ() - target.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean isCurrentTurn(Encounter encounter, LivingEntity actor) {
        return actor.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null));
    }

    private static TurnBudget actingBudget(Encounter encounter, LivingEntity actor) {
        return ActionEconomy.budgetOf(encounter, actor.getUUID());
    }

    private static void spend(
            Encounter encounter, LivingEntity actor, TurnBudget budget, ActionCost cost, double movementCost) {
        switch (cost) {
            case ACTION -> budget.useAction();
            case BONUS_ACTION -> budget.useBonusAction();
            case MOVEMENT -> budget.consumeMovement(movementCost);
            case REACTION -> encounter.flags().useReaction(actor.getUUID());
            case FREE -> {}
        }
    }
}
