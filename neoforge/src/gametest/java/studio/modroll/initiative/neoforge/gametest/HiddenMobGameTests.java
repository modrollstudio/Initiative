package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.HiddenMobScenarios;

/** NeoForge registration shim: delegates to the shared {@link HiddenMobScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class HiddenMobGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(
            template = TEMPLATE,
            batch = "hiddenMobAHiddenJoinerIsKeptFromThePlayersUntilItAttacks",
            timeoutTicks = 200)
    public void aHiddenJoinerIsKeptFromThePlayersUntilItAttacks(GameTestHelper helper) {
        HiddenMobScenarios.aHiddenJoinerIsKeptFromThePlayersUntilItAttacks(helper);
    }

    @GameTest(template = TEMPLATE, batch = "hiddenMobAHitFindsAHiddenMob", timeoutTicks = 200)
    public void aHitFindsAHiddenMob(GameTestHelper helper) {
        HiddenMobScenarios.aHitFindsAHiddenMob(helper);
    }

    @GameTest(template = TEMPLATE, batch = "hiddenMobWithChecksAKeenEyeFindsAHiddenMob", timeoutTicks = 200)
    public void withChecksAKeenEyeFindsAHiddenMob(GameTestHelper helper) {
        HiddenMobScenarios.withChecksAKeenEyeFindsAHiddenMob(helper);
    }

    @GameTest(template = TEMPLATE, batch = "hiddenMobAnAmbushCatchesThePlayerBySurprise", timeoutTicks = 200)
    public void anAmbushCatchesThePlayerBySurprise(GameTestHelper helper) {
        HiddenMobScenarios.anAmbushCatchesThePlayerBySurprise(helper);
    }

    @GameTest(template = TEMPLATE, batch = "hiddenMobWithSurpriseOffAnAmbushRollsOneDie", timeoutTicks = 200)
    public void withSurpriseOffAnAmbushRollsOneDie(GameTestHelper helper) {
        HiddenMobScenarios.withSurpriseOffAnAmbushRollsOneDie(helper);
    }

    @GameTest(template = TEMPLATE, batch = "hiddenMobWithChecksASharpPlayerIsNotSurprised", timeoutTicks = 200)
    public void withChecksASharpPlayerIsNotSurprised(GameTestHelper helper) {
        HiddenMobScenarios.withChecksASharpPlayerIsNotSurprised(helper);
    }

    @GameTest(template = TEMPLATE, batch = "hiddenMobAnAmbushWithEncountersOffStartsNothing", timeoutTicks = 200)
    public void anAmbushWithEncountersOffStartsNothing(GameTestHelper helper) {
        HiddenMobScenarios.anAmbushWithEncountersOffStartsNothing(helper);
    }
}
