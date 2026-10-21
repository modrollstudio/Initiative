package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.TeleportScenarios;

/** Fabric registration shim: delegates to the shared {@link TeleportScenarios} bodies. */
public class TeleportGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "teleportFar")
    public void aPearlPastYourMovementIsRefused(GameTestHelper helper) {
        TeleportScenarios.aPearlPastYourMovementIsRefused(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "teleportNear")
    public void aPearlInsideYourMovementCostsTheDistance(GameTestHelper helper) {
        TeleportScenarios.aPearlInsideYourMovementCostsTheDistance(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "teleportOffTurn")
    public void aTeleportOffYourTurnIsRefused(GameTestHelper helper) {
        TeleportScenarios.aTeleportOffYourTurnIsRefused(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "teleportToggle")
    public void theToggleOffLetsATeleportThrough(GameTestHelper helper) {
        TeleportScenarios.theToggleOffLetsATeleportThrough(helper);
    }
}
