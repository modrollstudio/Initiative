package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.MovementScenarios;

/** Fabric registration shim: delegates to the shared {@link MovementScenarios} bodies. */
public class MovementGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "movementDefault")
    public void aTurnGivesTenBlocksByDefault(GameTestHelper helper) {
        MovementScenarios.aTurnGivesTenBlocksByDefault(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "movementSwiftness")
    public void swiftnessGivesMoreBlocks(GameTestHelper helper) {
        MovementScenarios.swiftnessGivesMoreBlocks(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "movementSlowness")
    public void slownessGivesFewerBlocks(GameTestHelper helper) {
        MovementScenarios.slownessGivesFewerBlocks(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "movementHeld")
    public void aHeldPlayerStartsItsTurnWithItsFullBudget(GameTestHelper helper) {
        MovementScenarios.aHeldPlayerStartsItsTurnWithItsFullBudget(helper);
    }
}
