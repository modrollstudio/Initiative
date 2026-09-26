package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.turn.InitiativeDerivation;
import studio.modroll.initiative.turn.InitiativeEntry;
import studio.modroll.initiative.turn.TurnOrder;

/**
 * M2a turn-order scenarios. The trigger radius is kept at 1 block so nothing is pulled by the
 * radius scan — every join happens through an explicit attack, which makes the join order (and
 * with it the scripted d20 draw order) fully deterministic: attacker first, then victim, in hurt
 * order. Each scenario runs in its own batch like the M1 scenarios.
 */
public final class TurnOrderScenarios {

    private static final EncounterConfig TIGHT_BUBBLE = new EncounterConfig(true, true, true, false, true, 1.0, 30.0);
    /** Freeze stays off here: M2a order semantics must hold without M2b's freeze in play. */
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());

    private TurnOrderScenarios() {}

    public static void forcedRollsProduceExactTurnOrder(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        formEncounter(helper, player, List.of(1, 20, 10), husk1, husk2);
        TurnOrder order = turnOrder(helper, player);
        requireOrder(helper, order, husk1, husk2, player);
        requireEntry(helper, order, husk1, 20);
        requireEntry(helper, order, husk2, 10);
        requireEntry(helper, order, player, 1);
        if (order.round() != 1) {
            helper.fail("a fresh encounter must start in round 1 but was in " + order.round());
        }
        if (!order.currentTurn().orElseThrow().equals(husk1.getUUID())) {
            helper.fail("the highest initiative must act first");
        }
        cleanUp(player, husk1, husk2);
        helper.succeed();
    }

    public static void endTurnWalksOrderAndRounds(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        formEncounter(helper, player, List.of(1, 20, 10), husk1, husk2);
        TurnOrder order = turnOrder(helper, player);
        requireCurrent(helper, order, husk1, "the first turn");
        order.endTurn();
        requireCurrent(helper, order, husk2, "the second turn");
        order.endTurn();
        requireCurrent(helper, order, player, "the last turn of the round");
        if (order.round() != 1) {
            helper.fail("the round must not change before the order is exhausted");
        }
        order.endTurn();
        requireCurrent(helper, order, husk1, "the first turn of round 2");
        if (order.round() != 2) {
            helper.fail("a full pass over the order must start round 2 but round was " + order.round());
        }
        cleanUp(player, husk1, husk2);
        helper.succeed();
    }

    public static void turnTimeoutAdvancesWithoutEndTurn(GameTestHelper helper) {
        prepare(helper, new TurnConfig(true, 2, 2, 20.0, 12, false, false, false, Set.of()));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        formEncounter(helper, player, List.of(1, 20), husk);
        TurnOrder order = turnOrder(helper, player);
        requireCurrent(helper, order, husk, "the first turn");
        helper.startSequence()
                .thenExecuteAfter(6, () -> {
                    if (order.round() < 2) {
                        helper.fail("the per-turn timeout must advance turns through round 2 on its own");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /**
     * The two timeouts are separate settings for a reason: a mob that cannot act must not hold the
     * table for a player's length. The player's own turn must be untouched by the short one.
     */
    public static void aMobsTurnTimesOutFasterThanAPlayers(GameTestHelper helper) {
        prepare(helper, new TurnConfig(true, 200, 20, 20.0, 12, false, false, false, Set.of()));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        formEncounter(helper, player, List.of(20, 1), husk);
        TurnOrder order = turnOrder(helper, player);
        requireCurrent(helper, order, player, "the first turn");
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    requireCurrent(helper, order, player, "the player's turn after 30 ticks");
                    order.endTurn();
                    requireCurrent(helper, order, husk, "the mob's turn");
                })
                .thenExecuteAfter(25, () -> {
                    if (husk.getUUID().equals(order.currentTurn().orElse(null))) {
                        helper.fail("a mob that cannot act must give the turn up on the short timeout");
                    }
                    requireCurrent(helper, order, player, "the turn after the mob timed out");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void lateJoinerInsertsWithoutStealingTheTurn(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        Husk late = ScenarioSupport.spawnHusk(helper, 6, 6);
        formEncounter(helper, player, List.of(1, 20, 10), husk1, husk2);
        TurnOrder order = turnOrder(helper, player);
        order.endTurn();
        requireCurrent(helper, order, husk2, "the turn before the late join");
        forceNaturals(
                helper,
                List.of(16),
                () -> late.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f));
        requireOrder(helper, order, husk1, late, husk2, player);
        requireCurrent(helper, order, husk2, "the turn after the late join");
        requireEntry(helper, order, late, 16);
        cleanUp(player, husk1, husk2, late);
        helper.succeed();
    }

    public static void removalUpdatesOrderAndCurrentTurn(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        Husk husk3 = ScenarioSupport.spawnHusk(helper, 6, 6);
        formEncounter(helper, player, List.of(1, 20, 15, 10), husk1, husk2, husk3);
        TurnOrder order = turnOrder(helper, player);
        order.endTurn();
        requireCurrent(helper, order, husk2, "the turn before any removal");
        helper.startSequence()
                .thenExecute(husk1::discard)
                .thenExecuteAfter(2, () -> {
                    requireOrder(helper, order, husk2, husk3, player);
                    requireCurrent(helper, order, husk2, "the turn after removing an earlier participant");
                    husk2.discard();
                })
                .thenExecuteAfter(2, () -> {
                    requireOrder(helper, order, husk3, player);
                    requireCurrent(helper, order, husk3, "the turn after removing the acting participant");
                    if (order.round() != 1) {
                        helper.fail("removals inside the round must not change the round number");
                    }
                    cleanUp(player, husk3);
                })
                .thenSucceed();
    }

    public static void equalRollsBreakTiesByJoinOrder(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        formEncounter(helper, player, List.of(1, 15, 15), husk1, husk2);
        TurnOrder order = turnOrder(helper, player);
        requireOrder(helper, order, husk1, husk2, player);
        cleanUp(player, husk1, husk2);
        helper.succeed();
    }

    private static void prepare(GameTestHelper helper, TurnConfig turns) {
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(turns);
        InitiativeConfig.overrideActionsForTesting(ScenarioSupport.ACTIONS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static void formEncounter(GameTestHelper helper, Player player, List<Integer> naturals, Husk... husks) {
        ScenarioSupport.formEncounter(helper, player, naturals, husks);
    }

    private static void forceNaturals(GameTestHelper helper, List<Integer> naturals, Runnable action) {
        ScenarioSupport.forceNaturals(helper, naturals, action);
    }

    private static TurnOrder turnOrder(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before its turn order can be inspected");
        }
        return encounter.turnOrder();
    }

    private static void requireOrder(GameTestHelper helper, TurnOrder order, LivingEntity... expected) {
        List<UUID> actual =
                order.order().stream().map(InitiativeEntry::participant).toList();
        List<UUID> wanted =
                List.of(expected).stream().map(LivingEntity::getUUID).toList();
        if (!actual.equals(wanted)) {
            helper.fail("turn order must be exactly " + wanted + " but was " + actual);
        }
    }

    private static void requireCurrent(GameTestHelper helper, TurnOrder order, LivingEntity expected, String when) {
        UUID current = order.currentTurn().orElse(null);
        if (!expected.getUUID().equals(current)) {
            helper.fail(when + " must belong to " + expected.getUUID() + " but was " + current);
        }
    }

    /** The entry's total must be the forced natural plus the movement-speed-derived bonus. */
    private static void requireEntry(GameTestHelper helper, TurnOrder order, LivingEntity entity, int natural) {
        InitiativeEntry entry = order.order().stream()
                .filter(candidate -> candidate.participant().equals(entity.getUUID()))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            helper.fail("every participant must have an initiative entry");
        }
        int bonus = InitiativeDerivation.bonus(
                entity.getAttributeValue(Attributes.MOVEMENT_SPEED), InitiativeConfig.turns());
        if (entry.bonus() != bonus || entry.total() != natural + bonus) {
            helper.fail("initiative must be natural " + natural + " + bonus " + bonus + " but was total "
                    + entry.total() + " + bonus " + entry.bonus());
        }
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
