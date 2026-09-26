package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.ActionUiScenarios;

/** Fabric registration shim: delegates to the shared {@link ActionUiScenarios} bodies. */
public class ActionUiGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiBar")
    public void barMirrorsTheRegistryForTheActingPlayer(GameTestHelper helper) {
        ActionUiScenarios.barMirrorsTheRegistryForTheActingPlayer(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiSpent")
    public void spendingTheActionGreysTheActionsItPaysFor(GameTestHelper helper) {
        ActionUiScenarios.spendingTheActionGreysTheActionsItPaysFor(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiDisabledAction")
    public void disabledActionLeavesTheBarEntirely(GameTestHelper helper) {
        ActionUiScenarios.disabledActionLeavesTheBarEntirely(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiReach")
    public void reachDecidesWhichTargetsAButtonOffers(GameTestHelper helper) {
        ActionUiScenarios.reachDecidesWhichTargetsAButtonOffers(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiNoTarget")
    public void anActionWithNoTargetInReachIsGreyed(GameTestHelper helper) {
        ActionUiScenarios.anActionWithNoTargetInReachIsGreyed(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiExternal")
    public void externalActionRunsThroughTheClickPath(GameTestHelper helper) {
        ActionUiScenarios.externalActionRunsThroughTheClickPath(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiParity")
    public void clickAndCommandShareTheGate(GameTestHelper helper) {
        ActionUiScenarios.clickAndCommandShareTheGate(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiEndTurn")
    public void endTurnButtonEndsTheTurn(GameTestHelper helper) {
        ActionUiScenarios.endTurnButtonEndsTheTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiNoAutoEnd")
    public void aSpentActionNoLongerEndsThePlayersTurn(GameTestHelper helper) {
        ActionUiScenarios.aSpentActionNoLongerEndsThePlayersTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiBudget")
    public void budgetAndClockTrackTheTurn(GameTestHelper helper) {
        ActionUiScenarios.budgetAndClockTrackTheTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiWaiting")
    public void aWaitingPlayerGetsNoBar(GameTestHelper helper) {
        ActionUiScenarios.aWaitingPlayerGetsNoBar(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8UiOff")
    public void disabledUiSendsNothingAndRefusesClicks(GameTestHelper helper) {
        ActionUiScenarios.disabledUiSendsNothingAndRefusesClicks(helper);
    }
}
