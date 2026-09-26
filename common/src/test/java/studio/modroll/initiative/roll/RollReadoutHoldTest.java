package studio.modroll.initiative.roll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.initiative.config.RollAnimationConfig;

class RollReadoutHoldTest {

    private static final RollAnimationConfig CONFIG = new RollAnimationConfig(true, 10, 10, 2, true, true);
    private static final long START = 1_000L;
    private static final Component READOUT = Component.literal("d20 17+3=20 vs AC 14 — HIT");

    @AfterEach
    void clearState() {
        RollAnimationClientCache.clear();
        RollReadoutHold.clear();
    }

    private static long millisAfter(int ticks) {
        return START + ticks * 50L;
    }

    private static void startRoll() {
        RollAnimationClientCache.accept(RollAnimation.initiative(RollDetail.normal(17), "NightsHigh"), START);
    }

    @Test
    void aReadoutWithNoRollInFlightPassesStraightThrough() {
        assertFalse(RollReadoutHold.hold(READOUT, START, CONFIG));
    }

    @Test
    void aReadoutLandingMidTumbleIsHeldUntilTheDieSettles() {
        startRoll();
        assertTrue(RollReadoutHold.hold(READOUT, START, CONFIG));
        assertTrue(RollReadoutHold.release(millisAfter(CONFIG.tumbleTicks() - 1), CONFIG)
                .isEmpty());
        assertEquals(
                READOUT,
                RollReadoutHold.release(millisAfter(CONFIG.tumbleTicks()), CONFIG)
                        .orElseThrow());
    }

    @Test
    void aReleasedReadoutIsOnlyHandedOverOnce() {
        startRoll();
        RollReadoutHold.hold(READOUT, START, CONFIG);
        long settled = millisAfter(CONFIG.tumbleTicks());
        RollReadoutHold.release(settled, CONFIG);
        assertTrue(RollReadoutHold.release(settled, CONFIG).isEmpty());
    }

    @Test
    void onlyTheLatestHeldReadoutSurvives() {
        startRoll();
        RollReadoutHold.hold(READOUT, START, CONFIG);
        Component newer = Component.literal("d20 20+3=23 vs AC 14 — CRIT!");
        RollReadoutHold.hold(newer, START, CONFIG);
        assertEquals(
                newer,
                RollReadoutHold.release(millisAfter(CONFIG.tumbleTicks()), CONFIG)
                        .orElseThrow());
    }

    @Test
    void aReadoutAfterTheDieSettledIsNotHeld() {
        startRoll();
        assertFalse(RollReadoutHold.hold(READOUT, millisAfter(CONFIG.tumbleTicks()), CONFIG));
    }

    @Test
    void holdingIsOffWhenTheAnimationOrTheHoldIsDisabled() {
        startRoll();
        assertFalse(RollReadoutHold.hold(READOUT, START, new RollAnimationConfig(false, 10, 10, 2, true, true)));
        assertFalse(RollReadoutHold.hold(READOUT, START, new RollAnimationConfig(true, 10, 10, 2, false, true)));
    }
}
