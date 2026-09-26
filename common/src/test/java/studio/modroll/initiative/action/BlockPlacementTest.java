package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlockPlacementTest {

    private static TurnBudget budgetWith(double movement) {
        TurnBudget budget = new TurnBudget();
        budget.reset(movement);
        return budget;
    }

    @Test
    void chargeSpendsMovementAndAllows() {
        TurnBudget budget = budgetWith(6.0);
        assertTrue(BlockPlacement.tryCharge(budget, 1.0));
        assertEquals(5.0, budget.movementRemaining());
    }

    @Test
    void chargeIsRejectedWithoutEnoughMovement() {
        TurnBudget budget = budgetWith(0.5);
        assertFalse(BlockPlacement.tryCharge(budget, 1.0));
        assertEquals(0.5, budget.movementRemaining());
    }

    @Test
    void exactRemainingMovementStillAllows() {
        TurnBudget budget = budgetWith(1.0);
        assertTrue(BlockPlacement.tryCharge(budget, 1.0));
        assertEquals(0.0, budget.movementRemaining());
    }

    @Test
    void exhaustedBudgetRejectsPlacement() {
        TurnBudget budget = budgetWith(0.0);
        assertFalse(BlockPlacement.tryCharge(budget, 1.0));
    }

    @Test
    void zeroCostAlwaysAllowsAndSpendsNothing() {
        TurnBudget budget = budgetWith(0.0);
        assertTrue(BlockPlacement.tryCharge(budget, 0.0));
        assertEquals(0.0, budget.movementRemaining());
    }
}
