package studio.modroll.initiative.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TurnOrderClientCacheTest {

    private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static TurnOrderSnapshot snapshot(UUID encounterId, int actorId) {
        return new TurnOrderSnapshot(
                encounterId,
                List.of(new TurnOrderSnapshot.Entry(actorId, "Husk", "minecraft:husk", false)),
                actorId,
                15);
    }

    @BeforeEach
    void reset() {
        TurnOrderClientCache.clear();
    }

    @Test
    void storesAndReplacesSnapshots() {
        TurnOrderClientCache.accept(snapshot(FIRST, 1));
        TurnOrderClientCache.accept(snapshot(FIRST, 2));
        assertEquals(2, TurnOrderClientCache.current().orElseThrow().currentActorId());
    }

    @Test
    void emptySnapshotWithMatchingIdClears() {
        TurnOrderClientCache.accept(snapshot(FIRST, 1));
        TurnOrderClientCache.accept(TurnOrderSnapshot.empty(FIRST));
        assertTrue(TurnOrderClientCache.current().isEmpty());
    }

    @Test
    void emptySnapshotFromAnotherEncounterIsIgnored() {
        TurnOrderClientCache.accept(snapshot(FIRST, 1));
        TurnOrderClientCache.accept(TurnOrderSnapshot.empty(SECOND));
        assertEquals(FIRST, TurnOrderClientCache.current().orElseThrow().encounterId());
    }

    @Test
    void emptySnapshotWithNothingCachedIsANoOp() {
        TurnOrderClientCache.accept(TurnOrderSnapshot.empty(FIRST));
        assertTrue(TurnOrderClientCache.current().isEmpty());
    }

    @Test
    void clearDropsTheSnapshot() {
        TurnOrderClientCache.accept(snapshot(FIRST, 1));
        TurnOrderClientCache.clear();
        assertTrue(TurnOrderClientCache.current().isEmpty());
    }
}
