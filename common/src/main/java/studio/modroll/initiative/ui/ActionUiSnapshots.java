package studio.modroll.initiative.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionGate;
import studio.modroll.initiative.action.TurnBudget;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.turn.TurnOrder;
import studio.modroll.initiative.turn.TurnTimeout;

/**
 * Builds one player's action UI from the registry: every action that exists for them, gated by the
 * very {@link ActionGate} the invocation uses. Nothing here knows what an action is — a mod's
 * registration walks this path exactly like Attack does.
 */
public final class ActionUiSnapshots {

    private static final double MOVEMENT_STEP = 10.0;

    private ActionUiSnapshots() {}

    public static ActionUiSnapshot build(ServerLevel level, Encounter encounter, Player player) {
        if (!player.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            return ActionUiSnapshot.INACTIVE;
        }
        ActionConfig actions = InitiativeConfig.actions();
        List<ActionUiSnapshot.Entry> entries = new ArrayList<>();
        for (Action action : ActionRegistry.all()) {
            if (action.cost() == ActionCost.REACTION || !action.enabled() || !action.availableTo(player)) {
                continue; // reactions are triggered; a disabled or inapplicable action does not exist here
            }
            entries.add(entry(level, encounter, player, action));
        }
        int timeoutTicks = TurnTimeout.forCurrentActor(level, encounter.turnOrder(), InitiativeConfig.turns());
        return new ActionUiSnapshot(
                true,
                List.copyOf(entries),
                budget(encounter, player, actions),
                encounter.turnOrder().secondsRemaining(timeoutTicks),
                TurnOrder.seconds(timeoutTicks));
    }

    private static ActionUiSnapshot.Entry entry(ServerLevel level, Encounter encounter, Player player, Action action) {
        ActionStatus unavailable = ActionGate.availability(encounter, player, action);
        ActionTargeting targeting = action.targeting();
        List<Integer> validTargetIds = validTargetIds(level, encounter, player, targeting);
        if (unavailable == null && targeting instanceof ActionTargeting.Entity && validTargetIds.isEmpty()) {
            unavailable = ActionStatus.INVALID_TARGET;
        }
        return new ActionUiSnapshot.Entry(
                action.id(), action.cost(), kind(targeting), Optional.ofNullable(unavailable), validTargetIds);
    }

    private static List<Integer> validTargetIds(
            ServerLevel level, Encounter encounter, Player player, ActionTargeting targeting) {
        if (!(targeting instanceof ActionTargeting.Entity entity)) {
            return List.of();
        }
        return ActionGate.validTargets(level, encounter, player, entity).stream()
                .map(LivingEntity::getId)
                .toList();
    }

    private static ActionUiSnapshot.TargetKind kind(ActionTargeting targeting) {
        return switch (targeting) {
            case ActionTargeting.None ignored -> ActionUiSnapshot.TargetKind.NONE;
            case ActionTargeting.Self ignored -> ActionUiSnapshot.TargetKind.SELF;
            case ActionTargeting.Position ignored -> ActionUiSnapshot.TargetKind.POSITION;
            case ActionTargeting.Entity ignored -> ActionUiSnapshot.TargetKind.ENTITY;
        };
    }

    /** Movement is rounded to a tenth of a block so a walking player does not resend every tick. */
    private static ActionUiSnapshot.Budget budget(Encounter encounter, Player player, ActionConfig actions) {
        TurnBudget budget = ActionEconomy.budgetOf(encounter, player.getUUID());
        return new ActionUiSnapshot.Budget(
                budget.actionAvailable(),
                budget.bonusActionAvailable(),
                encounter.flags().hasReaction(player.getUUID()),
                Math.round(budget.movementRemaining() * MOVEMENT_STEP) / MOVEMENT_STEP,
                actions.movementBudgetBlocks());
    }
}
