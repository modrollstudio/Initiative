package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.api.ActionTargeting.Side;

class ActionGateTest {

    private static ActionStatus resource(ActionCost cost, boolean available) {
        return ActionGate.resourceRejection(cost, available, available, available, available ? 6.0 : 0.0, 1.0);
    }

    @Test
    void eachCostReportsItsOwnShortage() {
        assertEquals(ActionStatus.NO_ACTION, resource(ActionCost.ACTION, false));
        assertEquals(ActionStatus.NO_BONUS_ACTION, resource(ActionCost.BONUS_ACTION, false));
        assertEquals(ActionStatus.NO_REACTION, resource(ActionCost.REACTION, false));
        assertEquals(ActionStatus.NO_MOVEMENT, resource(ActionCost.MOVEMENT, false));
    }

    @Test
    void anAvailableResourceIsNotRejected() {
        assertNull(resource(ActionCost.ACTION, true));
        assertNull(resource(ActionCost.BONUS_ACTION, true));
        assertNull(resource(ActionCost.REACTION, true));
        assertNull(resource(ActionCost.MOVEMENT, true));
    }

    @Test
    void freeCostsNeverReject() {
        assertNull(resource(ActionCost.FREE, false));
    }

    @Test
    void movementIsComparedAgainstTheDeclaredCost() {
        assertNull(ActionGate.resourceRejection(ActionCost.MOVEMENT, false, false, false, 2.0, 2.0));
        assertEquals(
                ActionStatus.NO_MOVEMENT,
                ActionGate.resourceRejection(ActionCost.MOVEMENT, false, false, false, 1.9, 2.0));
    }

    @Test
    void onlyReactionsSkipTheTurnGate() {
        assertFalse(ActionGate.turnGated(ActionCost.REACTION));
        assertTrue(ActionGate.turnGated(ActionCost.ACTION));
        assertTrue(ActionGate.turnGated(ActionCost.BONUS_ACTION));
        assertTrue(ActionGate.turnGated(ActionCost.MOVEMENT));
        assertTrue(ActionGate.turnGated(ActionCost.FREE));
    }

    @Test
    void entityTargetingRejectsAnAbsentOrInvalidTarget() {
        ActionTargeting targeting = ActionTargeting.entity(Side.ENEMY);
        assertEquals(ActionStatus.INVALID_TARGET, ActionGate.targetRejection(targeting, false, false, false, 0.0));
        assertEquals(ActionStatus.INVALID_TARGET, ActionGate.targetRejection(targeting, true, false, false, 0.0));
    }

    @Test
    void entityTargetingRejectsOutOfReach() {
        ActionTargeting targeting = ActionTargeting.entity(Side.ENEMY, () -> 3.0);
        assertNull(ActionGate.targetRejection(targeting, true, true, false, 9.0));
        assertEquals(ActionStatus.OUT_OF_REACH, ActionGate.targetRejection(targeting, true, true, false, 9.01));
    }

    @Test
    void anUndeclaredReachNeverRejects() {
        ActionTargeting targeting = ActionTargeting.entity(Side.ENEMY);
        assertNull(ActionGate.targetRejection(targeting, true, true, false, 4096.0));
    }

    @Test
    void selfAndNoneTargetingNeverReject() {
        assertNull(ActionGate.targetRejection(ActionTargeting.self(), false, false, false, 0.0));
        assertNull(ActionGate.targetRejection(ActionTargeting.none(), false, false, false, 0.0));
    }

    @Test
    void positionTargetingNeedsAPosition() {
        assertEquals(
                ActionStatus.INVALID_TARGET,
                ActionGate.targetRejection(ActionTargeting.position(), false, false, false, 0.0));
        assertNull(ActionGate.targetRejection(ActionTargeting.position(), false, false, true, 0.0));
    }
}
