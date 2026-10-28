package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.JoinScenarios;

/** Fabric registration shim: delegates to the shared {@link JoinScenarios} bodies. */
public class JoinGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "joinZombie")
    public void aZombieJoinsOnItsHitAndHitsNoMore(GameTestHelper helper) {
        JoinScenarios.aZombieJoinsOnItsHitAndHitsNoMore(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "joinPlayer")
    public void aPlayerJoinsOnTheirHitAndHitsNoMore(GameTestHelper helper) {
        JoinScenarios.aPlayerJoinsOnTheirHitAndHitsNoMore(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "joinSkeleton")
    public void aSkeletonJoinsOnItsShotAndShootsNoMore(GameTestHelper helper) {
        JoinScenarios.aSkeletonJoinsOnItsShotAndShootsNoMore(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "joinPastLeaveRadius")
    public void anAttackerFromPastTheLeaveRadiusStaysInTheFight(GameTestHelper helper) {
        JoinScenarios.anAttackerFromPastTheLeaveRadiusStaysInTheFight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dimensionTurnPasses")
    public void aPlayerWhoChangesDimensionLeavesAtOnce(GameTestHelper helper) {
        JoinScenarios.aPlayerWhoChangesDimensionLeavesAtOnce(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dimensionEndsFight")
    public void theLastPlayerChangingDimensionEndsTheFight(GameTestHelper helper) {
        JoinScenarios.theLastPlayerChangingDimensionEndsTheFight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "openingZombieMiss")
    public void aZombiesOpeningSwingRollsAndCanMiss(GameTestHelper helper) {
        JoinScenarios.aZombiesOpeningSwingRollsAndCanMiss(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "openingSkeletonMiss")
    public void aSkeletonsOpeningArrowRollsAndCanMiss(GameTestHelper helper) {
        JoinScenarios.aSkeletonsOpeningArrowRollsAndCanMiss(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "openingPlayerSwingMiss")
    public void aPlayersOpeningSwingRollsAndCanMiss(GameTestHelper helper) {
        JoinScenarios.aPlayersOpeningSwingRollsAndCanMiss(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "openingPlayerArrowMiss")
    public void aPlayersOpeningArrowRollsAndCanMiss(GameTestHelper helper) {
        JoinScenarios.aPlayersOpeningArrowRollsAndCanMiss(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "openingZombieHit")
    public void anOpeningSwingThatHitsDealsRolledDamage(GameTestHelper helper) {
        JoinScenarios.anOpeningSwingThatHitsDealsRolledDamage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "openingInitiativeFirst")
    public void anOpeningAttackRollsWhenInitiativesListenerRunsFirst(GameTestHelper helper) {
        JoinScenarios.anOpeningAttackRollsWhenInitiativesListenerRunsFirst(helper);
    }
}
