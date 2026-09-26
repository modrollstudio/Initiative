package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.RollAnimationScenarios;

/** Fabric registration shim: delegates to the shared {@link RollAnimationScenarios} bodies. */
public class RollAnimationGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m6RollAdvantage")
    public void advantageAnimatesBothNaturalsKeepingTheHigher(GameTestHelper helper) {
        RollAnimationScenarios.advantageAnimatesBothNaturalsKeepingTheHigher(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m6RollDisadvantage")
    public void disadvantageAnimatesBothNaturalsKeepingTheLower(GameTestHelper helper) {
        RollAnimationScenarios.disadvantageAnimatesBothNaturalsKeepingTheLower(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m6RollNormal")
    public void aNormalAttackAnimatesOneDie(GameTestHelper helper) {
        RollAnimationScenarios.aNormalAttackAnimatesOneDie(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m6RollContest")
    public void aContestAnimatesBothSides(GameTestHelper helper) {
        RollAnimationScenarios.aContestAnimatesBothSides(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m6RollEmphasis")
    public void natTwentyAndNatOneAreFlaggedForEmphasis(GameTestHelper helper) {
        RollAnimationScenarios.natTwentyAndNatOneAreFlaggedForEmphasis(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m6RollReaction")
    public void aMobsOpportunityAttackAnimatesForTheMover(GameTestHelper helper) {
        RollAnimationScenarios.aMobsOpportunityAttackAnimatesForTheMover(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m6RollInitiative")
    public void theInitiativeRollAnimates(GameTestHelper helper) {
        RollAnimationScenarios.theInitiativeRollAnimates(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m6RollDisabled")
    public void disabledAnimationSendsNothingAndLeavesCombatUnchanged(GameTestHelper helper) {
        RollAnimationScenarios.disabledAnimationSendsNothingAndLeavesCombatUnchanged(helper);
    }
}
