package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.ReactionScenarios;

/** Fabric registration shim: delegates to the shared {@link ReactionScenarios} bodies. */
public class ReactionGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bOaProvoke")
    public void movingOutOfReachProvokesAnOpportunityAttack(GameTestHelper helper) {
        ReactionScenarios.movingOutOfReachProvokesAnOpportunityAttack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bOaTurnState")
    public void aReactionLeavesTheTurnStateUnchanged(GameTestHelper helper) {
        ReactionScenarios.aReactionLeavesTheTurnStateUnchanged(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bOaDisengage")
    public void disengageSuppressesTheOpportunityAttack(GameTestHelper helper) {
        ReactionScenarios.disengageSuppressesTheOpportunityAttack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bOaEconomy")
    public void oneReactionPerRoundThatRefreshesAtTheReactorsTurn(GameTestHelper helper) {
        ReactionScenarios.oneReactionPerRoundThatRefreshesAtTheReactorsTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bOaFrozen")
    public void aFrozenReactorStaysFrozenThroughItsOpportunityAttack(GameTestHelper helper) {
        ReactionScenarios.aFrozenReactorStaysFrozenThroughItsOpportunityAttack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bOaIframe")
    public void opportunityAttackLandsFullDamageAfterOtherDamage(GameTestHelper helper) {
        ReactionScenarios.opportunityAttackLandsFullDamageAfterOtherDamage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bOaOff")
    public void opportunityAttackOffProvokesNothing(GameTestHelper helper) {
        ReactionScenarios.opportunityAttackOffProvokesNothing(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bHide")
    public void hideGrantsAdvantageOnTheNextTurnsAttackThenBreaks(GameTestHelper helper) {
        ReactionScenarios.hideGrantsAdvantageOnTheNextTurnsAttackThenBreaks(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bHideTie")
    public void hideTieLeavesYouSeen(GameTestHelper helper) {
        ReactionScenarios.hideTieLeavesYouSeen(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bHideLoss")
    public void hideLossLeavesYouSeen(GameTestHelper helper) {
        ReactionScenarios.hideLossLeavesYouSeen(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bHideHelp")
    public void hiddenAttackerWithHelpBreaksHiddenToo(GameTestHelper helper) {
        ReactionScenarios.hiddenAttackerWithHelpBreaksHiddenToo(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bHideOff")
    public void hideOffIsRejected(GameTestHelper helper) {
        ReactionScenarios.hideOffIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bHideDefense")
    public void attackAgainstAHiddenParticipantRollsDisadvantage(GameTestHelper helper) {
        ReactionScenarios.attackAgainstAHiddenParticipantRollsDisadvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bHideBreakHit")
    public void hiddenBreaksWhenTheHiderIsHit(GameTestHelper helper) {
        ReactionScenarios.hiddenBreaksWhenTheHiderIsHit(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bHideKeepMiss")
    public void hiddenSurvivesWhenTheHiderIsMissed(GameTestHelper helper) {
        ReactionScenarios.hiddenSurvivesWhenTheHiderIsMissed(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4bCleanup")
    public void participantRemovalClearsEveryFlag(GameTestHelper helper) {
        ReactionScenarios.participantRemovalClearsEveryFlag(helper);
    }
}
