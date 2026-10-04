package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.ActionOutcome;
import studio.modroll.initiative.action.NativeActions;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * M5a Minecraft-native actions: Shove (contested knockback) and improvised item actions (ender-pearl
 * blink, fishing-rod reel). Contests are scripted initiator-first (actor die, then target die).
 * Knockback is asserted on the target's horizontal delta-movement so no physics tick is required.
 */
public final class NativeActionScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0, 0,
            true, 8.0, true, 1.0, 0, 0);
    private static final ActionConfig SHOVE_OFF = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, true, false, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    private static final ActionConfig BLINK_OFF = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0, 0,
            false, 8.0, true, 1.0, 0, 0);
    private static final ActionConfig REEL_OFF = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0, 0,
            true, 8.0, false, 1.0, 0, 0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> HUSK_FIRST = List.of(1, 20);
    private static final double MOVED = 1.0e-3;
    private static final double STILL = 1.0e-6;

    private NativeActionScenarios() {}

    public static void shoveWinKnocksTargetBack(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1); // ~2 blocks: within the 3-block reach
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        husk.setDeltaMovement(0.0, 0.0, 0.0); // clear the knockback the encounter-forming hit imparted
        ScenarioSupport.forceD20Faces(15, 5); // actor wins the contest
        try {
            NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, husk);
            if (attempt.status() != ActionOutcome.PERFORMED || !attempt.knockedBack()) {
                helper.fail("a won shove must knock back but was " + attempt.status() + "/" + attempt.knockedBack());
            }
        } finally {
            RollService.resetRoller();
        }
        Vec3 delta = husk.getDeltaMovement();
        if (delta.horizontalDistanceSqr() <= MOVED) {
            helper.fail("a won shove must give the target horizontal knockback but delta was " + delta);
        }
        double away = delta.x * (husk.getX() - player.getX()) + delta.z * (husk.getZ() - player.getZ());
        if (away <= 0) {
            helper.fail("the knockback must push the target away from the shover but pointed toward it");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("a shove must consume the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void shoveLossDoesNothingButSpendsAction(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        husk.setDeltaMovement(0.0, 0.0, 0.0); // clear the knockback the encounter-forming hit imparted
        ScenarioSupport.forceD20Faces(5, 15); // actor loses the contest
        try {
            NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, husk);
            if (attempt.status() != ActionOutcome.PERFORMED || attempt.knockedBack()) {
                helper.fail(
                        "a lost shove must not knock back but was " + attempt.status() + "/" + attempt.knockedBack());
            }
        } finally {
            RollService.resetRoller();
        }
        if (husk.getDeltaMovement().horizontalDistanceSqr() >= STILL) {
            helper.fail("a lost shove must not move the target");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("a lost shove must still spend the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void shoveOutOfReachIsRejected(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 10, 1); // 9 blocks: beyond the 3-block reach
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        // No dice scripted: an out-of-reach shove must reject before any contest roll.
        NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, husk);
        if (attempt.status() != ActionOutcome.OUT_OF_REACH || attempt.knockedBack()) {
            helper.fail("an out-of-reach shove must be OUT_OF_REACH but was " + attempt.status());
        }
        if (!encounter.budget().actionAvailable()) {
            helper.fail("a rejected shove must not spend the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void shoveOutOfTurnIsRejected(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk); // husk leads: not the player's turn
        encounterOf(helper, player);
        husk.setDeltaMovement(0.0, 0.0, 0.0); // clear the knockback the encounter-forming hit imparted
        NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, husk);
        if (attempt.status() != ActionOutcome.NOT_YOUR_TURN) {
            helper.fail("an out-of-turn shove must be NOT_YOUR_TURN but was " + attempt.status());
        }
        if (husk.getDeltaMovement().horizontalDistanceSqr() >= STILL) {
            helper.fail("a rejected shove must not move the target");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void shoveOffIsRejected(GameTestHelper helper) {
        prepare(helper, SHOVE_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player);
        NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, husk);
        if (attempt.status() != ActionOutcome.DISABLED) {
            helper.fail("shove off must reject with DISABLED but was " + attempt.status());
        }
        // A disabled shove touches nothing, so an enabled action still goes through — proof it spent no action.
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionOutcome.PERFORMED) {
            helper.fail("a disabled shove must leave the turn's action intact");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void nativeActionWithoutAnActionIsRejected(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player);
        if (ActionEconomy.dash(helper.getLevel(), player) != ActionOutcome.PERFORMED) {
            helper.fail("the dash that spends the action must succeed");
        }
        NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, husk);
        if (attempt.status() != ActionOutcome.NO_ACTION) {
            helper.fail("a shove with no action left must be NO_ACTION but was " + attempt.status());
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void blinkConsumesPearlAndAction(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1); // spawnPlayer faces yaw 0 -> look +Z
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        player.getInventory().add(new ItemStack(Items.ENDER_PEARL));
        double startX = player.getX();
        double startY = player.getY();
        double startZ = player.getZ();
        ActionOutcome status = NativeActions.blink(helper.getLevel(), player);
        if (status != ActionOutcome.PERFORMED) {
            helper.fail("blink with a pearl must be PERFORMED but was " + status);
        }
        if (countPearls(player) != 0) {
            helper.fail("blink must consume exactly one ender pearl");
        }
        if (Math.abs(player.getZ() - (startZ + 8.0)) > 0.5 || Math.abs(player.getX() - startX) > 0.5) {
            helper.fail("blink must teleport ~8 blocks along facing (+Z) but landed at " + player.position());
        }
        if (Math.abs(player.getY() - startY) > 1.0e-6) {
            helper.fail("blink must preserve Y (horizontal teleport)");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("blink must consume the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void blinkWithoutPearlIsRejected(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player);
        double startZ = player.getZ();
        ActionOutcome status = NativeActions.blink(helper.getLevel(), player);
        if (status != ActionOutcome.MISSING_ITEM) {
            helper.fail("blink without a pearl must be MISSING_ITEM but was " + status);
        }
        if (Math.abs(player.getZ() - startZ) > 1.0e-6) {
            helper.fail("a rejected blink must not teleport");
        }
        // The action is untouched, so an enabled action still goes through.
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionOutcome.PERFORMED) {
            helper.fail("a rejected blink must leave the turn's action intact");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void blinkOffIsRejected(GameTestHelper helper) {
        prepare(helper, BLINK_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player);
        player.getInventory().add(new ItemStack(Items.ENDER_PEARL));
        ActionOutcome status = NativeActions.blink(helper.getLevel(), player);
        if (status != ActionOutcome.DISABLED) {
            helper.fail("blink off must be DISABLED but was " + status);
        }
        if (countPearls(player) != 1) {
            helper.fail("a disabled blink must not consume the pearl");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void reelWinPullsTargetTowardActor(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        husk.setDeltaMovement(0.0, 0.0, 0.0); // clear the knockback the encounter-forming hit imparted
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
        ScenarioSupport.forceD20Faces(15, 5); // actor wins the contest
        try {
            NativeActions.ReelAttempt attempt = NativeActions.reel(helper.getLevel(), player, husk);
            if (attempt.status() != ActionOutcome.PERFORMED || !attempt.pulled()) {
                helper.fail("a won reel must pull but was " + attempt.status() + "/" + attempt.pulled());
            }
        } finally {
            RollService.resetRoller();
        }
        Vec3 delta = husk.getDeltaMovement();
        if (delta.horizontalDistanceSqr() <= MOVED) {
            helper.fail("a won reel must give the target horizontal velocity but delta was " + delta);
        }
        double toward = delta.x * (player.getX() - husk.getX()) + delta.z * (player.getZ() - husk.getZ());
        if (toward <= 0) {
            helper.fail("the pull must move the target toward the actor but pointed away");
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() != 1) {
            helper.fail("reel must use one durability on the rod");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("reel must consume the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void reelLossDoesNotMoveButUsesRodAndAction(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        husk.setDeltaMovement(0.0, 0.0, 0.0); // clear the knockback the encounter-forming hit imparted
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
        ScenarioSupport.forceD20Faces(5, 15); // actor loses the contest
        try {
            NativeActions.ReelAttempt attempt = NativeActions.reel(helper.getLevel(), player, husk);
            if (attempt.status() != ActionOutcome.PERFORMED || attempt.pulled()) {
                helper.fail("a lost reel must not pull but was " + attempt.status() + "/" + attempt.pulled());
            }
        } finally {
            RollService.resetRoller();
        }
        if (husk.getDeltaMovement().horizontalDistanceSqr() >= STILL) {
            helper.fail("a lost reel must not move the target");
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() != 1) {
            helper.fail("a lost reel still uses the rod");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("a lost reel still spends the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void reelWithoutRodIsRejected(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player);
        // No rod in hand, no dice scripted: the reel must reject before any contest roll.
        NativeActions.ReelAttempt attempt = NativeActions.reel(helper.getLevel(), player, husk);
        if (attempt.status() != ActionOutcome.MISSING_ITEM || attempt.pulled()) {
            helper.fail("a reel without a rod must be MISSING_ITEM but was " + attempt.status());
        }
        // The action is untouched, so an enabled action still goes through.
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionOutcome.PERFORMED) {
            helper.fail("a rejected reel must leave the turn's action intact");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void reelOffIsRejected(GameTestHelper helper) {
        prepare(helper, REEL_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
        NativeActions.ReelAttempt attempt = NativeActions.reel(helper.getLevel(), player, husk);
        if (attempt.status() != ActionOutcome.DISABLED) {
            helper.fail("reel off must be DISABLED but was " + attempt.status());
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() != 0) {
            helper.fail("a disabled reel must not use the rod");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    private static int countPearls(Player player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(Items.ENDER_PEARL)) {
                count += player.getInventory().getItem(slot).getCount();
            }
        }
        return count;
    }

    private static void prepare(GameTestHelper helper, ActionConfig actions) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
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
