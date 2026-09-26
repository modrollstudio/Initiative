package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.ActionOutcome;
import studio.modroll.initiative.action.ActionEconomy.AttackAttempt;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * M4a standard actions: Dash, Disengage, Dodge and Help. Dodge's disadvantage and Help's advantage
 * flow through Critfall's roll mode ({@code AttackContext.withMode}) — the scripted d20 faces prove
 * two dice roll and the right one is kept. The rest assert the turn/action gating and that the
 * per-turn flags are raised, spent and cleared without leaking past a turn boundary.
 */
public final class StandardActionScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    /** End-turn-when-spent off so the movement assertions are not cut short by an auto-end. */
    private static final ActionConfig DASH_ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    private static final ActionConfig DASH_DISABLED = new ActionConfig(
            true, 6.0, true, 4.0, true, true, false, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> HUSK_FIRST = List.of(1, 20);
    private static final List<Integer> HELPER_ALLY_HUSK = List.of(20, 1, 19);

    private StandardActionScenarios() {}

    public static void dashExtendsMovementAndStillStops(GameTestHelper helper) {
        prepare(helper, DASH_ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        requireAction(
                helper, ActionEconomy.dash(helper.getLevel(), player), ActionOutcome.PERFORMED, "a dash on your turn");
        if (Math.abs(encounter.budget().movementRemaining() - 12.0) > 1.0e-6) {
            helper.fail("dash must add the base budget on top; expected 12 but left "
                    + encounter.budget().movementRemaining());
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("dash must consume the action");
        }
        Vec3 start = player.position();
        helper.startSequence()
                .thenExecuteAfter(2, () -> player.moveTo(start.x + 9, start.y, start.z, 0, 0))
                .thenExecuteAfter(2, () -> {
                    double remaining = encounter.budget().movementRemaining();
                    if (Math.abs(remaining - 3.0) > 1.0e-6) {
                        helper.fail("a 9-block move must leave 3 of the extended budget but left " + remaining);
                    }
                    player.moveTo(start.x + 20, start.y, start.z, 0, 0);
                })
                .thenExecuteAfter(2, () -> {
                    if (Math.abs(player.getX() - (start.x + 9)) > 1.0e-6) {
                        helper.fail("a move past the extended budget must be reverted but the player is at "
                                + player.getX());
                    }
                    if (!encounter.budget().movementExhausted()) {
                        helper.fail("the over-budget attempt must burn the remaining movement");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void dodgingTargetIsAttackedWithDisadvantage(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        encounter.flags().setDodging(husk.getUUID());
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(15, 5);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
            ScenarioSupport.requireAttack(
                    helper, attempt.status(), AttackStatus.PERFORMED, "an attack on a dodging target");
            AttackResult result = attempt.result().orElseThrow();
            if (dice.d20Draws != 2) {
                helper.fail("disadvantage must roll two d20s but drew " + dice.d20Draws);
            }
            if (result.natural() != 5) {
                helper.fail("disadvantage must keep the lower die (5) but kept " + result.natural());
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void mobAttackingADodgingPlayerRollsWithDisadvantage(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        if (!husk.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the husk must lead the round for this scenario");
        }
        encounter.flags().setDodging(player.getUUID());
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(15, 5);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), husk, player);
            ScenarioSupport.requireAttack(
                    helper, attempt.status(), AttackStatus.PERFORMED, "a mob attack on a dodging player");
            AttackResult result = attempt.result().orElseThrow();
            if (dice.d20Draws != 2) {
                helper.fail("a mob attacking a dodging player must roll two d20s but drew " + dice.d20Draws);
            }
            if (result.natural() != 5) {
                helper.fail("disadvantage must keep the lower die (5) but kept " + result.natural());
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void helpedAllyAttacksWithAdvantageAndConsumesIt(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player helper1 = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player ally = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, HELPER_ALLY_HUSK, helper1, ally, husk);
        Encounter encounter = encounterOf(helper, helper1);
        if (!helper1.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the helper must lead the round for this scenario");
        }
        requireAction(
                helper, ActionEconomy.help(helper.getLevel(), helper1, ally), ActionOutcome.PERFORMED, "Help an ally");
        if (!encounter.flags().hasHelpAdvantage(ally.getUUID())) {
            helper.fail("Help must grant the ally pending advantage");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("Help must consume the helper's action");
        }
        if (!ActionEconomy.endTurn(helper.getLevel(), helper1)) {
            helper.fail("the helper must be able to end its turn");
        }
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(5, 15);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), ally, husk);
            ScenarioSupport.requireAttack(helper, attempt.status(), AttackStatus.PERFORMED, "the helped ally's attack");
            AttackResult result = attempt.result().orElseThrow();
            if (dice.d20Draws != 2) {
                helper.fail("advantage must roll two d20s but drew " + dice.d20Draws);
            }
            if (result.natural() != 15) {
                helper.fail("advantage must keep the higher die (15) but kept " + result.natural());
            }
            if (encounter.flags().hasHelpAdvantage(ally.getUUID())) {
                helper.fail("the ally's attack must consume the Help advantage");
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(helper1, ally, husk);
        helper.succeed();
    }

    public static void disengageSetsTheFlagAndSpendsTheAction(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        requireAction(
                helper, ActionEconomy.disengage(helper.getLevel(), player), ActionOutcome.PERFORMED, "a disengage");
        if (!encounter.flags().isDisengaged(player.getUUID())) {
            helper.fail("Disengage must raise the disengaged flag");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("Disengage must consume the action");
        }
        AttackAttempt second = ActionEconomy.attack(helper.getLevel(), player, husk);
        ScenarioSupport.requireAttack(
                helper, second.status(), AttackStatus.NO_ACTION, "an attack after the action is spent");
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void dodgeActionSetsTheFlagAndSpendsTheAction(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        requireAction(helper, ActionEconomy.dodge(helper.getLevel(), player), ActionOutcome.PERFORMED, "a dodge");
        if (!encounter.flags().isDodging(player.getUUID())) {
            helper.fail("Dodge must raise the dodging flag");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("Dodge must consume the action");
        }
        AttackAttempt second = ActionEconomy.attack(helper.getLevel(), player, husk);
        ScenarioSupport.requireAttack(
                helper, second.status(), AttackStatus.NO_ACTION, "an attack after the action is spent");
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void outOfTurnStandardActionIsRejected(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        requireAction(
                helper,
                ActionEconomy.dash(helper.getLevel(), player),
                ActionOutcome.NOT_YOUR_TURN,
                "an out-of-turn dash");
        requireAction(
                helper,
                ActionEconomy.dodge(helper.getLevel(), player),
                ActionOutcome.NOT_YOUR_TURN,
                "an out-of-turn dodge");
        requireAction(
                helper,
                ActionEconomy.disengage(helper.getLevel(), player),
                ActionOutcome.NOT_YOUR_TURN,
                "an out-of-turn disengage");
        requireAction(
                helper,
                ActionEconomy.help(helper.getLevel(), player, husk),
                ActionOutcome.NOT_YOUR_TURN,
                "an out-of-turn help");
        if (encounter.flags().isDodging(player.getUUID()) || encounter.flags().isDisengaged(player.getUUID())) {
            helper.fail("a rejected out-of-turn action must not raise any flag");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void disabledActionIsUnavailableWithOthersUnaffected(GameTestHelper helper) {
        prepare(helper, DASH_DISABLED);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        requireAction(helper, ActionEconomy.dash(helper.getLevel(), player), ActionOutcome.DISABLED, "a disabled dash");
        // A disabled Dash leaves the turn's action intact, so an enabled action still goes through
        // and spends it — the proof that Dash consumed nothing and the others are unaffected.
        requireAction(
                helper,
                ActionEconomy.dodge(helper.getLevel(), player),
                ActionOutcome.PERFORMED,
                "dodge with dash disabled");
        if (!encounter.flags().isDodging(player.getUUID()) || encounter.budget().actionAvailable()) {
            helper.fail("disabling one action must leave the others working");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void dodgeClearsAtTheDodgersNextTurn(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        requireAction(helper, ActionEconomy.dodge(helper.getLevel(), player), ActionOutcome.PERFORMED, "a dodge");
        if (!ActionEconomy.endTurn(helper.getLevel(), player)) {
            helper.fail("the player must be able to end its turn");
        }
        if (!encounter.flags().isDodging(player.getUUID())) {
            helper.fail("dodging must persist through the other participants' turns");
        }
        if (!ActionEconomy.endTurn(helper.getLevel(), husk)) {
            helper.fail("the husk must be able to end its turn to wrap the round");
        }
        ActionEconomy.tick(helper.getLevel(), encounter);
        if (encounter.flags().isDodging(player.getUUID())) {
            helper.fail("dodging must clear at the dodger's next turn start");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    private static void prepare(GameTestHelper helper, ActionConfig actions) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static Encounter encounterOf(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void requireAction(
            GameTestHelper helper, ActionOutcome actual, ActionOutcome expected, String what) {
        if (actual != expected) {
            helper.fail(what + " must be " + expected + " but was " + actual);
        }
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
