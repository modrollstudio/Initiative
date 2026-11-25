package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.MountScenarios;

/** Fabric registration shim: delegates to the shared {@link MountScenarios} bodies. */
public class MountGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "mountRiderAndMountShareOneTurn", timeoutTicks = 400)
    public void riderAndMountShareOneTurn(GameTestHelper helper) {
        MountScenarios.riderAndMountShareOneTurn(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mountWithTheToggleOffTheMountTakesItsOwnTurn",
            timeoutTicks = 400)
    public void withTheToggleOffTheMountTakesItsOwnTurn(GameTestHelper helper) {
        MountScenarios.withTheToggleOffTheMountTakesItsOwnTurn(helper);
    }
}
