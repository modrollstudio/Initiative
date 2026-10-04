package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import studio.modroll.initiative.action.EncounterFlags;
import studio.modroll.initiative.action.Reactions;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.turn.AiFreeze;

/**
 * M4b reactions: opportunity attacks fire when a participant leaves a hostile's reach, resolve
 * through Critfall's {@code performAttack} for the reactor alone, and never touch the turn state
 * machine (current actor, round, order) or the actor's own budget.
 */
public final class ReactionScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    private static final ActionConfig OA_OFF = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, false, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    private static final ActionConfig HIDE_OFF = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, false, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    /** Hide contest with flat 0/0 bonuses so the scripted faces map straight to win/tie/loss. */
    private static final ActionConfig HIDE_CONTEST = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0, 0,
            true, 8.0, true, 1.0, 0, 0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> HUSK_FIRST = List.of(1, 20);
    private static final List<Integer> PLAYER_ALLY_HUSK = List.of(20, 1, 19);

    private ReactionScenarios() {}

    public static void movingOutOfReachProvokesAnOpportunityAttack(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1); // ~1 block away: in reach
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Vec3 from = player.position();
        Vec3 to = from.add(6, 0, 0); // moved well outside 3-block reach
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(15);
        try {
            Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, to);
            if (dice.d20Draws != 1) {
                helper.fail("the opportunity attack must roll one d20 but drew " + dice.d20Draws);
            }
            if (encounter.flags().hasReaction(husk.getUUID())) {
                helper.fail("the opportunity attack must consume the husk's reaction");
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void aReactionLeavesTheTurnStateUnchanged(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        UUID currentBefore = encounter.turnOrder().currentTurn().orElseThrow();
        int roundBefore = encounter.turnOrder().round();
        List<studio.modroll.initiative.turn.InitiativeEntry> orderBefore =
                encounter.turnOrder().order();
        Vec3 from = player.position();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(15);
        try {
            Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(6, 0, 0));
        } finally {
            RollService.resetRoller();
        }
        if (!currentBefore.equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the current actor must be unchanged after a reaction");
        }
        if (roundBefore != encounter.turnOrder().round()) {
            helper.fail("the round must be unchanged after a reaction");
        }
        if (!orderBefore.equals(encounter.turnOrder().order())) {
            helper.fail("the turn order must be unchanged after a reaction");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void disengageSuppressesTheOpportunityAttack(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        encounter.flags().setDisengaged(player.getUUID());
        Vec3 from = player.position();
        // No dice are scripted here: the real oracle is the assertion below — a disengaged mover
        // must provoke nothing, so the husk's reaction must stay intact.
        Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(6, 0, 0));
        if (!encounter.flags().hasReaction(husk.getUUID())) {
            helper.fail("a disengaged mover must not provoke, so the reaction must be intact");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void opportunityAttackOffProvokesNothing(GameTestHelper helper) {
        prepare(helper, OA_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Vec3 from = player.position();
        // No dice are scripted here: opportunity_attack is off, so leaving reach must draw nothing.
        Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(6, 0, 0));
        if (!encounter.flags().hasReaction(husk.getUUID())) {
            helper.fail("opportunity attacks off must leave the husk's reaction intact");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void oneReactionPerRoundThatRefreshesAtTheReactorsTurn(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Vec3 from = player.position();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(15);
        try {
            Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(6, 0, 0));
            // Second provocation the same round: no reaction left, so no roll — a scripted draw here would throw.
            Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(6, 0, 0));
            if (dice.d20Draws != 1) {
                helper.fail("only the first provocation may fire this round but drew " + dice.d20Draws);
            }
        } finally {
            RollService.resetRoller();
        }
        // Advance to the husk's own turn: its reaction refreshes.
        if (!ActionEconomy.endTurn(helper.getLevel(), player)) {
            helper.fail("the player must be able to end its turn");
        }
        encounter.budgetFor(encounter.turnOrder().round(), husk.getUUID(), ACTIONS.movementBudgetBlocks());
        if (!encounter.flags().hasReaction(husk.getUUID())) {
            helper.fail("the reaction must refresh at the reactor's own turn start");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void aFrozenReactorStaysFrozenThroughItsOpportunityAttack(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        AiFreeze.freeze(husk.getUUID()); // it is not the husk's turn, so it is frozen
        Vec3 from = player.position();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(15);
        try {
            Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(6, 0, 0));
        } finally {
            RollService.resetRoller();
        }
        if (dice.d20Draws != 1) {
            helper.fail("the frozen reactor must still take its opportunity attack");
        }
        if (!AiFreeze.isFrozen(husk.getUUID())) {
            helper.fail("the reaction must not thaw the frozen reactor");
        }
        AiFreeze.thaw(husk.getUUID());
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void opportunityAttackLandsFullDamageAfterOtherDamage(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        player.invulnerableTime = 20; // simulate i-frames from other damage this instant
        float before = player.getHealth();
        Vec3 from = player.position();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(20); // guaranteed hit
        try {
            Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(6, 0, 0));
        } finally {
            RollService.resetRoller();
        }
        if (player.getHealth() >= before) {
            helper.fail("the opportunity attack must bypass i-frames and deal damage; health was " + before
                    + " and is now " + player.getHealth());
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void hideGrantsAdvantageOnTheNextTurnsAttackThenBreaks(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        ScenarioSupport.forceD20Faces(20, 1); // Hide contest: hider nat 20 beats the observer's nat 1
        try {
            ActionEconomy.HideAttempt hidden = ActionEconomy.hide(helper.getLevel(), player);
            if (hidden.status() != ActionOutcome.PERFORMED || !hidden.hidden()) {
                helper.fail("a nat-20 Hide must succeed but was " + hidden.status() + "/" + hidden.hidden());
            }
        } finally {
            RollService.resetRoller();
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("Hide must consume the action");
        }
        if (!encounter.flags().isHidden(player.getUUID())) {
            helper.fail("a successful Hide must set the hidden flag");
        }
        // Hide spends the action, so the attack happens on a later turn: end the player's turn,
        // end the husk's turn, and let the player's next turn begin.
        if (!ActionEconomy.endTurn(helper.getLevel(), player)) {
            helper.fail("the player must be able to end its turn");
        }
        if (!ActionEconomy.endTurn(helper.getLevel(), husk)) {
            helper.fail("the husk must be able to end its turn to wrap the round");
        }
        ActionEconomy.tick(helper.getLevel(), encounter); // the player's next turn begins
        if (!encounter.flags().isHidden(player.getUUID())) {
            helper.fail("hidden must persist across the hider's own turn start, unlike dodge/disengage");
        }
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(5, 15);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
            ScenarioSupport.requireAttack(
                    helper, attempt.status(), AttackStatus.PERFORMED, "the hidden attacker's strike");
            AttackResult result = attempt.result().orElseThrow();
            if (dice.d20Draws != 2) {
                helper.fail("hidden must grant advantage (two d20s) but drew " + dice.d20Draws);
            }
            if (result.natural() != 15) {
                helper.fail("advantage must keep the higher die (15) but kept " + result.natural());
            }
            if (encounter.flags().isHidden(player.getUUID())) {
                helper.fail("attacking must break the hidden state");
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void hideTieLeavesYouSeen(GameTestHelper helper) {
        prepare(helper, HIDE_CONTEST);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        ScenarioSupport.forceD20Faces(10, 10); // equal totals: the tie goes to the observer
        try {
            ActionEconomy.HideAttempt attempt = ActionEconomy.hide(helper.getLevel(), player);
            if (attempt.status() != ActionOutcome.PERFORMED || attempt.hidden()) {
                helper.fail("a tied Hide contest must not hide but was " + attempt.status() + "/" + attempt.hidden());
            }
        } finally {
            RollService.resetRoller();
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("a failed Hide must still spend the action");
        }
        if (encounter.flags().isHidden(player.getUUID())) {
            helper.fail("a tied Hide must not set the hidden flag");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void hideLossLeavesYouSeen(GameTestHelper helper) {
        prepare(helper, HIDE_CONTEST);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        ScenarioSupport.forceD20Faces(5, 15); // hider loses the contest
        try {
            ActionEconomy.HideAttempt attempt = ActionEconomy.hide(helper.getLevel(), player);
            if (attempt.status() != ActionOutcome.PERFORMED || attempt.hidden()) {
                helper.fail("a lost Hide contest must not hide but was " + attempt.status() + "/" + attempt.hidden());
            }
        } finally {
            RollService.resetRoller();
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("a failed Hide must still spend the action");
        }
        if (encounter.flags().isHidden(player.getUUID())) {
            helper.fail("a lost Hide must not set the hidden flag");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void hiddenAttackerWithHelpBreaksHiddenToo(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player ally = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, PLAYER_ALLY_HUSK, player, ally, husk);
        Encounter encounter = encounterOf(helper, player);
        if (!player.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the hidden attacker must lead the round for this scenario");
        }
        encounter.flags().setHidden(player.getUUID());
        encounter.flags().grantHelpAdvantage(player.getUUID(), ally.getUUID());
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(5, 15);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
            ScenarioSupport.requireAttack(
                    helper, attempt.status(), AttackStatus.PERFORMED, "the hidden-and-helped attacker's strike");
            AttackResult result = attempt.result().orElseThrow();
            if (dice.d20Draws != 2) {
                helper.fail("advantage must roll two d20s but drew " + dice.d20Draws);
            }
            if (result.natural() != 15) {
                helper.fail("advantage must keep the higher die (15) but kept " + result.natural());
            }
            if (encounter.flags().hasHelpAdvantage(player.getUUID())) {
                helper.fail("the attack must consume the Help advantage");
            }
            if (encounter.flags().isHidden(player.getUUID())) {
                helper.fail("the attack must also break the hidden state");
            }
        } finally {
            RollService.resetRoller();
        }
        cleanUp(player, ally, husk);
        helper.succeed();
    }

    public static void hideOffIsRejected(GameTestHelper helper) {
        prepare(helper, HIDE_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        ActionEconomy.HideAttempt hidden = ActionEconomy.hide(helper.getLevel(), player);
        if (hidden.status() != ActionOutcome.DISABLED || hidden.hidden()) {
            helper.fail("a disabled Hide must reject with DISABLED and not hide but was " + hidden.status() + "/"
                    + hidden.hidden());
        }
        if (encounter.flags().isHidden(player.getUUID())) {
            helper.fail("a disabled Hide must not set the hidden flag");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void attackAgainstAHiddenParticipantRollsDisadvantage(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        if (!husk.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the husk must lead the round for this scenario");
        }
        encounter.flags().setHidden(player.getUUID());
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(15, 5);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), husk, player);
            ScenarioSupport.requireAttack(
                    helper, attempt.status(), AttackStatus.PERFORMED, "an attack on a hidden player");
            AttackResult result = attempt.result().orElseThrow();
            if (dice.d20Draws != 2) {
                helper.fail("an attack on a hidden participant must roll two d20s but drew " + dice.d20Draws);
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

    public static void hiddenBreaksWhenTheHiderIsHit(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        if (!husk.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the husk must lead the round for this scenario");
        }
        encounter.flags().setHidden(player.getUUID());
        // The target is hidden, so the mob's attack rolls with disadvantage — two d20 faces, both a
        // guaranteed hit — and the landed hit then breaks the hidden state.
        ScenarioSupport.forceD20Faces(20, 20);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), husk, player);
            ScenarioSupport.requireAttack(
                    helper, attempt.status(), AttackStatus.PERFORMED, "a mob attack on a hidden player");
            AttackResult result = attempt.result().orElseThrow();
            if (!result.isHit()) {
                helper.fail("a nat-20 attack must hit");
            }
        } finally {
            RollService.resetRoller();
        }
        if (encounter.flags().isHidden(player.getUUID())) {
            helper.fail("a landed hit must break the hider's hidden state");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void hiddenSurvivesWhenTheHiderIsMissed(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        if (!husk.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the husk must lead the round for this scenario");
        }
        encounter.flags().setHidden(player.getUUID());
        // Forces every die to its minimum, not a fixed d20 queue: a nat-1 miss can trigger Critfall's
        // own fumble-confirmation roll (an extra d20 draw), which forceD20Faces would reject as
        // "exhausted" once its single scripted face is spent.
        ScenarioSupport.forceCountedDice(false);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), husk, player);
            ScenarioSupport.requireAttack(
                    helper, attempt.status(), AttackStatus.PERFORMED, "a mob attack on a hidden player");
            AttackResult result = attempt.result().orElseThrow();
            if (result.natural() != 1) {
                helper.fail("forced-min dice must roll a natural 1 but rolled " + result.natural());
            }
            if (result.isHit()) {
                helper.fail("a nat-1 attack must miss");
            }
        } finally {
            RollService.resetRoller();
        }
        if (!encounter.flags().isHidden(player.getUUID())) {
            helper.fail("a missed attack must not break the hider's hidden state");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void participantRemovalClearsEveryFlag(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        EncounterFlags flags = encounter.flags();
        flags.setDodging(player.getUUID());
        flags.setDisengaged(player.getUUID());
        flags.grantHelpAdvantage(player.getUUID(), husk.getUUID());
        flags.setHidden(player.getUUID());
        flags.useReaction(player.getUUID());
        encounter.remove(player.getUUID());
        if (flags.isDodging(player.getUUID())
                || flags.isDisengaged(player.getUUID())
                || flags.hasHelpAdvantage(player.getUUID())
                || flags.isHidden(player.getUUID())
                || !flags.hasReaction(player.getUUID())) {
            helper.fail("removing a participant must clear all of its flags and restore its reaction");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    private static void prepare(GameTestHelper helper, ActionConfig actions) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
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

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
