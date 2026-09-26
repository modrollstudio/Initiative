package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TurnBudgetTest {

    @Test
    void freshBudgetIsInertUntilReset() {
        TurnBudget budget = new TurnBudget();
        assertFalse(budget.actionAvailable());
        assertTrue(budget.movementExhausted());
    }

    @Test
    void resetGrantsAllResources() {
        TurnBudget budget = new TurnBudget();
        budget.reset(6.0);
        assertEquals(6.0, budget.movementRemaining());
        assertTrue(budget.actionAvailable());
        assertTrue(budget.bonusActionAvailable());
        assertFalse(budget.movementExhausted());
    }

    @Test
    void movementConsumesAndClampsAtZero() {
        TurnBudget budget = new TurnBudget();
        budget.reset(6.0);
        budget.consumeMovement(2.5);
        assertEquals(3.5, budget.movementRemaining(), 1e-9);
        budget.consumeMovement(10.0);
        assertEquals(0.0, budget.movementRemaining());
        assertTrue(budget.movementExhausted());
    }

    @Test
    void useActionAlwaysSpends() {
        TurnBudget budget = new TurnBudget();
        budget.reset(6.0);
        budget.useAction();
        assertFalse(budget.actionAvailable());
    }

    @Test
    void addMovementExtendsTheRemainingBudget() {
        TurnBudget budget = new TurnBudget();
        budget.reset(6.0);
        budget.consumeMovement(4.0);
        budget.addMovement(6.0);
        assertEquals(8.0, budget.movementRemaining(), 1e-9);
        assertFalse(budget.movementExhausted());
    }
}
