package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.MultiplayerScenarios;

/** NeoForge registration shim: delegates to the shared {@link MultiplayerScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class MultiplayerGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m9SharedRoll")
    public void everyPlayerInTheEncounterWatchesTheSameRoll(GameTestHelper helper) {
        MultiplayerScenarios.everyPlayerInTheEncounterWatchesTheSameRoll(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9RollAttribution")
    public void theAnimationNamesItsRoller(GameTestHelper helper) {
        MultiplayerScenarios.theAnimationNamesItsRoller(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9SharedRollOff")
    public void sharedVisibilityOffKeepsTheRollAtTheExchange(GameTestHelper helper) {
        MultiplayerScenarios.sharedVisibilityOffKeepsTheRollAtTheExchange(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9LateJoin")
    public void aSecondPlayerJoinsTheOrderAndGetsItsOwnUi(GameTestHelper helper) {
        MultiplayerScenarios.aSecondPlayerJoinsTheOrderAndGetsItsOwnUi(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9PerPlayerBudget")
    public void eachPlayersUiShowsOnlyItsOwnBudget(GameTestHelper helper) {
        MultiplayerScenarios.eachPlayersUiShowsOnlyItsOwnBudget(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9NoPvp")
    public void playersAreAlliesAndCannotFightEachOther(GameTestHelper helper) {
        MultiplayerScenarios.playersAreAlliesAndCannotFightEachOther(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9Disconnect")
    public void aDisconnectedPlayerLeavesNothingBehind(GameTestHelper helper) {
        MultiplayerScenarios.aDisconnectedPlayerLeavesNothingBehind(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9Timer")
    public void theTurnTimerEndsAPlayersTurnAndAdvances(GameTestHelper helper) {
        MultiplayerScenarios.theTurnTimerEndsAPlayersTurnAndAdvances(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9StripClock")
    public void theTurnStripCountsDownForEveryPlayer(GameTestHelper helper) {
        MultiplayerScenarios.theTurnStripCountsDownForEveryPlayer(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9OneEncounterPerEntity")
    public void aHostileAlreadyFightingJoinsNoSecondEncounter(GameTestHelper helper) {
        MultiplayerScenarios.aHostileAlreadyFightingJoinsNoSecondEncounter(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9FreedMob")
    public void aMobFreedByADisconnectJoinsAnotherPlayersEncounter(GameTestHelper helper) {
        MultiplayerScenarios.aMobFreedByADisconnectJoinsAnotherPlayersEncounter(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9PartyPull")
    public void aNearbyPlayerRollsInWhenTheFightStarts(GameTestHelper helper) {
        MultiplayerScenarios.aNearbyPlayerRollsInWhenTheFightStarts(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9WalkIn")
    public void aPlayerWalkingInJoinsTheRunningOrder(GameTestHelper helper) {
        MultiplayerScenarios.aPlayerWalkingInJoinsTheRunningOrder(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m9PartyPullOff")
    public void pullNearbyPlayersOffLeavesABystanderOut(GameTestHelper helper) {
        MultiplayerScenarios.pullNearbyPlayersOffLeavesABystanderOut(helper);
    }
}
