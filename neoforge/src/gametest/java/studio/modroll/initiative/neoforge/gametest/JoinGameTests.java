package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.JoinScenarios;

/** NeoForge registration shim: delegates to the shared {@link JoinScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class JoinGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "joinZombie")
    public void aZombieJoinsOnItsHitAndHitsNoMore(GameTestHelper helper) {
        JoinScenarios.aZombieJoinsOnItsHitAndHitsNoMore(helper);
    }

    @GameTest(template = TEMPLATE, batch = "joinPlayer")
    public void aPlayerJoinsOnTheirHitAndHitsNoMore(GameTestHelper helper) {
        JoinScenarios.aPlayerJoinsOnTheirHitAndHitsNoMore(helper);
    }

    @GameTest(template = TEMPLATE, batch = "joinSkeleton")
    public void aSkeletonJoinsOnItsShotAndShootsNoMore(GameTestHelper helper) {
        JoinScenarios.aSkeletonJoinsOnItsShotAndShootsNoMore(helper);
    }

    @GameTest(template = TEMPLATE, batch = "joinPastLeaveRadius")
    public void anAttackerFromPastTheLeaveRadiusStaysInTheFight(GameTestHelper helper) {
        JoinScenarios.anAttackerFromPastTheLeaveRadiusStaysInTheFight(helper);
    }

    @GameTest(template = TEMPLATE, batch = "dimensionTurnPasses")
    public void aPlayerWhoChangesDimensionLeavesAtOnce(GameTestHelper helper) {
        JoinScenarios.aPlayerWhoChangesDimensionLeavesAtOnce(helper);
    }

    @GameTest(template = TEMPLATE, batch = "dimensionEndsFight")
    public void theLastPlayerChangingDimensionEndsTheFight(GameTestHelper helper) {
        JoinScenarios.theLastPlayerChangingDimensionEndsTheFight(helper);
    }

    @GameTest(template = TEMPLATE, batch = "openingZombieMiss")
    public void aZombiesOpeningSwingRollsAndCanMiss(GameTestHelper helper) {
        JoinScenarios.aZombiesOpeningSwingRollsAndCanMiss(helper);
    }

    @GameTest(template = TEMPLATE, batch = "openingSkeletonMiss")
    public void aSkeletonsOpeningArrowRollsAndCanMiss(GameTestHelper helper) {
        JoinScenarios.aSkeletonsOpeningArrowRollsAndCanMiss(helper);
    }

    @GameTest(template = TEMPLATE, batch = "openingPlayerSwingMiss")
    public void aPlayersOpeningSwingRollsAndCanMiss(GameTestHelper helper) {
        JoinScenarios.aPlayersOpeningSwingRollsAndCanMiss(helper);
    }

    @GameTest(template = TEMPLATE, batch = "openingPlayerArrowMiss")
    public void aPlayersOpeningArrowRollsAndCanMiss(GameTestHelper helper) {
        JoinScenarios.aPlayersOpeningArrowRollsAndCanMiss(helper);
    }

    @GameTest(template = TEMPLATE, batch = "openingZombieHit")
    public void anOpeningSwingThatHitsDealsRolledDamage(GameTestHelper helper) {
        JoinScenarios.anOpeningSwingThatHitsDealsRolledDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "openingInitiativeFirst")
    public void anOpeningAttackRollsWhenInitiativesListenerRunsFirst(GameTestHelper helper) {
        JoinScenarios.anOpeningAttackRollsWhenInitiativesListenerRunsFirst(helper);
    }
}
