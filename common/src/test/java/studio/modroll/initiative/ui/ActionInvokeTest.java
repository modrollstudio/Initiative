package studio.modroll.initiative.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ActionInvokeTest {

    @Test
    void ordinaryCoordinatesArePositions() {
        assertTrue(ActionInvoke.isUsablePosition(new Vec3(12.5, 64.0, -3.5)));
    }

    @Test
    void notANumberIsNotAPosition() {
        assertFalse(ActionInvoke.isUsablePosition(new Vec3(Double.NaN, 64.0, 0.0)));
        assertFalse(ActionInvoke.isUsablePosition(new Vec3(0.0, Double.NaN, 0.0)));
        assertFalse(ActionInvoke.isUsablePosition(new Vec3(0.0, 64.0, Double.NaN)));
    }

    @Test
    void infiniteCoordinatesAreNotAPosition() {
        assertFalse(ActionInvoke.isUsablePosition(new Vec3(Double.POSITIVE_INFINITY, 64.0, 0.0)));
        assertFalse(ActionInvoke.isUsablePosition(new Vec3(0.0, Double.NEGATIVE_INFINITY, 0.0)));
    }
}
