package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.ActionUiScenarios;

/** NeoForge registration shim: delegates to the shared {@link ActionUiScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class ActionUiGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m8UiBar")
    public void barMirrorsTheRegistryForTheActingPlayer(GameTestHelper helper) {
        ActionUiScenarios.barMirrorsTheRegistryForTheActingPlayer(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiSpent")
    public void spendingTheActionGreysTheActionsItPaysFor(GameTestHelper helper) {
        ActionUiScenarios.spendingTheActionGreysTheActionsItPaysFor(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiDisabledAction")
    public void disabledActionLeavesTheBarEntirely(GameTestHelper helper) {
        ActionUiScenarios.disabledActionLeavesTheBarEntirely(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiReach")
    public void reachDecidesWhichTargetsAButtonOffers(GameTestHelper helper) {
        ActionUiScenarios.reachDecidesWhichTargetsAButtonOffers(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiNoTarget")
    public void anActionWithNoTargetInReachIsGreyed(GameTestHelper helper) {
        ActionUiScenarios.anActionWithNoTargetInReachIsGreyed(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiExternal")
    public void externalActionRunsThroughTheClickPath(GameTestHelper helper) {
        ActionUiScenarios.externalActionRunsThroughTheClickPath(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiParity")
    public void clickAndCommandShareTheGate(GameTestHelper helper) {
        ActionUiScenarios.clickAndCommandShareTheGate(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiEndTurn")
    public void endTurnButtonEndsTheTurn(GameTestHelper helper) {
        ActionUiScenarios.endTurnButtonEndsTheTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiNoAutoEnd")
    public void aSpentActionNoLongerEndsThePlayersTurn(GameTestHelper helper) {
        ActionUiScenarios.aSpentActionNoLongerEndsThePlayersTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiBudget")
    public void budgetAndClockTrackTheTurn(GameTestHelper helper) {
        ActionUiScenarios.budgetAndClockTrackTheTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiWaiting")
    public void aWaitingPlayerGetsNoBar(GameTestHelper helper) {
        ActionUiScenarios.aWaitingPlayerGetsNoBar(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8UiOff")
    public void disabledUiSendsNothingAndRefusesClicks(GameTestHelper helper) {
        ActionUiScenarios.disabledUiSendsNothingAndRefusesClicks(helper);
    }
}
