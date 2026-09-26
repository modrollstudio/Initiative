package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.OffTurnScenarios;

/** NeoForge registration shim: delegates to the shared {@link OffTurnScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class OffTurnGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "offTurnHold")
    public void anOffTurnPlayerIsRootedAndPulledBack(GameTestHelper helper) {
        OffTurnScenarios.anOffTurnPlayerIsRootedAndPulledBack(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnFluid")
    public void aHeldPlayerDoesNotSinkInAFluid(GameTestHelper helper) {
        OffTurnScenarios.aHeldPlayerDoesNotSinkInAFluid(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnFall")
    public void aHeldPlayerOutOfAFluidStillFalls(GameTestHelper helper) {
        OffTurnScenarios.aHeldPlayerOutOfAFluidStillFalls(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnActing")
    public void theActingPlayerMovesFreely(GameTestHelper helper) {
        OffTurnScenarios.theActingPlayerMovesFreely(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnInteraction")
    public void offTurnWorldInteractionIsRefused(GameTestHelper helper) {
        OffTurnScenarios.offTurnWorldInteractionIsRefused(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnArrival")
    public void theHoldReleasesWhenTheTurnArrives(GameTestHelper helper) {
        OffTurnScenarios.theHoldReleasesWhenTheTurnArrives(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnDeath")
    public void deathReleasesTheHold(GameTestHelper helper) {
        OffTurnScenarios.deathReleasesTheHold(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnLeave")
    public void leavingTheBubbleReleasesTheHold(GameTestHelper helper) {
        OffTurnScenarios.leavingTheBubbleReleasesTheHold(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnDisconnect")
    public void aDisconnectReleasesTheHold(GameTestHelper helper) {
        OffTurnScenarios.aDisconnectReleasesTheHold(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnEnd")
    public void theEncounterEndingReleasesTheHold(GameTestHelper helper) {
        OffTurnScenarios.theEncounterEndingReleasesTheHold(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnBystander")
    public void aNonParticipantPlayerIsUntouched(GameTestHelper helper) {
        OffTurnScenarios.aNonParticipantPlayerIsUntouched(helper);
    }

    @GameTest(template = TEMPLATE, batch = "offTurnToggle")
    public void theToggleOffRestoresFreeMovement(GameTestHelper helper) {
        OffTurnScenarios.theToggleOffRestoresFreeMovement(helper);
    }
}
