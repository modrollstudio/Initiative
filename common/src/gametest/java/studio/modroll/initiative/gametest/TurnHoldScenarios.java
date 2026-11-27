package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.BuiltinActions;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.Turns;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.turn.TurnOrder;

/** {@link Turns#holdUntil}: a hit on its way holds the fight, and it goes on once the hit lands. */
public final class TurnHoldScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, true, 1.0, 30.0, true);
    private static final TurnConfig QUICK_TURNS =
            new TurnConfig(true, 2, 2, 20.0, 12, false, false, false, Set.of(), true, true);
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, true, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0, true, 24.0, 64.0, 20);

    private TurnHoldScenarios() {}

    public static void aHeldFightWaitsForTheHit(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        TurnOrder order = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElseThrow()
                .turnOrder();
        boolean[] landed = {false};
        Turns.holdUntil(player, () -> landed[0]);
        ActionStatus endTurn = ActionRegistry.invoke(
                        helper.getLevel(), player, BuiltinActions.END_TURN, ActionRequest.none())
                .status();
        if (endTurn != ActionStatus.WAITING) {
            helper.fail("End Turn must wait for the hit, was " + endTurn);
        }
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionEconomy.ActionOutcome.WAITING) {
            helper.fail("an action must wait for the hit");
        }
        helper.startSequence()
                .thenExecuteAfter(6, () -> {
                    requireCurrent(helper, order, player, "a held turn past its timeout");
                    landed[0] = true;
                })
                .thenExecuteAfter(6, () -> {
                    if (order.round() == 1 && order.currentTurn().orElseThrow().equals(player.getUUID())) {
                        helper.fail("once the hit lands, the timeout must end the turn again");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    private static void requireCurrent(GameTestHelper helper, TurnOrder order, LivingEntity expected, String what) {
        if (!order.currentTurn().orElseThrow().equals(expected.getUUID())) {
            helper.fail(what + " must still be " + expected.getName().getString() + "'s");
        }
    }

    private static void prepare(GameTestHelper helper) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(QUICK_TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
