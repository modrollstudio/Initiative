package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.RollAnimationScenarios;

/** NeoForge registration shim: delegates to the shared {@link RollAnimationScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class RollAnimationGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m6RollAdvantage")
    public void advantageAnimatesBothNaturalsKeepingTheHigher(GameTestHelper helper) {
        RollAnimationScenarios.advantageAnimatesBothNaturalsKeepingTheHigher(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m6RollDisadvantage")
    public void disadvantageAnimatesBothNaturalsKeepingTheLower(GameTestHelper helper) {
        RollAnimationScenarios.disadvantageAnimatesBothNaturalsKeepingTheLower(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m6RollNormal")
    public void aNormalAttackAnimatesOneDie(GameTestHelper helper) {
        RollAnimationScenarios.aNormalAttackAnimatesOneDie(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m6RollContest")
    public void aContestAnimatesBothSides(GameTestHelper helper) {
        RollAnimationScenarios.aContestAnimatesBothSides(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m6RollEmphasis")
    public void natTwentyAndNatOneAreFlaggedForEmphasis(GameTestHelper helper) {
        RollAnimationScenarios.natTwentyAndNatOneAreFlaggedForEmphasis(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m6RollReaction")
    public void aMobsOpportunityAttackAnimatesForTheMover(GameTestHelper helper) {
        RollAnimationScenarios.aMobsOpportunityAttackAnimatesForTheMover(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m6RollInitiative")
    public void theInitiativeRollAnimates(GameTestHelper helper) {
        RollAnimationScenarios.theInitiativeRollAnimates(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m6RollDisabled")
    public void disabledAnimationSendsNothingAndLeavesCombatUnchanged(GameTestHelper helper) {
        RollAnimationScenarios.disabledAnimationSendsNothingAndLeavesCombatUnchanged(helper);
    }
}
