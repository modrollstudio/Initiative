package studio.modroll.initiative.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.config.TurnConfig;

class OffTurnRestrictionTest {

    private static TurnConfig turns(boolean enabled, boolean restrict) {
        return new TurnConfig(enabled, 600, 120, 20.0, 12, true, true, restrict, Set.of());
    }

    @Test
    void aParticipantIsHeldWhileSomebodyElseActs() {
        assertTrue(OffTurnRestriction.holds(turns(true, true), false));
    }

    @Test
    void theActingParticipantIsFree() {
        assertFalse(OffTurnRestriction.holds(turns(true, true), true));
    }

    @Test
    void theToggleOffHoldsNobody() {
        assertFalse(OffTurnRestriction.holds(turns(true, false), false));
    }

    @Test
    void turnsDisabledHoldsNobody() {
        assertFalse(OffTurnRestriction.holds(turns(false, true), false));
    }

    @Test
    void horizontalDriftIsPulledBackToTheAnchor() {
        Vec3 anchor = new Vec3(10.0, 64.0, 20.0);
        assertEquals(anchor, OffTurnRestriction.heldPosition(anchor, new Vec3(13.0, 64.0, 24.0), false));
    }

    /** Falling and knockback are vertical, so out of a fluid the pull-back keeps the current height. */
    @Test
    void thePullBackKeepsTheCurrentHeightOutOfAFluid() {
        Vec3 anchor = new Vec3(10.0, 64.0, 20.0);
        assertEquals(
                new Vec3(10.0, 60.5, 20.0), OffTurnRestriction.heldPosition(anchor, new Vec3(13.0, 60.5, 20.0), false));
    }

    @Test
    void purelyVerticalMovementIsLeftAlone() {
        Vec3 current = new Vec3(10.0, 60.5, 20.0);
        assertEquals(current, OffTurnRestriction.heldPosition(new Vec3(10.0, 64.0, 20.0), current, false));
    }

    @Test
    void standingStillNeedsNoCorrection() {
        Vec3 anchor = new Vec3(10.0, 64.0, 20.0);
        assertEquals(anchor, OffTurnRestriction.heldPosition(anchor, anchor, false));
    }

    /** In a fluid a free vertical is a slow sink, so the anchor holds the height as well. */
    @Test
    void sinkingInAFluidIsPulledBackToTheAnchorHeight() {
        Vec3 anchor = new Vec3(10.0, 64.0, 20.0);
        assertEquals(anchor, OffTurnRestriction.heldPosition(anchor, new Vec3(10.0, 63.6, 20.0), true));
    }

    @Test
    void floatingUpInAFluidIsPulledBackToTheAnchorHeight() {
        Vec3 anchor = new Vec3(10.0, 64.0, 20.0);
        assertEquals(anchor, OffTurnRestriction.heldPosition(anchor, new Vec3(10.0, 64.4, 20.0), true));
    }

    @Test
    void aFluidHoldsEveryAxisAtOnce() {
        Vec3 anchor = new Vec3(10.0, 64.0, 20.0);
        assertEquals(anchor, OffTurnRestriction.heldPosition(anchor, new Vec3(13.0, 63.6, 24.0), true));
    }
}
