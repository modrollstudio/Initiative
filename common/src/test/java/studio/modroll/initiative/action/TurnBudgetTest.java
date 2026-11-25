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

    @Test
    void theFirstAttackSpendsTheActionAndOpensTheRest() {
        TurnBudget budget = new TurnBudget();
        budget.reset(6.0);
        assertTrue(budget.attackAvailable());
        budget.useAttack(3);
        assertFalse(budget.actionAvailable());
        assertEquals(2, budget.attacksLeft());
        budget.useAttack(3);
        assertEquals(1, budget.attacksLeft());
        assertTrue(budget.attackAvailable());
        budget.useAttack(3);
        assertEquals(0, budget.attacksLeft());
        assertFalse(budget.attackAvailable());
    }

    @Test
    void oneAttackSpendsTheActionAndLeavesNoneOver() {
        TurnBudget budget = new TurnBudget();
        budget.reset(6.0);
        budget.useAttack(1);
        assertFalse(budget.actionAvailable());
        assertEquals(0, budget.attacksLeft());
        assertFalse(budget.attackAvailable());
    }

    @Test
    void theAttackPoolEmptiesEachTurn() {
        TurnBudget budget = new TurnBudget();
        budget.reset(6.0);
        budget.useAttack(2);
        budget.reset(6.0);
        assertEquals(0, budget.attacksLeft());
        assertTrue(budget.actionAvailable());
    }

    @Test
    void anActionSpentElsewhereClosesTheAttack() {
        TurnBudget budget = new TurnBudget();
        budget.reset(6.0);
        budget.useAction();
        assertFalse(budget.attackAvailable());
    }
}
