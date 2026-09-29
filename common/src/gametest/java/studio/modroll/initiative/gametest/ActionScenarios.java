package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackOutcome;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.action.BuiltinActions;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * M3 action economy scenarios: the Attack action drives Critfall's d20 turn-ordered inside
 * encounters. The scripted roller forces exact naturals and counts draws, so these tests prove the
 * d20 really rolled (and rolled exactly once — no double roll from the suppressed automatic
 * pipeline). Freeze stays off: economy semantics must hold without M2b in play, and the mob-turn
 * driver acts independently of mob AI.
 */
public final class ActionScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    /** Player natural drawn first (attacker joins first), so (20, 1) puts the player's turn first. */
    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);

    private static final List<Integer> HUSK_FIRST = List.of(1, 20);

    /** Drawn in join order (first attacker, husk, second attacker): both players act before the husk. */
    private static final List<Integer> FOCUS_FIRE_ORDER = List.of(20, 1, 19);

    private ActionScenarios() {}

    public static void attackOnYourTurnRollsTheD20(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(true);
        try {
            ActionEconomy.AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
            requireStatus(helper, attempt.status(), AttackStatus.PERFORMED, "an on-turn attack");
            AttackResult result = attempt.result().orElseThrow();
            if (result.natural() != 20) {
                helper.fail("forced-max dice must roll a natural 20 but rolled " + result.natural());
            }
            if (result.outcome() != AttackOutcome.CRIT) {
                helper.fail("a natural 20 must crit but was " + result.outcome());
            }
            if (result.damage() <= 0) {
                helper.fail("a crit must deal damage but dealt " + result.damage());
            }
            if (dice.d20Draws != 1) {
                helper.fail("one driven attack must roll exactly one d20 but drew " + dice.d20Draws);
            }
            if (husk.isAlive() && husk.getHealth() >= before) {
                helper.fail("the driven crit must apply its damage to the target (rolled " + result.damage()
                        + ", health " + before + " -> " + husk.getHealth() + ")");
            }
        } finally {
            RollService.resetRoller();
        }
        Encounter encounter = encounterOf(helper, player);
        if (encounter.budget().actionAvailable()) {
            helper.fail("attacking must consume the action");
        }
        if (!player.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the player must keep the turn while movement remains");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /**
     * The Critfall 0.2.3 payoff: two attackers focus one target in the same round and both driven
     * hits land full damage. The target still carries i-frames from the forming hit and from the
     * first driven hit, so before 0.2.3 the second strike would have been swallowed. Driven damage
     * now bypasses i-frames in Critfall, so the target loses exactly both rolled amounts.
     */
    public static void focusFireLandsBothDrivenHits(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player first = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player second = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, FOCUS_FIRE_ORDER, first, second, husk);
        Encounter encounter = encounterOf(helper, first);
        if (!first.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the first attacker must lead the round for the focus-fire case");
        }
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(true);
        try {
            AttackResult firstHit = requirePerformed(helper, first, husk, "the first focus-fire attack");
            if (!ActionEconomy.endTurn(helper.getLevel(), first)) {
                helper.fail("the first attacker must be able to end its turn");
            }
            AttackResult secondHit = requirePerformed(helper, second, husk, "the second focus-fire attack");
            if (dice.d20Draws != 2) {
                helper.fail("two driven attacks must roll exactly two d20s but drew " + dice.d20Draws);
            }
            if (firstHit.damage() <= 0 || secondHit.damage() <= 0) {
                helper.fail("both focus-fire crits must deal damage");
            }
            float expectedDrop = firstHit.damage() + secondHit.damage();
            float actualDrop = before - husk.getHealth();
            if (Math.abs(actualDrop - expectedDrop) > 1.0e-4) {
                helper.fail("both driven hits must land full damage despite i-frames; expected the target" + " to lose "
                        + expectedDrop + " but it lost " + actualDrop);
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(first, second, husk);
        helper.succeed();
    }

    private static AttackResult requirePerformed(GameTestHelper helper, Player attacker, Husk target, String what) {
        ActionEconomy.AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), attacker, target);
        requireStatus(helper, attempt.status(), AttackStatus.PERFORMED, what);
        return attempt.result().orElseThrow();
    }

    public static void forcedNatOneMissesWithoutDamage(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(false);
        try {
            ActionEconomy.AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
            requireStatus(helper, attempt.status(), AttackStatus.PERFORMED, "an on-turn attack");
            AttackResult result = attempt.result().orElseThrow();
            if (result.natural() != 1) {
                helper.fail("forced-min dice must roll a natural 1 but rolled " + result.natural());
            }
            if (result.outcome() != AttackOutcome.MISS && result.outcome() != AttackOutcome.FUMBLE) {
                helper.fail("a natural 1 must miss or fumble but was " + result.outcome());
            }
            if (result.damage() != 0) {
                helper.fail("a miss must deal no damage but dealt " + result.damage());
            }
            if (husk.getHealth() != before) {
                helper.fail("a missed attack must not damage the target");
            }
            if (dice.d20Draws < 1) {
                helper.fail("the miss must still have rolled the d20");
            }
        } finally {
            RollService.resetRoller();
        }
        if (encounterOf(helper, player).budget().actionAvailable()) {
            helper.fail("a missed attack must still consume the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /** A melee swing reaches as far as the config says and not a block further. */
    public static void attackBeyondReachIsRejectedWithoutARoll(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 6, 6);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(true);
        try {
            ActionResult result =
                    ActionRegistry.invoke(helper.getLevel(), player, BuiltinActions.ATTACK, ActionRequest.of(husk));
            if (result.status() != ActionStatus.OUT_OF_REACH) {
                helper.fail("an attack past its reach must be OUT_OF_REACH but was " + result.status());
            }
            if (dice.d20Draws != 0) {
                helper.fail("an out-of-reach attack must not roll but drew " + dice.d20Draws + " d20s");
            }
            if (husk.getHealth() != before) {
                helper.fail("an out-of-reach attack must not damage the target");
            }
        } finally {
            RollService.resetRoller();
        }
        if (!encounterOf(helper, player).budget().actionAvailable()) {
            helper.fail("a rejected attack must leave the turn's action intact");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void outOfTurnAttackIsRejectedWithoutARoll(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(true);
        try {
            ActionEconomy.AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
            requireStatus(helper, attempt.status(), AttackStatus.NOT_YOUR_TURN, "an out-of-turn attack");
            if (dice.d20Draws != 0) {
                helper.fail("an out-of-turn attack must not roll but drew " + dice.d20Draws + " d20s");
            }
            if (husk.getHealth() != before) {
                helper.fail("an out-of-turn attack must not damage the target");
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void secondAttackInOneTurnIsRejected(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(true);
        try {
            ActionEconomy.AttackAttempt first = ActionEconomy.attack(helper.getLevel(), player, husk);
            requireStatus(helper, first.status(), AttackStatus.PERFORMED, "the first attack of the turn");
            ActionEconomy.AttackAttempt second = ActionEconomy.attack(helper.getLevel(), player, husk);
            requireStatus(helper, second.status(), AttackStatus.NO_ACTION, "a second attack with no action left");
            if (dice.d20Draws != 1) {
                helper.fail("one turn must produce exactly one d20 but drew " + dice.d20Draws);
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void explicitEndTurnAdvancesOnlyForTheActor(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        if (!ActionEconomy.endTurn(helper.getLevel(), player)) {
            helper.fail("the acting player must be able to end its turn");
        }
        if (!husk.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("ending the turn must advance to the next participant");
        }
        if (ActionEconomy.endTurn(helper.getLevel(), player)) {
            helper.fail("a participant must not end someone else's turn");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void movementBudgetIsConsumedAndEnforced(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Vec3 start = player.position();
        helper.startSequence()
                .thenExecuteAfter(2, () -> player.moveTo(start.x + 3, start.y, start.z, 0, 0))
                .thenExecuteAfter(2, () -> {
                    double remaining = encounter.budget().movementRemaining();
                    if (Math.abs(remaining - 3.0) > 1.0e-6) {
                        helper.fail("a 3-block move must leave 3 blocks of budget but left " + remaining);
                    }
                    player.moveTo(start.x + 13, start.y, start.z, 0, 0);
                })
                .thenExecuteAfter(2, () -> {
                    if (Math.abs(player.getX() - (start.x + 3)) > 1.0e-6) {
                        helper.fail("an over-budget move must be reverted but the player is at " + player.getX());
                    }
                    if (!encounter.budget().movementExhausted()) {
                        helper.fail("the over-budget attempt must burn the remaining movement");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void knockbackDuringNewTurnPreservesMovementBudget(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        ScenarioSupport.forceCountedDice(true);
        try {
            ActionEconomy.tick(helper.getLevel(), encounter);
        } finally {
            RollService.resetRoller();
        }
        if (!player.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null)) || player.hurtTime <= 0) {
            helper.fail("the mob's hit must pass the turn to the player while the player is still hurt");
        }
        ActionEconomy.tick(helper.getLevel(), encounter);
        Vec3 start = player.position();
        for (int step = 1; step <= 2; step++) {
            player.hurtTime = 3 - step;
            Vec3 displaced = start.add(step * 2.0, 0, 0);
            player.moveTo(displaced.x, displaced.y, displaced.z, 0, 0);
            ActionEconomy.tick(helper.getLevel(), encounter);
            if (Math.abs(encounter.budget().movementRemaining() - 6.0) > 1.0e-6) {
                helper.fail("knockback continuing into the player's turn must not spend movement");
            }
            if (!displaced.equals(encounter.movementAnchor())) {
                helper.fail("each hurt tick must re-anchor movement at the displaced position");
            }
        }
        player.hurtTime = 0;
        player.moveTo(start.x + 5, start.y, start.z, 0, 0);
        ActionEconomy.tick(helper.getLevel(), encounter);
        if (Math.abs(encounter.budget().movementRemaining() - 5.0) > 1.0e-6) {
            helper.fail("walking after knockback must spend only the distance since the last hurt tick");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void knockbackWithExhaustedMovementIsNotClamped(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        ActionEconomy.tick(helper.getLevel(), encounter);
        encounter.budget().consumeMovement(6.0);
        Vec3 displaced = player.position().add(3, 0, 0);
        player.hurtTime = 1;
        player.moveTo(displaced.x, displaced.y, displaced.z, 0, 0);
        ActionEconomy.tick(helper.getLevel(), encounter);
        if (!displaced.equals(player.position()) || !displaced.equals(encounter.movementAnchor())) {
            helper.fail("an exhausted budget must not revert knockback or retain the pre-hit anchor");
        }
        player.hurtTime = 0;
        player.moveTo(displaced.x + 1, displaced.y, displaced.z, 0, 0);
        ActionEconomy.tick(helper.getLevel(), encounter);
        if (!displaced.equals(player.position()) || !encounter.budget().movementExhausted()) {
            helper.fail("ordinary movement must still be clamped after knockback ends");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void mobTurnDrivesItsAttackThroughTheApi(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        // Inside the husk's melee reach: it spawns at the block center (2.5, 1.5).
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        float before = player.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(true);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    RollService.resetRoller();
                    if (dice.d20Draws != 1) {
                        helper.fail("the mob turn must drive exactly one d20 but drew " + dice.d20Draws);
                    }
                    if (player.getHealth() >= before) {
                        helper.fail("the mob's forced crit must damage the player");
                    }
                    Encounter encounter = encounterOf(helper, player);
                    if (!player.getUUID()
                            .equals(encounter.turnOrder().currentTurn().orElse(null))) {
                        helper.fail("the spent mob turn must auto-end and pass to the player");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** The cancel rule keys on membership as of the previous tick, so the assertions wait a tick. */
    public static void vanillaDamageBetweenParticipantsIsCanceled(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    husk.invulnerableTime = 0;
                    float before = husk.getHealth();
                    husk.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0f);
                    if (husk.getHealth() != before) {
                        helper.fail("vanilla damage between participants must be canceled"
                                + " while the economy is active");
                    }
                    husk.hurt(helper.getLevel().damageSources().generic(), 4.0f);
                    if (husk.getHealth() >= before) {
                        helper.fail("damage without an attacking participant must still apply");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void actionsDisabledRestoresRealTimeWindows(GameTestHelper helper) {
        prepare(
                helper,
                new ActionConfig(
                        false, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true,
                        3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ActionEconomy.AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
        requireStatus(helper, attempt.status(), AttackStatus.INACTIVE, "an attack with the economy disabled");
        float before = husk.getHealth();
        husk.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0f);
        if (husk.getHealth() >= before) {
            helper.fail("disabling the economy must restore vanilla real-time damage");
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

    private static void requireStatus(GameTestHelper helper, AttackStatus actual, AttackStatus expected, String what) {
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
