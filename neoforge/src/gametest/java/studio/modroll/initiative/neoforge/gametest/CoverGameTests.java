package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.CoverScenarios;

/** NeoForge registration shim: delegates to the shared {@link CoverScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class CoverGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m5bNoCover")
    public void openLineOfSightAppliesNoCover(GameTestHelper helper) {
        CoverScenarios.openLineOfSightAppliesNoCover(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5bHalfCover")
    public void halfCoverTurnsAHitIntoAMiss(GameTestHelper helper) {
        CoverScenarios.halfCoverTurnsAHitIntoAMiss(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5bThreeQuarter")
    public void loweredThresholdsPromoteHalfToThreeQuarter(GameTestHelper helper) {
        CoverScenarios.loweredThresholdsPromoteHalfToThreeQuarter(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5bTotalCover")
    public void totalCoverRejectsTheAttackAndKeepsTheAction(GameTestHelper helper) {
        CoverScenarios.totalCoverRejectsTheAttackAndKeepsTheAction(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5bDynamic")
    public void coverRecomputesPerAttack(GameTestHelper helper) {
        CoverScenarios.coverRecomputesPerAttack(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5bOaCover")
    public void opportunityAttackRespectsTotalCover(GameTestHelper helper) {
        CoverScenarios.opportunityAttackRespectsTotalCover(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5bCoverOff")
    public void coverOffMatchesPreM5b(GameTestHelper helper) {
        CoverScenarios.coverOffMatchesPreM5b(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5bBuildCost")
    public void buildingSpendsMovementAndBlocksWhenExhausted(GameTestHelper helper) {
        CoverScenarios.buildingSpendsMovementAndBlocksWhenExhausted(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5bBuildRules")
    public void buildingRulesRespectToggleTurnAndMembership(GameTestHelper helper) {
        CoverScenarios.buildingRulesRespectToggleTurnAndMembership(helper);
    }
}
