package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * Teleports during an encounter. The teleporter is a mock server player, because a teleport only
 * reaches the gate through a real connection — the call that moves a player and tells its client.
 * The pearls are real ender pearls dropped onto the plot floor, landing where they were dropped.
 * The far pearl lands past the leave radius, so one that got through would carry the player out of
 * the fight exactly as it did in the playtest.
 */
public final class TeleportScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, true, 1.0, 8.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, true, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 10.0, false, true, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0);
    private static final ActionConfig TELEPORTS_FREE = new ActionConfig(
            true, 10.0, false, false, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> HUSK_FIRST = List.of(1, 20);

    private static final double BUDGET = 10.0;
    private static final double NEAR = 4.0;
    private static final double FAR = 15.0;
    private static final double PEARL_DROP_HEIGHT = 3.0;
    private static final int LANDING_TICKS = 10;
    private static final double TOLERANCE = 0.05;

    private TeleportScenarios() {}

    /** The bug this exists for: a pearl thrown on your turn carried you out of the fight. */
    public static void aPearlPastYourMovementIsRefused(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        ServerPlayer player = spawnServerPlayer(helper);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Vec3 start = player.position();
        dropPearl(helper, player, FAR);
        helper.startSequence()
                .thenExecuteAfter(LANDING_TICKS, () -> {
                    requireHorizontallyAt(helper, player, start, "a pearl past the movement left must not move you");
                    Encounter encounter = encounterOf(helper, player);
                    requireMovementLeft(helper, encounter, player, BUDGET, "a refused pearl must cost nothing");
                    cleanUp(helper, player, husk);
                })
                .thenSucceed();
    }

    /** Inside the movement left a pearl is movement, charged by the distance it covered. */
    public static void aPearlInsideYourMovementCostsTheDistance(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        ServerPlayer player = spawnServerPlayer(helper);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Vec3 landing = player.position().add(0, 0, NEAR);
        dropPearl(helper, player, NEAR);
        helper.startSequence()
                .thenExecuteAfter(LANDING_TICKS, () -> {
                    requireHorizontallyAt(helper, player, landing, "a pearl inside the movement left must land you");
                    Encounter encounter = encounterOf(helper, player);
                    requireMovementLeft(
                            helper, encounter, player, BUDGET - NEAR, "the pearl must cost the distance it covered");
                    cleanUp(helper, player, husk);
                })
                .thenSucceed();
    }

    /** Off turn a teleport is refused however short; this is the call a chorus fruit makes. */
    public static void aTeleportOffYourTurnIsRefused(GameTestHelper helper) {
        prepare(helper, ACTIONS);
        ServerPlayer player = spawnServerPlayer(helper);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    Vec3 start = player.position();
                    player.teleportTo(start.x, start.y, start.z + 1.0);
                    requireHorizontallyAt(helper, player, start, "a teleport off your turn must be refused");
                    cleanUp(helper, player, husk);
                })
                .thenSucceed();
    }

    /** Off restores the old gate exactly: on your turn nothing stops the teleport itself. */
    public static void theToggleOffLetsATeleportThrough(GameTestHelper helper) {
        prepare(helper, TELEPORTS_FREE);
        ServerPlayer player = spawnServerPlayer(helper);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    Vec3 landing = player.position().add(0, 0, FAR);
                    player.teleportTo(landing.x, landing.y, landing.z);
                    requireHorizontallyAt(helper, player, landing, "with the toggle off a teleport must go through");
                    cleanUp(helper, player, husk);
                })
                .thenSucceed();
    }

    /** Dropped straight down onto the floor {@code distance} blocks along +z, so it lands right there. */
    private static void dropPearl(GameTestHelper helper, ServerPlayer owner, double distance) {
        ThrownEnderpearl pearl = new ThrownEnderpearl(helper.getLevel(), owner);
        pearl.setPos(owner.getX(), owner.getY() + PEARL_DROP_HEIGHT, owner.getZ() + distance);
        pearl.setDeltaMovement(0.0, -1.0, 0.0);
        helper.getLevel().addFreshEntity(pearl);
    }

    private static ServerPlayer spawnServerPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1, 1, 1));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }

    private static void requireHorizontallyAt(GameTestHelper helper, ServerPlayer player, Vec3 expected, String what) {
        double off = Math.hypot(player.getX() - expected.x, player.getZ() - expected.z);
        if (off > TOLERANCE) {
            helper.fail(what + ": expected " + expected + " but was " + player.position());
        }
    }

    private static void requireMovementLeft(
            GameTestHelper helper, Encounter encounter, LivingEntity player, double expected, String what) {
        double left = ActionEconomy.budgetOf(encounter, player).movementRemaining();
        if (Math.abs(left - expected) > TOLERANCE) {
            helper.fail(what + ": expected " + expected + " movement left but had " + left);
        }
    }

    private static Encounter encounterOf(GameTestHelper helper, LivingEntity entity) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), entity.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the player must still be in the encounter");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, ActionConfig actions) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
    }

    private static void cleanUp(GameTestHelper helper, ServerPlayer player, LivingEntity... others) {
        helper.getLevel().getServer().getPlayerList().remove(player);
        for (LivingEntity other : others) {
            other.discard();
        }
    }
}
