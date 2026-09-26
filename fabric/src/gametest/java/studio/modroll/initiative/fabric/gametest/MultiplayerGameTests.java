package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.MultiplayerScenarios;

/** Fabric registration shim: delegates to the shared {@link MultiplayerScenarios} bodies. */
public class MultiplayerGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9SharedRoll")
    public void everyPlayerInTheEncounterWatchesTheSameRoll(GameTestHelper helper) {
        MultiplayerScenarios.everyPlayerInTheEncounterWatchesTheSameRoll(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9RollAttribution")
    public void theAnimationNamesItsRoller(GameTestHelper helper) {
        MultiplayerScenarios.theAnimationNamesItsRoller(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9SharedRollOff")
    public void sharedVisibilityOffKeepsTheRollAtTheExchange(GameTestHelper helper) {
        MultiplayerScenarios.sharedVisibilityOffKeepsTheRollAtTheExchange(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9LateJoin")
    public void aSecondPlayerJoinsTheOrderAndGetsItsOwnUi(GameTestHelper helper) {
        MultiplayerScenarios.aSecondPlayerJoinsTheOrderAndGetsItsOwnUi(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9PerPlayerBudget")
    public void eachPlayersUiShowsOnlyItsOwnBudget(GameTestHelper helper) {
        MultiplayerScenarios.eachPlayersUiShowsOnlyItsOwnBudget(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9NoPvp")
    public void playersAreAlliesAndCannotFightEachOther(GameTestHelper helper) {
        MultiplayerScenarios.playersAreAlliesAndCannotFightEachOther(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9Disconnect")
    public void aDisconnectedPlayerLeavesNothingBehind(GameTestHelper helper) {
        MultiplayerScenarios.aDisconnectedPlayerLeavesNothingBehind(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9Timer")
    public void theTurnTimerEndsAPlayersTurnAndAdvances(GameTestHelper helper) {
        MultiplayerScenarios.theTurnTimerEndsAPlayersTurnAndAdvances(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9StripClock")
    public void theTurnStripCountsDownForEveryPlayer(GameTestHelper helper) {
        MultiplayerScenarios.theTurnStripCountsDownForEveryPlayer(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9OneEncounterPerEntity")
    public void aHostileAlreadyFightingJoinsNoSecondEncounter(GameTestHelper helper) {
        MultiplayerScenarios.aHostileAlreadyFightingJoinsNoSecondEncounter(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m9FreedMob")
    public void aMobFreedByADisconnectJoinsAnotherPlayersEncounter(GameTestHelper helper) {
        MultiplayerScenarios.aMobFreedByADisconnectJoinsAnotherPlayersEncounter(helper);
    }
}
