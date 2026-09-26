package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.ProvocationScenarios;

/** Fabric registration shim: delegates to the shared {@link ProvocationScenarios} bodies. */
public class ProvocationGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralTrigger")
    public void hittingANeutralMobStartsAnEncounter(GameTestHelper helper) {
        ProvocationScenarios.hittingANeutralMobStartsAnEncounter(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralTurns")
    public void aProvokedNeutralFightsThroughTheTurnOrder(GameTestHelper helper) {
        ProvocationScenarios.aProvokedNeutralFightsThroughTheTurnOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralPassive")
    public void farmingAPassiveMobStartsNothing(GameTestHelper helper) {
        ProvocationScenarios.farmingAPassiveMobStartsNothing(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralJoin")
    public void aProvokedNeutralJoinsTheFightAlreadyRunning(GameTestHelper helper) {
        ProvocationScenarios.aProvokedNeutralJoinsTheFightAlreadyRunning(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralCalm")
    public void aNeutralThatCalmsDownLeavesCleanly(GameTestHelper helper) {
        ProvocationScenarios.aNeutralThatCalmsDownLeavesCleanly(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralAnger")
    public void angerKeepsAWolfInTheFightWithoutATarget(GameTestHelper helper) {
        ProvocationScenarios.angerKeepsAWolfInTheFightWithoutATarget(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralToggle")
    public void disabledFlagLeavesProvocationInRealTime(GameTestHelper helper) {
        ProvocationScenarios.disabledFlagLeavesProvocationInRealTime(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralBystander")
    public void attackingAnotherNeutralPullsItIntoTheFight(GameTestHelper helper) {
        ProvocationScenarios.attackingAnotherNeutralPullsItIntoTheFight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralLivestock")
    public void livestockStaysHittableAndOutOfTheFight(GameTestHelper helper) {
        ProvocationScenarios.livestockStaysHittableAndOutOfTheFight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neutralGateToggle")
    public void disabledFlagStillRefusesAnOutsiderSwing(GameTestHelper helper) {
        ProvocationScenarios.disabledFlagStillRefusesAnOutsiderSwing(helper);
    }
}
