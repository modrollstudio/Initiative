package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.MobTurnScenarios;

/** NeoForge registration shim: delegates to the shared {@link MobTurnScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class MobTurnGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "mobTurnAMobWithTwoAttacksMakesBothOnItsTurn", timeoutTicks = 200)
    public void aMobWithTwoAttacksMakesBothOnItsTurn(GameTestHelper helper) {
        MobTurnScenarios.aMobWithTwoAttacksMakesBothOnItsTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "mobTurnAMobWithOneAttackMakesOneAsBefore", timeoutTicks = 200)
    public void aMobWithOneAttackMakesOneAsBefore(GameTestHelper helper) {
        MobTurnScenarios.aMobWithOneAttackMakesOneAsBefore(helper);
    }

    @GameTest(template = TEMPLATE, batch = "mobTurnAHandlerTakesItsOwnActionsThroughTheGate", timeoutTicks = 200)
    public void aHandlerTakesItsOwnActionsThroughTheGate(GameTestHelper helper) {
        MobTurnScenarios.aHandlerTakesItsOwnActionsThroughTheGate(helper);
    }

    @GameTest(template = TEMPLATE, batch = "mobTurnAHandlerThatNeverFinishesEndsOnTheTimeout", timeoutTicks = 200)
    public void aHandlerThatNeverFinishesEndsOnTheTimeout(GameTestHelper helper) {
        MobTurnScenarios.aHandlerThatNeverFinishesEndsOnTheTimeout(helper);
    }

    @GameTest(template = TEMPLATE, batch = "mobTurnAHandlerThatThrowsEndsTheTurn", timeoutTicks = 200)
    public void aHandlerThatThrowsEndsTheTurn(GameTestHelper helper) {
        MobTurnScenarios.aHandlerThatThrowsEndsTheTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "mobTurnWithCustomMobTurnsOffTheMobTakesInitiativesTurn", timeoutTicks = 200)
    public void withCustomMobTurnsOffTheMobTakesInitiativesTurn(GameTestHelper helper) {
        MobTurnScenarios.withCustomMobTurnsOffTheMobTakesInitiativesTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "mobTurnAReactionOutOfTurnSpendsTheMobsReactionOnly", timeoutTicks = 200)
    public void aReactionOutOfTurnSpendsTheMobsReactionOnly(GameTestHelper helper) {
        MobTurnScenarios.aReactionOutOfTurnSpendsTheMobsReactionOnly(helper);
    }

    @GameTest(template = TEMPLATE, batch = "mobTurnALaunchedPlayerFliesAndIsHeldWhereItLands", timeoutTicks = 200)
    public void aLaunchedPlayerFliesAndIsHeldWhereItLands(GameTestHelper helper) {
        MobTurnScenarios.aLaunchedPlayerFliesAndIsHeldWhereItLands(helper);
    }
}
