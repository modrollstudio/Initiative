package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.TurnOrderScenarios;

/** Fabric registration shim: delegates to the shared {@link TurnOrderScenarios} bodies. */
public class TurnOrderGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2aOrder")
    public void forcedRollsProduceExactTurnOrder(GameTestHelper helper) {
        TurnOrderScenarios.forcedRollsProduceExactTurnOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2aRounds")
    public void endTurnWalksOrderAndRounds(GameTestHelper helper) {
        TurnOrderScenarios.endTurnWalksOrderAndRounds(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2aTimeout")
    public void turnTimeoutAdvancesWithoutEndTurn(GameTestHelper helper) {
        TurnOrderScenarios.turnTimeoutAdvancesWithoutEndTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2aLateJoin")
    public void lateJoinerInsertsWithoutStealingTheTurn(GameTestHelper helper) {
        TurnOrderScenarios.lateJoinerInsertsWithoutStealingTheTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2aRemoval")
    public void removalUpdatesOrderAndCurrentTurn(GameTestHelper helper) {
        TurnOrderScenarios.removalUpdatesOrderAndCurrentTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2aTieBreak")
    public void equalRollsBreakTiesByJoinOrder(GameTestHelper helper) {
        TurnOrderScenarios.equalRollsBreakTiesByJoinOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2aMobTimeout")
    public void aMobsTurnTimesOutFasterThanAPlayers(GameTestHelper helper) {
        TurnOrderScenarios.aMobsTurnTimesOutFasterThanAPlayers(helper);
    }
}
