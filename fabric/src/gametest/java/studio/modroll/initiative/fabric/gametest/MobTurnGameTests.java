package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.MobTurnScenarios;

/** Fabric registration shim: delegates to the shared {@link MobTurnScenarios} bodies. */
public class MobTurnGameTests implements FabricGameTest {

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mobTurnAMobWithTwoAttacksMakesBothOnItsTurn",
            timeoutTicks = 200)
    public void aMobWithTwoAttacksMakesBothOnItsTurn(GameTestHelper helper) {
        MobTurnScenarios.aMobWithTwoAttacksMakesBothOnItsTurn(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mobTurnAMobWithOneAttackMakesOneAsBefore",
            timeoutTicks = 200)
    public void aMobWithOneAttackMakesOneAsBefore(GameTestHelper helper) {
        MobTurnScenarios.aMobWithOneAttackMakesOneAsBefore(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mobTurnAHandlerTakesItsOwnActionsThroughTheGate",
            timeoutTicks = 200)
    public void aHandlerTakesItsOwnActionsThroughTheGate(GameTestHelper helper) {
        MobTurnScenarios.aHandlerTakesItsOwnActionsThroughTheGate(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mobTurnAHandlerThatNeverFinishesEndsOnTheTimeout",
            timeoutTicks = 200)
    public void aHandlerThatNeverFinishesEndsOnTheTimeout(GameTestHelper helper) {
        MobTurnScenarios.aHandlerThatNeverFinishesEndsOnTheTimeout(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mobTurnAHandlerThatThrowsEndsTheTurn",
            timeoutTicks = 200)
    public void aHandlerThatThrowsEndsTheTurn(GameTestHelper helper) {
        MobTurnScenarios.aHandlerThatThrowsEndsTheTurn(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mobTurnWithCustomMobTurnsOffTheMobTakesInitiativesTurn",
            timeoutTicks = 200)
    public void withCustomMobTurnsOffTheMobTakesInitiativesTurn(GameTestHelper helper) {
        MobTurnScenarios.withCustomMobTurnsOffTheMobTakesInitiativesTurn(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mobTurnAReactionOutOfTurnSpendsTheMobsReactionOnly",
            timeoutTicks = 200)
    public void aReactionOutOfTurnSpendsTheMobsReactionOnly(GameTestHelper helper) {
        MobTurnScenarios.aReactionOutOfTurnSpendsTheMobsReactionOnly(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "mobTurnALaunchedPlayerFliesAndIsHeldWhereItLands",
            timeoutTicks = 200)
    public void aLaunchedPlayerFliesAndIsHeldWhereItLands(GameTestHelper helper) {
        MobTurnScenarios.aLaunchedPlayerFliesAndIsHeldWhereItLands(helper);
    }
}
