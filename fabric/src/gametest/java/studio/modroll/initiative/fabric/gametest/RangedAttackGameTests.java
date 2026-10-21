package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.RangedAttackScenarios;

/** Fabric registration shim: delegates to the shared {@link RangedAttackScenarios} bodies. */
public class RangedAttackGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedHit")
    public void aPlayersArrowOnItsTurnRollsAndHits(GameTestHelper helper) {
        RangedAttackScenarios.aPlayersArrowOnItsTurnRollsAndHits(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedMiss")
    public void aMissedArrowFliesPastItsTarget(GameTestHelper helper) {
        RangedAttackScenarios.aMissedArrowFliesPastItsTarget(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedOffTurn")
    public void firingOffTurnIsRefusedButLoadingIsNot(GameTestHelper helper) {
        RangedAttackScenarios.firingOffTurnIsRefusedButLoadingIsNot(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedNoAction")
    public void aSpentActionRefusesTheDraw(GameTestHelper helper) {
        RangedAttackScenarios.aSpentActionRefusesTheDraw(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedLowered")
    public void aDrawHeldPastTheTurnIsLowered(GameTestHelper helper) {
        RangedAttackScenarios.aDrawHeldPastTheTurnIsLowered(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedInMelee")
    public void aHostileInMeleeReachImposesDisadvantage(GameTestHelper helper) {
        RangedAttackScenarios.aHostileInMeleeReachImposesDisadvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedPastNormal")
    public void beyondNormalRangeImposesDisadvantage(GameTestHelper helper) {
        RangedAttackScenarios.beyondNormalRangeImposesDisadvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedPastLong")
    public void beyondLongRangeThereIsNoShot(GameTestHelper helper) {
        RangedAttackScenarios.beyondLongRangeThereIsNoShot(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedCover")
    public void coverRaisesTheTargetsArmorClass(GameTestHelper helper) {
        RangedAttackScenarios.coverRaisesTheTargetsArmorClass(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedOff")
    public void rangedAttacksOffLeaveTheShotCanceled(GameTestHelper helper) {
        RangedAttackScenarios.rangedAttacksOffLeaveTheShotCanceled(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedSkeleton")
    public void aSkeletonShootsOnceOnItsTurn(GameTestHelper helper) {
        RangedAttackScenarios.aSkeletonShootsOnceOnItsTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedPillager")
    public void aPillagerShootsOnceOnItsTurn(GameTestHelper helper) {
        RangedAttackScenarios.aPillagerShootsOnceOnItsTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedFlameHit")
    public void aFlamingArrowThatHitsSetsTheTargetAlight(GameTestHelper helper) {
        RangedAttackScenarios.aFlamingArrowThatHitsSetsTheTargetAlight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedFlameMiss")
    public void aFlamingArrowThatMissesSetsNothingAlight(GameTestHelper helper) {
        RangedAttackScenarios.aFlamingArrowThatMissesSetsNothingAlight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedSkeletonMiss", timeoutTicks = 200)
    public void aSkeletonsMissFliesPastThePlayer(GameTestHelper helper) {
        RangedAttackScenarios.aSkeletonsMissFliesPastThePlayer(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedPillagerMiss", timeoutTicks = 200)
    public void aPillagersMissFliesPastThePlayer(GameTestHelper helper) {
        RangedAttackScenarios.aPillagersMissFliesPastThePlayer(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedSkeletonMissOutside", timeoutTicks = 200)
    public void aSkeletonsMissOutsideAnEncounterFliesPast(GameTestHelper helper) {
        RangedAttackScenarios.aSkeletonsMissOutsideAnEncounterFliesPast(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedPillagerMissOutside", timeoutTicks = 200)
    public void aPillagersMissOutsideAnEncounterFliesPast(GameTestHelper helper) {
        RangedAttackScenarios.aPillagersMissOutsideAnEncounterFliesPast(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedSkeletonInFlight", timeoutTicks = 200)
    public void aSkeletonsArrowInFlightWhenTheFightFormsFliesPast(GameTestHelper helper) {
        RangedAttackScenarios.aSkeletonsArrowInFlightWhenTheFightFormsFliesPast(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedPillagerInFlight", timeoutTicks = 200)
    public void aPillagersArrowInFlightWhenTheFightFormsFliesPast(GameTestHelper helper) {
        RangedAttackScenarios.aPillagersArrowInFlightWhenTheFightFormsFliesPast(helper);
    }
}
