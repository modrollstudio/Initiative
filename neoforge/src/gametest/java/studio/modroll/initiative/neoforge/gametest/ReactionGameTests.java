package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.ReactionScenarios;

/** NeoForge registration shim: delegates to the shared {@link ReactionScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReactionGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m4bOaProvoke")
    public void movingOutOfReachProvokesAnOpportunityAttack(GameTestHelper helper) {
        ReactionScenarios.movingOutOfReachProvokesAnOpportunityAttack(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bOaTurnState")
    public void aReactionLeavesTheTurnStateUnchanged(GameTestHelper helper) {
        ReactionScenarios.aReactionLeavesTheTurnStateUnchanged(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bOaDisengage")
    public void disengageSuppressesTheOpportunityAttack(GameTestHelper helper) {
        ReactionScenarios.disengageSuppressesTheOpportunityAttack(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bOaEconomy")
    public void oneReactionPerRoundThatRefreshesAtTheReactorsTurn(GameTestHelper helper) {
        ReactionScenarios.oneReactionPerRoundThatRefreshesAtTheReactorsTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bOaFrozen")
    public void aFrozenReactorStaysFrozenThroughItsOpportunityAttack(GameTestHelper helper) {
        ReactionScenarios.aFrozenReactorStaysFrozenThroughItsOpportunityAttack(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bOaIframe")
    public void opportunityAttackLandsFullDamageAfterOtherDamage(GameTestHelper helper) {
        ReactionScenarios.opportunityAttackLandsFullDamageAfterOtherDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bOaOff")
    public void opportunityAttackOffProvokesNothing(GameTestHelper helper) {
        ReactionScenarios.opportunityAttackOffProvokesNothing(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bHide")
    public void hideGrantsAdvantageOnTheNextTurnsAttackThenBreaks(GameTestHelper helper) {
        ReactionScenarios.hideGrantsAdvantageOnTheNextTurnsAttackThenBreaks(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bHideTie")
    public void hideTieLeavesYouSeen(GameTestHelper helper) {
        ReactionScenarios.hideTieLeavesYouSeen(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bHideLoss")
    public void hideLossLeavesYouSeen(GameTestHelper helper) {
        ReactionScenarios.hideLossLeavesYouSeen(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bHideHelp")
    public void hiddenAttackerWithHelpBreaksHiddenToo(GameTestHelper helper) {
        ReactionScenarios.hiddenAttackerWithHelpBreaksHiddenToo(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bHideOff")
    public void hideOffIsRejected(GameTestHelper helper) {
        ReactionScenarios.hideOffIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bHideDefense")
    public void attackAgainstAHiddenParticipantRollsDisadvantage(GameTestHelper helper) {
        ReactionScenarios.attackAgainstAHiddenParticipantRollsDisadvantage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bHideBreakHit")
    public void hiddenBreaksWhenTheHiderIsHit(GameTestHelper helper) {
        ReactionScenarios.hiddenBreaksWhenTheHiderIsHit(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bHideKeepMiss")
    public void hiddenSurvivesWhenTheHiderIsMissed(GameTestHelper helper) {
        ReactionScenarios.hiddenSurvivesWhenTheHiderIsMissed(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4bCleanup")
    public void participantRemovalClearsEveryFlag(GameTestHelper helper) {
        ReactionScenarios.participantRemovalClearsEveryFlag(helper);
    }
}
