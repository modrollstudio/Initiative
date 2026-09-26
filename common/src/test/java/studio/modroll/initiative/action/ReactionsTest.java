package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ReactionsTest {

    private static final Vec3 REACTOR = new Vec3(0, 0, 0);

    @Test
    void leavingReachProvokes() {
        assertTrue(Reactions.leftReach(new Vec3(2, 0, 0), new Vec3(5, 0, 0), REACTOR, 3.0));
    }

    @Test
    void stayingInReachDoesNotProvoke() {
        assertFalse(Reactions.leftReach(new Vec3(1, 0, 0), new Vec3(2.5, 0, 0), REACTOR, 3.0));
    }

    @Test
    void alreadyOutOfReachDoesNotProvoke() {
        assertFalse(Reactions.leftReach(new Vec3(4, 0, 0), new Vec3(6, 0, 0), REACTOR, 3.0));
    }

    @Test
    void verticalDistanceIsIgnored() {
        assertFalse(Reactions.leftReach(new Vec3(2, 0, 0), new Vec3(2, 40, 0), REACTOR, 3.0));
    }
}
