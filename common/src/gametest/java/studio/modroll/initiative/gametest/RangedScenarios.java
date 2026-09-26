package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.turn.AiFreeze;

/**
 * Ranged attackers. The shooter stands eight blocks out — past the trigger radius, and in the
 * suites below past the leave radius too — so nothing but its own arrow can put it in the fight or
 * keep it there. Turns and freeze are on: the point of pulling it in is that its next shot waits
 * for its turn.
 */
public final class RangedScenarios {

    private static final EncounterConfig OUT_OF_REACH =
            new EncounterConfig(true, true, true, true, true, true, 4.0, 5.0);
    private static final EncounterConfig HOLD_OFF = new EncounterConfig(true, true, true, true, false, true, 4.0, 5.0);
    private static final EncounterConfig TWO_BUBBLES =
            new EncounterConfig(true, true, true, true, true, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, true, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    /** Drawn in join order (player, then the husk), so (20, 1) puts the player's turn first. */
    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);

    /** The shooter joins after both, so its own natural has to leave the player's turn alone. */
    private static final List<Integer> SHOOTER_LAST = List.of(1);

    private static final int SETTLE_TICKS = 3;

    private RangedScenarios() {}

    /**
     * The bug this exists for: distance used to be a hiding place. The arrow lands in real time,
     * the shooter is in the fight from the tick after, and its next arrow is the turn order's to
     * hand out — even though it never came anywhere near the bubble.
     */
    public static void aRangedHitFromOutsideBindsTheShooterToTheTurnOrder(GameTestHelper helper) {
        prepare(helper, OUT_OF_REACH);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Skeleton shooter = spawnShooter(helper, player);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter running = encounterOf(helper, player);
        float[] health = new float[1];
        shoot(helper, shooter, player);
        if (!running.contains(shooter.getUUID())) {
            helper.fail("an arrow that reached a participant must pull its shooter into the fight");
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    if (!encounterOf(helper, shooter).contains(player.getUUID())) {
                        helper.fail("the shooter must still be in the fight it shot into");
                    }
                    if (!RollService.isSuppressed(shooter)) {
                        helper.fail("the shooter must carry the suppression flag like any participant");
                    }
                    if (!AiFreeze.isFrozen(shooter.getUUID())) {
                        helper.fail("the shooter must be frozen off-turn like any participant");
                    }
                    health[0] = player.getHealth();
                    shootWithoutJoining(helper, shooter, player);
                })
                .thenExecuteAfter(1, () -> {
                    if (player.getHealth() < health[0]) {
                        helper.fail("a participant's arrow must not land in real time off its turn");
                    }
                    cleanUp(player, husk, shooter);
                })
                .thenSucceed();
    }

    /** The one-encounter guard: the shooter joins the fight it shot into, never a second one. */
    public static void theShooterJoinsTheFightItShotInto(GameTestHelper helper) {
        prepare(helper, OUT_OF_REACH);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Skeleton shooter = spawnShooter(helper, player);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter running = encounterOf(helper, player);
        shoot(helper, shooter, player);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    if (encounterOf(helper, shooter) != running) {
                        helper.fail("the shooter must join the running fight, not open a second one");
                    }
                    requireEncounterCount(helper, 1);
                    cleanUp(player, husk, shooter);
                })
                .thenSucceed();
    }

    /** A shooter already fighting elsewhere keeps that fight, and neither one is corrupted. */
    public static void aShooterInAnotherFightStaysThere(GameTestHelper helper) {
        prepare(helper, TWO_BUBBLES);
        Player sniped = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Player busy = ScenarioSupport.spawnPlayer(helper, 1, 9);
        Skeleton shooter = spawnShooter(helper, sniped);
        ScenarioSupport.formEncounter(helper, sniped, PLAYER_FIRST, husk);
        ScenarioSupport.formEncounter(helper, busy, PLAYER_FIRST, shooter);
        Encounter here = encounterOf(helper, sniped);
        Encounter there = encounterOf(helper, busy);
        shootWithoutJoining(helper, shooter, sniped);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    if (encounterOf(helper, shooter) != there) {
                        helper.fail("a shooter already in a fight must not be dragged into another");
                    }
                    if (here.contains(shooter.getUUID()) || there.contains(sniped.getUUID())) {
                        helper.fail("neither fight may end up holding the other's participants");
                    }
                    requireEncounterCount(helper, 2);
                    cleanUp(sniped, husk, busy, shooter);
                })
                .thenSucceed();
    }

    /** A dispenser's arrow has no combatant behind it, and a dead shooter is no longer one. */
    public static void anArrowWithNoLivingShooterAddsNothing(GameTestHelper helper) {
        prepare(helper, OUT_OF_REACH);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Skeleton shooter = spawnShooter(helper, player);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter running = encounterOf(helper, player);
        hurtWithArrow(helper, player, arrow(helper, shooter), null);
        if (running.contains(shooter.getUUID())) {
            helper.fail("an ownerless arrow must not pull anybody in");
        }
        shooter.kill();
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    hurtWithArrow(helper, player, arrow(helper, shooter), shooter);
                    if (running.contains(shooter.getUUID())) {
                        helper.fail("a dead shooter must not become a participant");
                    }
                    requireEncounterCount(helper, 1);
                    cleanUp(player, husk, shooter);
                })
                .thenSucceed();
    }

    /** PvP is still out: an arrow between players forms no hostile and pulls nobody in. */
    public static void aPlayersArrowIntoAPlayerFormsNoHostile(GameTestHelper helper) {
        prepare(helper, OUT_OF_REACH);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Player rival = ScenarioSupport.spawnPlayer(helper, 2, 9);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter running = encounterOf(helper, player);
        hurtWithArrow(helper, player, arrow(helper, rival), rival);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    if (running.contains(rival.getUUID())) {
                        helper.fail("a player's arrow must not make the shooter a participant");
                    }
                    if (EncounterManager.encounterContaining(helper.getLevel(), rival.getUUID())
                            .isPresent()) {
                        helper.fail("PvP must not put the shooting player in any fight");
                    }
                    requireEncounterCount(helper, 1);
                    cleanUp(player, husk, rival);
                })
                .thenSucceed();
    }

    /** A shooter that drops its sights has stopped fighting, and the leave radius takes it back. */
    public static void aShooterThatLosesItsTargetLeaves(GameTestHelper helper) {
        prepare(helper, OUT_OF_REACH);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Skeleton shooter = spawnShooter(helper, player);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        shoot(helper, shooter, player);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    encounterOf(helper, shooter);
                    shooter.setTarget(null);
                })
                .thenExecuteAfter(2, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), shooter.getUUID())
                            .isPresent()) {
                        helper.fail("a shooter with nobody in its sights must fall back out of the fight");
                    }
                    if (RollService.isSuppressed(shooter) || AiFreeze.isFrozen(shooter.getUUID())) {
                        helper.fail("leaving must release the suppression flag and thaw the AI");
                    }
                    cleanUp(player, husk, shooter);
                })
                .thenSucceed();
    }

    /** Flag off: the shot still pulls the shooter in, and the leave radius drops it as it always did. */
    public static void disabledFlagLeavesTheShooterToTheLeaveRadius(GameTestHelper helper) {
        prepare(helper, HOLD_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Skeleton shooter = spawnShooter(helper, player);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        shoot(helper, shooter, player);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), shooter.getUUID())
                            .isPresent()) {
                        helper.fail("with the flag off the leave radius must drop the shooter again");
                    }
                    if (RollService.isSuppressed(shooter) || AiFreeze.isFrozen(shooter.getUUID())) {
                        helper.fail("nothing of the shooter may outlive the flag being off");
                    }
                    cleanUp(player, husk, shooter);
                })
                .thenSucceed();
    }

    /**
     * The shot lands as plain vanilla damage, exactly like the melee hit that forms an encounter:
     * the shooter joins mid-hurt, so the scripted natural here is its initiative roll.
     */
    private static void shoot(GameTestHelper helper, Skeleton shooter, Player target) {
        ScenarioSupport.forceNaturals(helper, SHOOTER_LAST, () -> shootWithoutJoining(helper, shooter, target));
    }

    /** For the shots that join nobody, and so roll no initiative to script. */
    private static void shootWithoutJoining(GameTestHelper helper, Skeleton shooter, Player target) {
        hurtWithArrow(helper, target, arrow(helper, shooter), shooter);
    }

    private static void hurtWithArrow(GameTestHelper helper, Player target, AbstractArrow arrow, LivingEntity shooter) {
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().arrow(arrow, shooter), 1.0f);
        arrow.discard();
    }

    private static AbstractArrow arrow(GameTestHelper helper, LivingEntity shooter) {
        Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        arrow.setPos(shooter.getX(), shooter.getY(), shooter.getZ());
        helper.getLevel().addFreshEntity(arrow);
        return arrow;
    }

    /** No AI, so only the scenario moves it or changes what it is shooting at. */
    private static Skeleton spawnShooter(GameTestHelper helper, Player target) {
        Skeleton shooter = helper.spawn(EntityType.SKELETON, new BlockPos(2, 1, 10));
        shooter.setNoAi(true);
        shooter.setTarget(target);
        return shooter;
    }

    private static Encounter encounterOf(GameTestHelper helper, LivingEntity entity) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), entity.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail(entity.getName().getString() + " must be in an encounter for the scenario to continue");
        }
        return encounter;
    }

    private static void requireEncounterCount(GameTestHelper helper, int expected) {
        List<Encounter> encounters = EncounterManager.encounters(helper.getLevel());
        if (encounters.size() != expected) {
            helper.fail("the level must hold " + expected + " encounter(s) but held " + encounters.size());
        }
    }

    private static void prepare(GameTestHelper helper, EncounterConfig encounters) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(encounters);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
