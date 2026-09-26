package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.ExternalActionScenarios;

/** Fabric registration shim: delegates to the shared {@link ExternalActionScenarios} bodies. */
public class ExternalActionGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m7ExternalOutOfTurn")
    public void externalActionIsRejectedOutOfTurn(GameTestHelper helper) {
        ExternalActionScenarios.externalActionIsRejectedOutOfTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m7ExternalSpends")
    public void externalActionRunsAndSpendsTheAction(GameTestHelper helper) {
        ExternalActionScenarios.externalActionRunsAndSpendsTheAction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m7ExternalTarget")
    public void externalActionRejectsAFriendlyTarget(GameTestHelper helper) {
        ExternalActionScenarios.externalActionRejectsAFriendlyTarget(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m7ExternalDisabled")
    public void disabledExternalActionIsRejected(GameTestHelper helper) {
        ExternalActionScenarios.disabledExternalActionIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m7ExternalAnimation")
    public void externalActionRollAnimatesLikeABuiltin(GameTestHelper helper) {
        ExternalActionScenarios.externalActionRollAnimatesLikeABuiltin(helper);
    }
}
