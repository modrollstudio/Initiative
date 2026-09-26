package studio.modroll.initiative.action;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.config.CoverConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * The build economy: while a participant is on its turn, placing a block spends movement budget, and
 * with too little movement left the placement is prevented. Gated by
 * {@code CoverConfig.blockPlacementCostsMovement}; off restores free placement. Only participants
 * acting inside an encounter are touched — non-participants and out-of-encounter placement are free.
 */
public final class BlockPlacement {

    private BlockPlacement() {}

    /**
     * Whether {@code player} may place a block right now, spending movement when it applies. Returns
     * true (leaving the budget untouched) for anyone the rule doesn't govern: the toggle is off, the
     * economy is inactive, the player isn't in an encounter, or it isn't their turn.
     */
    public static boolean onPlaceAttempt(ServerLevel level, Player player) {
        CoverConfig cover = InitiativeConfig.cover();
        if (!cover.blockPlacementCostsMovement() || !ActionEconomy.active()) {
            return true;
        }
        Encounter encounter =
                EncounterManager.encounterContaining(level, player.getUUID()).orElse(null);
        if (encounter == null
                || !player.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            return true;
        }
        TurnBudget budget = encounter.budgetFor(
                encounter.turnOrder().round(),
                player.getUUID(),
                InitiativeConfig.actions().movementBudgetBlocks());
        return tryCharge(budget, cover.blockPlacementMovementCost());
    }

    /** Spends {@code cost} movement if the budget can afford it; returns whether the placement is allowed. */
    static boolean tryCharge(TurnBudget budget, double cost) {
        if (budget.movementRemaining() < cost) {
            return false;
        }
        budget.consumeMovement(cost);
        return true;
    }
}
