package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.TurnOrderScenarios;

/** NeoForge registration shim: delegates to the shared {@link TurnOrderScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class TurnOrderGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m2aOrder")
    public void forcedRollsProduceExactTurnOrder(GameTestHelper helper) {
        TurnOrderScenarios.forcedRollsProduceExactTurnOrder(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2aRounds")
    public void endTurnWalksOrderAndRounds(GameTestHelper helper) {
        TurnOrderScenarios.endTurnWalksOrderAndRounds(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2aTimeout")
    public void turnTimeoutAdvancesWithoutEndTurn(GameTestHelper helper) {
        TurnOrderScenarios.turnTimeoutAdvancesWithoutEndTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2aLateJoin")
    public void lateJoinerInsertsWithoutStealingTheTurn(GameTestHelper helper) {
        TurnOrderScenarios.lateJoinerInsertsWithoutStealingTheTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2aRemoval")
    public void removalUpdatesOrderAndCurrentTurn(GameTestHelper helper) {
        TurnOrderScenarios.removalUpdatesOrderAndCurrentTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2aTieBreak")
    public void equalRollsBreakTiesByJoinOrder(GameTestHelper helper) {
        TurnOrderScenarios.equalRollsBreakTiesByJoinOrder(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2aMobTimeout")
    public void aMobsTurnTimesOutFasterThanAPlayers(GameTestHelper helper) {
        TurnOrderScenarios.aMobsTurnTimesOutFasterThanAPlayers(helper);
    }
}
