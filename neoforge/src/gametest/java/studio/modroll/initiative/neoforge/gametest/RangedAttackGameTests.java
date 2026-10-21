package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.RangedAttackScenarios;

/** NeoForge registration shim: delegates to the shared {@link RangedAttackScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class RangedAttackGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "rangedHit")
    public void aPlayersArrowOnItsTurnRollsAndHits(GameTestHelper helper) {
        RangedAttackScenarios.aPlayersArrowOnItsTurnRollsAndHits(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedMiss")
    public void aMissedArrowFliesPastItsTarget(GameTestHelper helper) {
        RangedAttackScenarios.aMissedArrowFliesPastItsTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedOffTurn")
    public void firingOffTurnIsRefusedButLoadingIsNot(GameTestHelper helper) {
        RangedAttackScenarios.firingOffTurnIsRefusedButLoadingIsNot(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedNoAction")
    public void aSpentActionRefusesTheDraw(GameTestHelper helper) {
        RangedAttackScenarios.aSpentActionRefusesTheDraw(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedLowered")
    public void aDrawHeldPastTheTurnIsLowered(GameTestHelper helper) {
        RangedAttackScenarios.aDrawHeldPastTheTurnIsLowered(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedInMelee")
    public void aHostileInMeleeReachImposesDisadvantage(GameTestHelper helper) {
        RangedAttackScenarios.aHostileInMeleeReachImposesDisadvantage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedPastNormal")
    public void beyondNormalRangeImposesDisadvantage(GameTestHelper helper) {
        RangedAttackScenarios.beyondNormalRangeImposesDisadvantage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedPastLong")
    public void beyondLongRangeThereIsNoShot(GameTestHelper helper) {
        RangedAttackScenarios.beyondLongRangeThereIsNoShot(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedCover")
    public void coverRaisesTheTargetsArmorClass(GameTestHelper helper) {
        RangedAttackScenarios.coverRaisesTheTargetsArmorClass(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedOff")
    public void rangedAttacksOffLeaveTheShotCanceled(GameTestHelper helper) {
        RangedAttackScenarios.rangedAttacksOffLeaveTheShotCanceled(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedSkeleton")
    public void aSkeletonShootsOnceOnItsTurn(GameTestHelper helper) {
        RangedAttackScenarios.aSkeletonShootsOnceOnItsTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedPillager")
    public void aPillagerShootsOnceOnItsTurn(GameTestHelper helper) {
        RangedAttackScenarios.aPillagerShootsOnceOnItsTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedFlameHit")
    public void aFlamingArrowThatHitsSetsTheTargetAlight(GameTestHelper helper) {
        RangedAttackScenarios.aFlamingArrowThatHitsSetsTheTargetAlight(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedFlameMiss")
    public void aFlamingArrowThatMissesSetsNothingAlight(GameTestHelper helper) {
        RangedAttackScenarios.aFlamingArrowThatMissesSetsNothingAlight(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedSkeletonMiss", timeoutTicks = 200)
    public void aSkeletonsMissFliesPastThePlayer(GameTestHelper helper) {
        RangedAttackScenarios.aSkeletonsMissFliesPastThePlayer(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedPillagerMiss", timeoutTicks = 200)
    public void aPillagersMissFliesPastThePlayer(GameTestHelper helper) {
        RangedAttackScenarios.aPillagersMissFliesPastThePlayer(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedSkeletonMissOutside", timeoutTicks = 200)
    public void aSkeletonsMissOutsideAnEncounterFliesPast(GameTestHelper helper) {
        RangedAttackScenarios.aSkeletonsMissOutsideAnEncounterFliesPast(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedPillagerMissOutside", timeoutTicks = 200)
    public void aPillagersMissOutsideAnEncounterFliesPast(GameTestHelper helper) {
        RangedAttackScenarios.aPillagersMissOutsideAnEncounterFliesPast(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedSkeletonInFlight", timeoutTicks = 200)
    public void aSkeletonsArrowInFlightWhenTheFightFormsFliesPast(GameTestHelper helper) {
        RangedAttackScenarios.aSkeletonsArrowInFlightWhenTheFightFormsFliesPast(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedPillagerInFlight", timeoutTicks = 200)
    public void aPillagersArrowInFlightWhenTheFightFormsFliesPast(GameTestHelper helper) {
        RangedAttackScenarios.aPillagersArrowInFlightWhenTheFightFormsFliesPast(helper);
    }
}
