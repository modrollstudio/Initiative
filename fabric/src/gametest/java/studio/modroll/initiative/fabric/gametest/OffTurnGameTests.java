package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.OffTurnScenarios;

/** Fabric registration shim: delegates to the shared {@link OffTurnScenarios} bodies. */
public class OffTurnGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnHold")
    public void anOffTurnPlayerIsRootedAndPulledBack(GameTestHelper helper) {
        OffTurnScenarios.anOffTurnPlayerIsRootedAndPulledBack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnFluid")
    public void aHeldPlayerDoesNotSinkInAFluid(GameTestHelper helper) {
        OffTurnScenarios.aHeldPlayerDoesNotSinkInAFluid(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnFall")
    public void aHeldPlayerOutOfAFluidStillFalls(GameTestHelper helper) {
        OffTurnScenarios.aHeldPlayerOutOfAFluidStillFalls(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnActing")
    public void theActingPlayerMovesFreely(GameTestHelper helper) {
        OffTurnScenarios.theActingPlayerMovesFreely(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnInteraction")
    public void offTurnWorldInteractionIsRefused(GameTestHelper helper) {
        OffTurnScenarios.offTurnWorldInteractionIsRefused(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnArrival")
    public void theHoldReleasesWhenTheTurnArrives(GameTestHelper helper) {
        OffTurnScenarios.theHoldReleasesWhenTheTurnArrives(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnDeath")
    public void deathReleasesTheHold(GameTestHelper helper) {
        OffTurnScenarios.deathReleasesTheHold(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnLeave")
    public void leavingTheBubbleReleasesTheHold(GameTestHelper helper) {
        OffTurnScenarios.leavingTheBubbleReleasesTheHold(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnDisconnect")
    public void aDisconnectReleasesTheHold(GameTestHelper helper) {
        OffTurnScenarios.aDisconnectReleasesTheHold(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnEnd")
    public void theEncounterEndingReleasesTheHold(GameTestHelper helper) {
        OffTurnScenarios.theEncounterEndingReleasesTheHold(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnBystander")
    public void aNonParticipantPlayerIsUntouched(GameTestHelper helper) {
        OffTurnScenarios.aNonParticipantPlayerIsUntouched(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "offTurnToggle")
    public void theToggleOffRestoresFreeMovement(GameTestHelper helper) {
        OffTurnScenarios.theToggleOffRestoresFreeMovement(helper);
    }
}
