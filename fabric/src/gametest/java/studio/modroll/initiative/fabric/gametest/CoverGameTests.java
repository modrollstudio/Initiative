package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.CoverScenarios;

/** Fabric registration shim: delegates to the shared {@link CoverScenarios} bodies. */
public class CoverGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bNoCover")
    public void openLineOfSightAppliesNoCover(GameTestHelper helper) {
        CoverScenarios.openLineOfSightAppliesNoCover(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bHalfCover")
    public void halfCoverTurnsAHitIntoAMiss(GameTestHelper helper) {
        CoverScenarios.halfCoverTurnsAHitIntoAMiss(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bThreeQuarter")
    public void loweredThresholdsPromoteHalfToThreeQuarter(GameTestHelper helper) {
        CoverScenarios.loweredThresholdsPromoteHalfToThreeQuarter(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bTotalCover")
    public void totalCoverRejectsTheAttackAndKeepsTheAction(GameTestHelper helper) {
        CoverScenarios.totalCoverRejectsTheAttackAndKeepsTheAction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bDynamic")
    public void coverRecomputesPerAttack(GameTestHelper helper) {
        CoverScenarios.coverRecomputesPerAttack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bOaCover")
    public void opportunityAttackRespectsTotalCover(GameTestHelper helper) {
        CoverScenarios.opportunityAttackRespectsTotalCover(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bCoverOff")
    public void coverOffMatchesPreM5b(GameTestHelper helper) {
        CoverScenarios.coverOffMatchesPreM5b(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bBuildCost")
    public void buildingSpendsMovementAndBlocksWhenExhausted(GameTestHelper helper) {
        CoverScenarios.buildingSpendsMovementAndBlocksWhenExhausted(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5bBuildRules")
    public void buildingRulesRespectToggleTurnAndMembership(GameTestHelper helper) {
        CoverScenarios.buildingRulesRespectToggleTurnAndMembership(helper);
    }
}
