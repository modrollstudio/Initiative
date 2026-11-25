package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.HiddenMobScenarios;

/** Fabric registration shim: delegates to the shared {@link HiddenMobScenarios} bodies. */
public class HiddenMobGameTests implements FabricGameTest {

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "hiddenMobAHiddenJoinerIsKeptFromThePlayersUntilItAttacks",
            timeoutTicks = 200)
    public void aHiddenJoinerIsKeptFromThePlayersUntilItAttacks(GameTestHelper helper) {
        HiddenMobScenarios.aHiddenJoinerIsKeptFromThePlayersUntilItAttacks(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "hiddenMobAHitFindsAHiddenMob", timeoutTicks = 200)
    public void aHitFindsAHiddenMob(GameTestHelper helper) {
        HiddenMobScenarios.aHitFindsAHiddenMob(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "hiddenMobWithChecksAKeenEyeFindsAHiddenMob",
            timeoutTicks = 200)
    public void withChecksAKeenEyeFindsAHiddenMob(GameTestHelper helper) {
        HiddenMobScenarios.withChecksAKeenEyeFindsAHiddenMob(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "hiddenMobAnAmbushCatchesThePlayerBySurprise",
            timeoutTicks = 200)
    public void anAmbushCatchesThePlayerBySurprise(GameTestHelper helper) {
        HiddenMobScenarios.anAmbushCatchesThePlayerBySurprise(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "hiddenMobWithSurpriseOffAnAmbushRollsOneDie",
            timeoutTicks = 200)
    public void withSurpriseOffAnAmbushRollsOneDie(GameTestHelper helper) {
        HiddenMobScenarios.withSurpriseOffAnAmbushRollsOneDie(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "hiddenMobWithChecksASharpPlayerIsNotSurprised",
            timeoutTicks = 200)
    public void withChecksASharpPlayerIsNotSurprised(GameTestHelper helper) {
        HiddenMobScenarios.withChecksASharpPlayerIsNotSurprised(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "hiddenMobAnAmbushWithEncountersOffStartsNothing",
            timeoutTicks = 200)
    public void anAmbushWithEncountersOffStartsNothing(GameTestHelper helper) {
        HiddenMobScenarios.anAmbushWithEncountersOffStartsNothing(helper);
    }
}
