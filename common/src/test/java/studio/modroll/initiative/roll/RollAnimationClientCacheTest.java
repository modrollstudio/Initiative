package studio.modroll.initiative.roll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.initiative.config.RollAnimationConfig;

class RollAnimationClientCacheTest {

    private static final RollAnimationConfig CONFIG = new RollAnimationConfig(true, 10, 10, 2, true, true);
    private static final long START = 1_000L;

    @AfterEach
    void clearCache() {
        RollAnimationClientCache.clear();
    }

    private static long millisAfter(int ticks) {
        return START + ticks * 50L;
    }

    @Test
    void nothingPlaysUntilAnAnimationArrives() {
        assertTrue(RollAnimationClientCache.frame(START, CONFIG).isEmpty());
    }

    @Test
    void anAcceptedAnimationTumblesThenSettles() {
        RollAnimationClientCache.accept(RollAnimation.initiative(RollDetail.normal(17), "NightsHigh"), START);
        assertFalse(RollAnimationClientCache.frame(START, CONFIG)
                .orElseThrow()
                .dice()
                .getFirst()
                .settled());
        assertEquals(
                17,
                RollAnimationClientCache.frame(millisAfter(CONFIG.tumbleTicks()), CONFIG)
                        .orElseThrow()
                        .dice()
                        .getFirst()
                        .face());
    }

    @Test
    void aFinishedAnimationStopsPlayingAndIsForgotten() {
        RollAnimationClientCache.accept(RollAnimation.initiative(RollDetail.normal(17), "NightsHigh"), START);
        assertTrue(RollAnimationClientCache.frame(millisAfter(CONFIG.totalTicks()), CONFIG)
                .isEmpty());
        assertTrue(RollAnimationClientCache.frame(START, CONFIG).isEmpty());
    }

    @Test
    void aNewRollRestartsTheAnimation() {
        RollAnimationClientCache.accept(RollAnimation.initiative(RollDetail.normal(17), "NightsHigh"), START);
        long later = millisAfter(CONFIG.tumbleTicks());
        RollAnimationClientCache.accept(RollAnimation.initiative(RollDetail.normal(3), "NightsHigh"), later);
        assertFalse(RollAnimationClientCache.frame(later, CONFIG)
                .orElseThrow()
                .dice()
                .getFirst()
                .settled());
    }
}
