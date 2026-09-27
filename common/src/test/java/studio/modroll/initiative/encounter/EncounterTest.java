package studio.modroll.initiative.encounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.action.TurnBudget;
import studio.modroll.initiative.hud.TurnOrderSnapshot;
import studio.modroll.initiative.turn.InitiativeEntry;
import studio.modroll.initiative.ui.ActionUiSnapshot;

class EncounterTest {

    private static final Vec3 CENTER = new Vec3(0, 64, 0);

    @Test
    void containsAfterAdd() {
        Encounter encounter = new Encounter(CENTER);
        UUID player = UUID.randomUUID();
        encounter.add(player, Encounter.Side.PLAYER, 0L);
        assertTrue(encounter.contains(player));
        assertEquals(Set.of(player), encounter.participants());
    }

    @Test
    void removeClearsMembership() {
        Encounter encounter = new Encounter(CENTER);
        UUID hostile = UUID.randomUUID();
        encounter.add(hostile, Encounter.Side.HOSTILE, 0L);
        encounter.remove(hostile);
        assertFalse(encounter.contains(hostile));
        assertTrue(encounter.participants().isEmpty());
    }

    @Test
    void overWithoutAnyHostile() {
        Encounter encounter = new Encounter(CENTER);
        encounter.add(UUID.randomUUID(), Encounter.Side.PLAYER, 0L);
        assertTrue(encounter.isOver());
    }

    @Test
    void overWithoutAnyPlayer() {
        Encounter encounter = new Encounter(CENTER);
        encounter.add(UUID.randomUUID(), Encounter.Side.HOSTILE, 0L);
        assertTrue(encounter.isOver());
    }

    @Test
    void notOverWithBothSides() {
        Encounter encounter = new Encounter(CENTER);
        encounter.add(UUID.randomUUID(), Encounter.Side.PLAYER, 0L);
        encounter.add(UUID.randomUUID(), Encounter.Side.HOSTILE, 0L);
        assertFalse(encounter.isOver());
    }

    @Test
    void eachEncounterGetsItsOwnId() {
        assertNotNull(new Encounter(CENTER).id());
        assertNotEquals(new Encounter(CENTER).id(), new Encounter(CENTER).id());
    }

    @Test
    void lastHudSnapshotStartsUnsetAndRemembersTheLastValue() {
        Encounter encounter = new Encounter(CENTER);
        assertNull(encounter.lastHudSnapshot());
        TurnOrderSnapshot snapshot = TurnOrderSnapshot.empty(encounter.id());
        encounter.setLastHudSnapshot(snapshot);
        assertEquals(snapshot, encounter.lastHudSnapshot());
    }

    @Test
    void withinRadiusIsInclusiveOfBoundary() {
        Encounter encounter = new Encounter(CENTER);
        assertTrue(encounter.isWithin(CENTER.add(3, 0, 0), 3.0));
        assertFalse(encounter.isWithin(CENTER.add(3.1, 0, 0), 3.0));
    }

    @Test
    void sideIsQueryablePerParticipant() {
        Encounter encounter = new Encounter(CENTER);
        UUID player = UUID.randomUUID();
        encounter.add(player, Encounter.Side.PLAYER, 0L);
        assertEquals(Encounter.Side.PLAYER, encounter.side(player));
        assertNull(encounter.side(UUID.randomUUID()));
    }

    @Test
    void budgetResetsOncePerTurnKey() {
        Encounter encounter = new Encounter(CENTER);
        UUID actor = UUID.randomUUID();
        TurnBudget budget = encounter.budgetFor(1, actor, 6.0);
        budget.consumeMovement(4.0);
        budget.useAction();
        assertSame(budget, encounter.budgetFor(1, actor, 6.0));
        assertEquals(2.0, encounter.budgetFor(1, actor, 6.0).movementRemaining(), 1e-9);
        assertEquals(6.0, encounter.budgetFor(1, UUID.randomUUID(), 6.0).movementRemaining());
        assertTrue(encounter.budgetFor(2, actor, 6.0).actionAvailable());
    }

    @Test
    void joinedBeforeIsFalseOnTheJoinTick() {
        Encounter encounter = new Encounter(CENTER);
        UUID player = UUID.randomUUID();
        encounter.add(player, Encounter.Side.PLAYER, 100L);
        assertFalse(encounter.joinedBefore(player, 100L));
        assertTrue(encounter.joinedBefore(player, 101L));
        assertFalse(encounter.joinedBefore(UUID.randomUUID(), 101L));
    }

    @Test
    void startingAnActorsTurnClearsItsOwnPerTurnFlags() {
        Encounter encounter = new Encounter(CENTER);
        UUID actor = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        encounter.budgetFor(1, actor, 6.0);
        encounter.flags().setDodging(actor);
        encounter.budgetFor(1, other, 6.0);
        assertTrue(encounter.flags().isDodging(actor));
        encounter.budgetFor(2, actor, 6.0);
        assertFalse(encounter.flags().isDodging(actor));
    }

    @Test
    void removingAParticipantPurgesItsFlags() {
        Encounter encounter = new Encounter(CENTER);
        UUID participant = UUID.randomUUID();
        encounter.add(participant, Encounter.Side.PLAYER, 0L);
        encounter.flags().setDodging(participant);
        encounter.remove(participant);
        assertFalse(encounter.flags().isDodging(participant));
    }

    @Test
    void budgetResetClearsTheMovementAnchor() {
        Encounter encounter = new Encounter(CENTER);
        UUID actor = UUID.randomUUID();
        encounter.budgetFor(1, actor, 6.0);
        encounter.setMovementAnchor(new Vec3(1, 0, 1));
        encounter.budgetFor(2, actor, 6.0);
        assertNull(encounter.movementAnchor());
    }

    @Test
    void firstMovementAnchorIsSetAtTheActorsCurrentPositionAfterTurnReset() {
        Encounter encounter = new Encounter(CENTER);
        UUID actor = UUID.randomUUID();
        encounter.budgetFor(1, actor, 6.0);
        encounter.setMovementAnchor(new Vec3(1, 0, 1));

        encounter.budgetFor(2, actor, 6.0);
        Vec3 positionAfterForcedMovement = new Vec3(8, 0, 1);
        if (encounter.movementAnchor() == null) {
            encounter.setMovementAnchor(positionAfterForcedMovement);
        }

        assertEquals(positionAfterForcedMovement, encounter.movementAnchor());
        assertEquals(6.0, encounter.budget().movementRemaining());
    }

    /**
     * The leak check for per-encounter state: a participant that leaves must take everything the
     * encounter held about it — membership, join time, its turn-order slot, its last action-UI
     * snapshot and its flags. A map added to Encounter without a matching line in remove() fails
     * here, which is what keeps a long-running server from accumulating dead participants.
     */
    @Test
    void removingAParticipantDropsEveryTraceOfIt() {
        Encounter encounter = new Encounter(CENTER);
        UUID leaver = UUID.randomUUID();
        UUID staying = UUID.randomUUID();
        encounter.add(leaver, Encounter.Side.PLAYER, 10L);
        encounter.add(staying, Encounter.Side.HOSTILE, 10L);
        encounter.turnOrder().insert(new InitiativeEntry(leaver, 18, 2));
        encounter.turnOrder().insert(new InitiativeEntry(staying, 9, 0));
        encounter.setLastActionUiSnapshot(leaver, ActionUiSnapshot.INACTIVE);
        encounter.flags().setDodging(leaver);
        encounter.flags().setGrappled(staying, leaver);

        encounter.remove(leaver);

        assertFalse(encounter.contains(leaver));
        assertFalse(encounter.participants().contains(leaver));
        assertNull(encounter.side(leaver));
        assertFalse(encounter.joinedBefore(leaver, 999L));
        assertNull(encounter.lastActionUiSnapshot(leaver));
        assertFalse(encounter.flags().isDodging(leaver));
        assertFalse(encounter.flags().isGrappled(staying));
        assertTrue(encounter.turnOrder().order().stream()
                .noneMatch(entry -> entry.participant().equals(leaver)));
        assertTrue(encounter.contains(staying));
    }
}
