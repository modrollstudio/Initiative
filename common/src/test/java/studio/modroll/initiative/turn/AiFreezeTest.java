package studio.modroll.initiative.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AiFreezeTest {

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @AfterEach
    void reset() {
        AiFreeze.clear();
    }

    @Test
    void freezeMarksOnlyThatEntity() {
        AiFreeze.freeze(first);
        assertTrue(AiFreeze.isFrozen(first));
        assertFalse(AiFreeze.isFrozen(second));
    }

    @Test
    void thawRemovesTheMark() {
        AiFreeze.freeze(first);
        AiFreeze.thaw(first);
        assertFalse(AiFreeze.isFrozen(first));
    }

    @Test
    void thawingAnUnknownUuidIsANoOp() {
        AiFreeze.thaw(first);
        assertFalse(AiFreeze.isFrozen(first));
        assertEquals(Set.of(), AiFreeze.frozenUuids());
    }

    @Test
    void freezingTwiceNeedsOnlyOneThaw() {
        AiFreeze.freeze(first);
        AiFreeze.freeze(first);
        AiFreeze.thaw(first);
        assertFalse(AiFreeze.isFrozen(first));
    }

    @Test
    void frozenUuidsExposesTheExactSet() {
        AiFreeze.freeze(first);
        AiFreeze.freeze(second);
        assertEquals(Set.of(first, second), AiFreeze.frozenUuids());
    }

    /**
     * The mixin reads this set from every mob's AI step server-wide, so a reader must never see a
     * set mid-mutation. A plain HashSet throws ConcurrentModificationException here.
     */
    @Test
    void readsAreSafeWhileTheSetIsBeingMutated() throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 100_000 && done.getCount() > 0; i++) {
                UUID id = UUID.randomUUID();
                AiFreeze.freeze(id);
                AiFreeze.thaw(id);
            }
            done.countDown();
        });
        Thread reader = new Thread(() -> {
            try {
                while (done.getCount() > 0) {
                    AiFreeze.frozenUuids();
                    AiFreeze.isFrozen(first);
                }
            } catch (Throwable t) {
                failure.set(t);
                done.countDown();
            }
        });
        writer.start();
        reader.start();
        writer.join();
        reader.join();
        assertNull(failure.get());
    }

    @Test
    void clearEmptiesEverything() {
        AiFreeze.freeze(first);
        AiFreeze.freeze(second);
        AiFreeze.clear();
        assertEquals(Set.of(), AiFreeze.frozenUuids());
        assertFalse(AiFreeze.isFrozen(first));
    }
}
