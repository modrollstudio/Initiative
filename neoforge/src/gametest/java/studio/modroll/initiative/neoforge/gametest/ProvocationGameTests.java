package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.ProvocationScenarios;

/** NeoForge registration shim: delegates to the shared {@link ProvocationScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class ProvocationGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "neutralTrigger")
    public void hittingANeutralMobStartsAnEncounter(GameTestHelper helper) {
        ProvocationScenarios.hittingANeutralMobStartsAnEncounter(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralTurns")
    public void aProvokedNeutralFightsThroughTheTurnOrder(GameTestHelper helper) {
        ProvocationScenarios.aProvokedNeutralFightsThroughTheTurnOrder(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralPassive")
    public void farmingAPassiveMobStartsNothing(GameTestHelper helper) {
        ProvocationScenarios.farmingAPassiveMobStartsNothing(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralJoin")
    public void aProvokedNeutralJoinsTheFightAlreadyRunning(GameTestHelper helper) {
        ProvocationScenarios.aProvokedNeutralJoinsTheFightAlreadyRunning(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralCalm")
    public void aNeutralThatCalmsDownLeavesCleanly(GameTestHelper helper) {
        ProvocationScenarios.aNeutralThatCalmsDownLeavesCleanly(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralAnger")
    public void angerKeepsAWolfInTheFightWithoutATarget(GameTestHelper helper) {
        ProvocationScenarios.angerKeepsAWolfInTheFightWithoutATarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralToggle")
    public void disabledFlagLeavesProvocationInRealTime(GameTestHelper helper) {
        ProvocationScenarios.disabledFlagLeavesProvocationInRealTime(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralBystander")
    public void attackingAnotherNeutralPullsItIntoTheFight(GameTestHelper helper) {
        ProvocationScenarios.attackingAnotherNeutralPullsItIntoTheFight(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralLivestock")
    public void livestockStaysHittableAndOutOfTheFight(GameTestHelper helper) {
        ProvocationScenarios.livestockStaysHittableAndOutOfTheFight(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neutralGateToggle")
    public void disabledFlagStillRefusesAnOutsiderSwing(GameTestHelper helper) {
        ProvocationScenarios.disabledFlagStillRefusesAnOutsiderSwing(helper);
    }
}
