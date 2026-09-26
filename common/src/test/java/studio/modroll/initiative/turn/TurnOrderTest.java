package studio.modroll.initiative.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TurnOrderTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();
    private static final UUID D = UUID.randomUUID();

    private static TurnOrder orderOf(InitiativeEntry... entries) {
        TurnOrder order = new TurnOrder();
        for (InitiativeEntry entry : entries) {
            order.insert(entry);
        }
        return order;
    }

    private static List<UUID> participants(TurnOrder order) {
        return order.order().stream().map(InitiativeEntry::participant).toList();
    }

    @Test
    void sortsByTotalDescending() {
        TurnOrder order =
                orderOf(new InitiativeEntry(A, 5, 0), new InitiativeEntry(B, 20, 0), new InitiativeEntry(C, 12, 0));
        assertEquals(List.of(B, C, A), participants(order));
        assertEquals(B, order.currentTurn().orElseThrow());
        assertEquals(1, order.round());
    }

    @Test
    void tieBreaksByHigherBonusThenJoinOrder() {
        TurnOrder order =
                orderOf(new InitiativeEntry(A, 15, 2), new InitiativeEntry(B, 15, 4), new InitiativeEntry(C, 15, 2));
        assertEquals(List.of(B, A, C), participants(order));
    }

    @Test
    void endTurnWalksTheOrderAndWrapsIntoNextRound() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        assertEquals(A, order.currentTurn().orElseThrow());
        order.endTurn();
        assertEquals(B, order.currentTurn().orElseThrow());
        assertEquals(1, order.round());
        order.endTurn();
        assertEquals(A, order.currentTurn().orElseThrow());
        assertEquals(2, order.round());
    }

    @Test
    void timeoutTickAdvancesTheTurn() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        order.tick(3);
        order.tick(3);
        assertEquals(A, order.currentTurn().orElseThrow());
        order.tick(3);
        assertEquals(B, order.currentTurn().orElseThrow());
    }

    @Test
    void endTurnResetsTheTimeoutClock() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        order.tick(3);
        order.tick(3);
        order.endTurn();
        order.tick(3);
        order.tick(3);
        assertEquals(B, order.currentTurn().orElseThrow());
    }

    @Test
    void lateJoinerAheadOfCurrentKeepsCurrentTurn() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        order.endTurn();
        order.insert(new InitiativeEntry(C, 25, 0));
        assertEquals(List.of(C, A, B), participants(order));
        assertEquals(B, order.currentTurn().orElseThrow());
    }

    @Test
    void lateJoinerBehindCurrentActsThisRound() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        order.insert(new InitiativeEntry(C, 15, 0));
        assertEquals(List.of(A, C, B), participants(order));
        assertEquals(A, order.currentTurn().orElseThrow());
        order.endTurn();
        assertEquals(C, order.currentTurn().orElseThrow());
    }

    @Test
    void removingBeforeCurrentKeepsCurrentTurn() {
        TurnOrder order =
                orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 15, 0), new InitiativeEntry(C, 10, 0));
        order.endTurn();
        order.remove(A);
        assertEquals(List.of(B, C), participants(order));
        assertEquals(B, order.currentTurn().orElseThrow());
        assertEquals(1, order.round());
    }

    @Test
    void removingCurrentPassesTurnToNext() {
        TurnOrder order =
                orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 15, 0), new InitiativeEntry(C, 10, 0));
        order.remove(A);
        assertEquals(B, order.currentTurn().orElseThrow());
        assertEquals(1, order.round());
    }

    @Test
    void removingCurrentAtEndOfOrderWrapsIntoNextRound() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        order.endTurn();
        order.remove(B);
        assertEquals(A, order.currentTurn().orElseThrow());
        assertEquals(2, order.round());
    }

    @Test
    void removingAfterCurrentKeepsCurrentTurn() {
        TurnOrder order =
                orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 15, 0), new InitiativeEntry(C, 10, 0));
        order.remove(C);
        assertEquals(A, order.currentTurn().orElseThrow());
        assertEquals(List.of(A, B), participants(order));
    }

    @Test
    void removingLastParticipantEmptiesTheOrder() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0));
        order.remove(A);
        assertTrue(order.currentTurn().isEmpty());
        assertTrue(order.order().isEmpty());
        order.endTurn();
        order.tick(1);
        assertTrue(order.currentTurn().isEmpty());
    }

    @Test
    void removingUnknownParticipantChangesNothing() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        order.remove(D);
        assertEquals(List.of(A, B), participants(order));
        assertEquals(A, order.currentTurn().orElseThrow());
    }

    /** Every client counts the same clock down, so it is read off the turn rather than per player. */
    @Test
    void theCountdownRoundsUpAndStopsAtZero() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        assertEquals(10, order.secondsRemaining(200));
        order.tick(200);
        assertEquals(10, order.secondsRemaining(200));
        for (int tick = 1; tick < 21; tick++) {
            order.tick(200);
        }
        assertEquals(9, order.secondsRemaining(200));
        assertEquals(0, order.secondsRemaining(0));
    }

    /** A timed-out turn passes on and the clock starts again for whoever holds it. */
    @Test
    void theCountdownRestartsWhenTheTimeoutEndsTheTurn() {
        TurnOrder order = orderOf(new InitiativeEntry(A, 20, 0), new InitiativeEntry(B, 10, 0));
        for (int tick = 0; tick < 40; tick++) {
            order.tick(40);
        }
        assertEquals(B, order.currentTurn().orElseThrow());
        assertEquals(2, order.secondsRemaining(40));
    }
}
