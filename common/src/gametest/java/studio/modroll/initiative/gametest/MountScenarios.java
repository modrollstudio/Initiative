package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.MovementSpeed;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.gametest.RollAnimationScenarios.CapturingSender;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;
import studio.modroll.initiative.turn.AiFreeze;

/**
 * A skeleton riding a spider, five blocks from the player. The rider steers through its mount's
 * navigation, which ticks only while the mount is awake, so the pair can only move while both are.
 * Every die after the initiative rolls is a natural 1, so the attacks miss and nobody dies.
 */
public final class MountScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, false, 1.0, 20.0, true);
    private static final TurnConfig TURNS =
            new TurnConfig(true, 1000, 60, 20.0, 12, true, false, false, Set.of(), true, true);
    private static final TurnConfig SEPARATE_TURNS =
            new TurnConfig(true, 1000, 60, 20.0, 12, true, false, false, Set.of(), true, false);
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, true, true, 4.0, false, true, true, true, true, true, true, false, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0, true, 24.0, 64.0, 20);
    /** The player first, then the skeleton, then the spider, whose roll beats the skeleton's. */
    private static final List<Integer> INITIATIVE = List.of(20, 5, 10);

    private MountScenarios() {}

    public static void riderAndMountShareOneTurn(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 6.5, 3.5);
        Spider spider = helper.spawn(EntityType.SPIDER, new BlockPos(1, 1, 3));
        Skeleton skeleton = rider(helper, spider, player);
        ScenarioSupport.formEncounter(helper, player, INITIATIVE, skeleton, spider);
        Encounter encounter = encounter(helper, player);
        CapturingSender rolls = new CapturingSender();
        boolean[] mountActedAlone = new boolean[1];
        boolean[] frozenOnRidersTurn = new boolean[1];
        double[] mountMovementLeft = {Double.MAX_VALUE};
        helper.onEachTick(() -> {
            UUID current = encounter.turnOrder().currentTurn().orElse(null);
            mountActedAlone[0] |= spider.getUUID().equals(current);
            if (skeleton.getUUID().equals(current)) {
                frozenOnRidersTurn[0] |= AiFreeze.isFrozen(spider.getUUID()) || AiFreeze.isFrozen(skeleton.getUUID());
            }
            if (skeleton.getUUID().equals(current) && encounter.turnOrder().isActing(spider.getUUID())) {
                mountMovementLeft[0] = Math.min(
                        mountMovementLeft[0],
                        ActionEconomy.budgetOf(encounter, spider).movementRemaining());
            }
        });
        Vec3[] start = new Vec3[1];
        helper.startSequence()
                .thenWaitUntil(() -> requireSettled(helper, spider))
                .thenExecute(() -> {
                    start[0] = spider.position();
                    endThePlayersTurn(helper, encounter, player, rolls);
                })
                .thenWaitUntil(() -> requireRound(helper, encounter, 2))
                .thenExecute(() -> {
                    double moved = horizontal(start[0], spider.position());
                    if (moved < 2.0) {
                        helper.fail("the pair must move on its shared turn but moved " + moved + " blocks");
                    }
                    if (frozenOnRidersTurn[0]) {
                        helper.fail("the rider and its mount must both be awake on the rider's turn");
                    }
                    if (mountMovementLeft[0] > MovementSpeed.fullBudget(spider) - 2.0) {
                        helper.fail("the pair's walk must be spent from the mount's movement, " + mountMovementLeft[0]
                                + " left");
                    }
                    requireAttacked(helper, rolls, player, skeleton);
                    requireAttacked(helper, rolls, player, spider);
                    if (mountActedAlone[0]) {
                        helper.fail("the mount's own slot must be passed over");
                    }
                    requireCurrent(helper, encounter, player, "the turn after the pair's");
                    cleanUp(player, skeleton, spider);
                })
                .thenSucceed();
    }

    public static void withTheToggleOffTheMountTakesItsOwnTurn(GameTestHelper helper) {
        prepare(helper, SEPARATE_TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 6.5, 3.5);
        Spider spider = helper.spawn(EntityType.SPIDER, new BlockPos(1, 1, 3));
        Skeleton skeleton = rider(helper, spider, player);
        ScenarioSupport.formEncounter(helper, player, INITIATIVE, skeleton, spider);
        Encounter encounter = encounter(helper, player);
        boolean[] mountActedAlone = new boolean[1];
        helper.onEachTick(() -> mountActedAlone[0] |=
                spider.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null)));
        helper.startSequence()
                .thenWaitUntil(() -> requireSettled(helper, spider))
                .thenExecute(() -> endThePlayersTurn(helper, encounter, player, new CapturingSender()))
                .thenWaitUntil(() -> requireRound(helper, encounter, 2))
                .thenExecute(() -> {
                    if (!mountActedAlone[0]) {
                        helper.fail("with mounts_act_with_riders off the mount must take its own turn");
                    }
                    cleanUp(player, skeleton, spider);
                })
                .thenSucceed();
    }

    private static void endThePlayersTurn(
            GameTestHelper helper, Encounter encounter, Player player, CapturingSender rolls) {
        requireCurrent(helper, encounter, player, "the player's natural 20");
        RollAnimationSync.overrideSenderForTesting(rolls);
        ScenarioSupport.forceCountedDice(false);
        encounter.turnOrder().endTurn();
    }

    private static void requireRound(GameTestHelper helper, Encounter encounter, int round) {
        if (encounter.turnOrder().round() < round) {
            helper.fail("waiting for round " + round);
        }
    }

    /**
     * A skeleton with no bow, so its own melee goal walks it at the player through its mount's
     * navigation, and a helmet, so the sun can't burn it.
     */
    private static Skeleton rider(GameTestHelper helper, Spider spider, LivingEntity target) {
        Skeleton skeleton = helper.spawn(EntityType.SKELETON, new BlockPos(1, 1, 3));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        skeleton.startRiding(spider, true);
        skeleton.setTarget(target);
        return skeleton;
    }

    private static void requireAttacked(
            GameTestHelper helper, CapturingSender rolls, Player watcher, LivingEntity attacker) {
        String name = attacker.getDisplayName().getString();
        boolean attacked = rolls.sent.stream()
                .filter(sent -> sent.player().equals(watcher.getUUID()))
                .map(CapturingSender.Sent::animation)
                .anyMatch(animation -> animation.kind() == RollAnimation.Kind.ATTACK
                        && animation.actor().roller().equals(name));
        if (!attacked) {
            helper.fail(name + " must attack on the shared turn");
        }
    }

    private static void requireSettled(GameTestHelper helper, Mob mob) {
        if (!mob.onGround() || mob.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
            helper.fail("waiting for the knockback of the forming hits to settle");
        }
    }

    private static void requireCurrent(GameTestHelper helper, Encounter encounter, LivingEntity expected, String what) {
        if (!expected.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail(what + ": the turn must be " + expected.getDisplayName().getString() + "'s");
        }
    }

    private static double horizontal(Vec3 from, Vec3 to) {
        return Math.hypot(to.x - from.x, to.z - from.z);
    }

    private static Encounter encounter(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, TurnConfig turns) {
        RollService.resetRoller();
        RollAnimationSync.clearSenderOverrideForTesting();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(turns);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideRollAnimationForTesting(ScenarioSupport.ANIMATION_ON);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
    }

    private static void cleanUp(LivingEntity... entities) {
        RollService.resetRoller();
        RollAnimationSync.clearSenderOverrideForTesting();
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
