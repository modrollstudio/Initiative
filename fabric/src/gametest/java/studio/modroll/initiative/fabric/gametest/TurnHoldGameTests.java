package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.TurnHoldScenarios;

/** Fabric registration shim: delegates to the shared {@link TurnHoldScenarios} bodies. */
public class TurnHoldGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "turnHold")
    public void aHeldFightWaitsForTheHit(GameTestHelper helper) {
        TurnHoldScenarios.aHeldFightWaitsForTheHit(helper);
    }
}
