package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.action.TurnBudget;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ChecksConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * A player's Attack action makes as many attacks as their Checks class gives at their level. Every
 * swing goes in through the attack hook the loaders call, and every d20 is a natural 1, so they
 * miss. A run without Checks has no classes: there every player attacks once, and the scenarios that
 * need a class pass untested.
 */
public final class ExtraAttackScenarios {

    private static final boolean WITHOUT_CHECKS = Boolean.getBoolean("initiative.gametest.withoutChecks");
    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, false, 1.0, 30.0, true);
    private static final TurnConfig TURNS =
            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of(), true, true);
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, true, true, 4.0, false, true, true, true, true, true, true, false, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0, true, 24.0, 64.0, 20);
    private static final ChecksConfig CHECKS_ON = new ChecksConfig(true, true, true);
    private static final ChecksConfig EXTRA_ATTACK_OFF = new ChecksConfig(true, true, false);
    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);

    private ExtraAttackScenarios() {}

    public static void aLevelFiveFighterAttacksTwice(GameTestHelper helper) {
        if (!prepareWithChecks(helper, CHECKS_ON)) {
            return;
        }
        ServerPlayer player = player(helper);
        ChecksCharacters.make(player, "fighter", 5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        requireAttacks(helper, player, husk, 2, "a level 5 Fighter");
        finish(helper, player, husk);
        helper.succeed();
    }

    public static void aLevelFourFighterAndALevelFiveWizardAttackOnce(GameTestHelper helper) {
        if (!prepareWithChecks(helper, CHECKS_ON)) {
            return;
        }
        ServerPlayer player = player(helper);
        ChecksCharacters.make(player, "fighter", 4);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        requireAttacks(helper, player, husk, 1, "a level 4 Fighter");
        ChecksCharacters.make(player, "wizard", 5);
        nextTurnOf(helper, player);
        requireAttacks(helper, player, husk, 1, "a level 5 Wizard");
        finish(helper, player, husk);
        helper.succeed();
    }

    public static void extraAttackOffLeavesOneAttack(GameTestHelper helper) {
        if (!prepareWithChecks(helper, EXTRA_ATTACK_OFF)) {
            return;
        }
        ServerPlayer player = player(helper);
        ChecksCharacters.make(player, "fighter", 5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        requireAttacks(helper, player, husk, 1, "a level 5 Fighter with extra_attack off");
        finish(helper, player, husk);
        helper.succeed();
    }

    /** A swing spends the action and opens the second attack, which an arrow then takes. */
    public static void aShotCanBeTheSecondAttack(GameTestHelper helper) {
        if (!prepareWithChecks(helper, CHECKS_ON)) {
            return;
        }
        ServerPlayer player = player(helper);
        ChecksCharacters.make(player, "fighter", 5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        player.getInventory().add(new ItemStack(Items.ARROW, 4));
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        ScenarioSupport.forceCountedDice(false);
        try {
            AttackStatus swing =
                    ActionEconomy.attack(helper.getLevel(), player, husk).status();
            ScenarioSupport.requireAttack(helper, swing, AttackStatus.PERFORMED, "the Fighter's first attack");
            player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
            if (!player.isUsingItem()) {
                helper.fail("a Fighter with an attack left must be allowed to draw");
            }
            player.getUseItem().releaseUsing(helper.getLevel(), player, 0);
            player.stopUsingItem();
            TurnBudget budget = ActionEconomy.budgetOf(encounter, player);
            if (budget.attackAvailable()) {
                helper.fail("the arrow must take the Fighter's second attack, " + budget.attacksLeft() + " left");
            }
        } finally {
            RollService.resetRoller();
        }
        finish(helper, player, husk);
        helper.succeed();
    }

    /** Runs with and without Checks: a player who made no character has no class to give a second attack. */
    public static void aPlayerWithNoClassAttacksOnce(GameTestHelper helper) {
        prepare(helper, CHECKS_ON);
        ServerPlayer player = player(helper);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        requireAttacks(helper, player, husk, 1, "a player with no class");
        finish(helper, player, husk);
        helper.succeed();
    }

    private static void requireAttacks(
            GameTestHelper helper, ServerPlayer player, LivingEntity target, int attacks, String who) {
        ScenarioSupport.forceCountedDice(false);
        try {
            for (int attack = 1; attack <= attacks; attack++) {
                AttackStatus status =
                        ActionEconomy.attack(helper.getLevel(), player, target).status();
                ScenarioSupport.requireAttack(helper, status, AttackStatus.PERFORMED, who + "'s attack " + attack);
                if (ActionEconomy.budgetOf(encounter(helper, player), player).actionAvailable()) {
                    helper.fail(who + "'s attack must spend the action");
                }
            }
            AttackStatus extra =
                    ActionEconomy.attack(helper.getLevel(), player, target).status();
            ScenarioSupport.requireAttack(helper, extra, AttackStatus.NO_ACTION, who + "'s attack " + (attacks + 1));
        } finally {
            RollService.resetRoller();
        }
    }

    /** Passes the husk's turn by hand, so the player's next turn starts with a fresh action. */
    private static void nextTurnOf(GameTestHelper helper, ServerPlayer player) {
        Encounter encounter = encounter(helper, player);
        encounter.turnOrder().endTurn();
        encounter.turnOrder().endTurn();
        if (!player.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the turn must come back to the player");
        }
    }

    /** In the husk's reach, as the mob turn scenarios stand them. */
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 spawn = helper.absoluteVec(new Vec3(2.0, 1, 2.5));
        player.moveTo(spawn.x, spawn.y, spawn.z, 0, 0);
        return player;
    }

    private static Encounter encounter(GameTestHelper helper, ServerPlayer player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, ChecksConfig checks) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideChecksForTesting(checks);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
        if (WITHOUT_CHECKS && ChecksIntegration.present()) {
            helper.fail("a run started with -PwithoutChecks must not have Checks installed");
        }
    }

    /** In a run without Checks this passes the scenario untested and answers false. */
    private static boolean prepareWithChecks(GameTestHelper helper, ChecksConfig checks) {
        prepare(helper, checks);
        if (WITHOUT_CHECKS) {
            helper.succeed();
            return false;
        }
        if (!ChecksIntegration.present()) {
            helper.fail("the GameTest runs must have Checks installed, or be started with -PwithoutChecks");
        }
        return true;
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, LivingEntity... others) {
        RollService.resetRoller();
        helper.getLevel().getServer().getPlayerList().remove(player);
        for (LivingEntity entity : others) {
            entity.discard();
        }
    }
}
