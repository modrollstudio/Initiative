package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.action.BlockPlacement;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.CoverConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.turn.OffTurnRestriction;
import studio.modroll.initiative.turn.TurnOrder;

/**
 * The off-turn player hold. A player is not an AI to freeze, so the hold is observed the way a
 * player would feel it: the movement-speed attribute the client obeys, the pull-back that undoes a
 * step taken anyway, and the refusal every world-interaction hook shares. The player always rolls a
 * natural 20 and hands the turn to the husk, so the held participant is a human off its turn.
 */
public final class OffTurnScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 8.0);
    private static final TurnConfig RESTRICT_ON =
            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, true, Set.of());
    private static final TurnConfig RESTRICT_OFF =
            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    /**
     * A movement budget far larger than any step taken here: these scenarios walk the acting player
     * repeatedly, and an exhausted budget would revert it for reasons that have nothing to do with
     * the hold under test.
     */
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 100.0, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    private static final CoverConfig PLACEMENT_COSTS_MOVEMENT =
            new CoverConfig(false, 2, 5, 0.5, 0.75, true, true, 1.0);

    /** Along x only: drifting diagonally would walk the player into the husk's reach and end its turn. */
    private static final double DRIFT = 3.0;

    /**
     * A pool one block wide at the plot's spawn column. A water source stays a source whatever is
     * under it, and the ring keeps it from spreading, so the fluid the scenarios need is exactly the
     * block the player stands in — the framework does not clear a plot between tests, and water that
     * flowed would outlive the scenario that placed it.
     */
    private static final BlockPos POOL = new BlockPos(1, 2, 1);

    private static final List<BlockPos> POOL_RIM =
            List.of(new BlockPos(0, 2, 1), new BlockPos(2, 2, 1), new BlockPos(1, 2, 0), new BlockPos(1, 2, 2));

    /** Feet in the middle of the pool block, so the sink below stays inside the same fluid block. */
    private static final double POOL_FEET_Y = 2.5;

    private static final double SINK = 0.4;

    private OffTurnScenarios() {}

    public static void anOffTurnPlayerIsRootedAndPulledBack(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Vec3[] anchor = new Vec3[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    anchor[0] = player.position();
                    player.setPos(anchor[0].x + DRIFT, anchor[0].y, anchor[0].z);
                })
                .thenExecuteAfter(2, () -> {
                    requireNear(helper, player, anchor[0], "a held player that walked anyway");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** In a fluid the free vertical of the pull-back is a slow sink, so the anchor holds the height too. */
    public static void aHeldPlayerDoesNotSinkInAFluid(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        fillPool(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1.5, 1.5);
        standInThePool(helper, player);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Vec3[] anchor = new Vec3[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    anchor[0] = player.position();
                    player.setPos(anchor[0].x, anchor[0].y - SINK, anchor[0].z);
                })
                .thenExecuteAfter(2, () -> {
                    requireHeight(helper, player, anchor[0].y, "a held player sinking in water");
                    cleanUp(player, husk);
                    drainPool(helper);
                })
                .thenSucceed();
    }

    /** The same drop out of a fluid is kept, because gravity is what the free vertical is for. */
    public static void aHeldPlayerOutOfAFluidStillFalls(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1.5, 1.5);
        standInThePool(helper, player);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Vec3[] anchor = new Vec3[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    anchor[0] = player.position();
                    player.setPos(anchor[0].x, anchor[0].y - SINK, anchor[0].z);
                })
                .thenExecuteAfter(2, () -> {
                    requireNoHigherThan(helper, player, anchor[0].y - SINK, "a held player falling in open air");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void theActingPlayerMovesFreely(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Vec3[] origin = new Vec3[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireCurrent(helper, player, player);
                    requireFree(helper, player, "the acting player");
                    origin[0] = player.position();
                    player.setPos(origin[0].x + DRIFT, origin[0].y, origin[0].z);
                })
                .thenExecuteAfter(2, () -> {
                    requireAwayFrom(helper, player, origin[0], "a step inside the acting player's budget");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** The one predicate every loader's break/place/use/interact hook asks before it cancels. */
    public static void offTurnWorldInteractionIsRefused(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        InitiativeConfig.overrideCoverForTesting(PLACEMENT_COSTS_MOVEMENT);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player bystander = ScenarioSupport.spawnPlayer(helper, 2, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireCurrent(helper, player, player);
                    requireInteraction(helper, player, true, "the acting player");
                    if (!BlockPlacement.onPlaceAttempt(helper.getLevel(), player)) {
                        helper.fail("placing on your own turn with movement left must still be allowed");
                    }
                    passTurnToTheHusk(helper, player, husk);
                })
                .thenExecuteAfter(2, () -> {
                    requireInteraction(helper, player, false, "a participant player off its turn");
                    requireInteraction(helper, bystander, true, "a player who is in no encounter");
                    cleanUp(player, bystander, husk);
                })
                .thenSucceed();
    }

    public static void theHoldReleasesWhenTheTurnArrives(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    turnOrder(helper, player).endTurn();
                })
                .thenExecuteAfter(2, () -> {
                    requireCurrent(helper, player, player);
                    requireReleased(helper, player, "the turn coming back around");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void deathReleasesTheHold(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    player.kill();
                })
                .thenExecuteAfter(2, () -> {
                    requireReleased(helper, player, "death");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** Leaving is checked before the hold is reapplied, so a player dragged out is let go, not pulled back. */
    public static void leavingTheBubbleReleasesTheHold(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Vec3 outside = helper.absoluteVec(new Vec3(1, 1, 14));
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    player.setPos(outside.x, outside.y, outside.z);
                })
                .thenExecuteAfter(2, () -> {
                    requireReleased(helper, player, "leaving the encounter bubble");
                    requireNear(helper, player, outside, "a player who left the bubble");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void aDisconnectReleasesTheHold(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    EncounterManager.playerDisconnected(helper.getLevel(), player.getUUID());
                })
                .thenExecute(() -> {
                    requireReleased(helper, player, "a disconnect");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void theEncounterEndingReleasesTheHold(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    husk.discard();
                })
                .thenExecuteAfter(2, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                            .isPresent()) {
                        helper.fail("the encounter must end once no hostile remains");
                    }
                    requireReleased(helper, player, "the encounter ending");
                    cleanUp(player);
                })
                .thenSucceed();
    }

    public static void aNonParticipantPlayerIsUntouched(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player bystander = ScenarioSupport.spawnPlayer(helper, 2, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Vec3[] origin = new Vec3[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    requireFree(helper, bystander, "a player who is in no encounter");
                    origin[0] = bystander.position();
                    bystander.setPos(origin[0].x + DRIFT, origin[0].y, origin[0].z);
                })
                .thenExecuteAfter(2, () -> {
                    requireAwayFrom(helper, bystander, origin[0], "a bystander walking past the fight");
                    cleanUp(player, bystander, husk);
                })
                .thenSucceed();
    }

    public static void theToggleOffRestoresFreeMovement(GameTestHelper helper) {
        prepare(helper, RESTRICT_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Vec3[] origin = new Vec3[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> passTurnToTheHusk(helper, player, husk))
                .thenExecuteAfter(2, () -> {
                    requireRooted(helper, player, "a participant player off its turn");
                    InitiativeConfig.overrideTurnsForTesting(RESTRICT_OFF);
                })
                .thenExecuteAfter(2, () -> {
                    requireReleased(helper, player, "turning the toggle off");
                    requireInteraction(helper, player, true, "a player with the toggle off");
                    origin[0] = player.position();
                    player.setPos(origin[0].x + DRIFT, origin[0].y, origin[0].z);
                })
                .thenExecuteAfter(2, () -> {
                    requireAwayFrom(helper, player, origin[0], "a player walking with the toggle off");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    private static void prepare(GameTestHelper helper, TurnConfig turns) {
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(turns);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        drainPool(helper);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static void fillPool(GameTestHelper helper) {
        for (BlockPos rim : POOL_RIM) {
            helper.setBlock(rim, Blocks.STONE);
        }
        helper.setBlock(POOL, Blocks.WATER);
    }

    private static void drainPool(GameTestHelper helper) {
        helper.setBlock(POOL, Blocks.AIR);
        for (BlockPos rim : POOL_RIM) {
            helper.setBlock(rim, Blocks.AIR);
        }
    }

    private static void standInThePool(GameTestHelper helper, Player player) {
        double feetY = helper.absoluteVec(new Vec3(0, POOL_FEET_Y, 0)).y;
        player.moveTo(player.getX(), feetY, player.getZ(), player.getYRot(), player.getXRot());
    }

    private static void passTurnToTheHusk(GameTestHelper helper, Player player, Husk husk) {
        requireCurrent(helper, player, player);
        turnOrder(helper, player).endTurn();
        requireCurrent(helper, player, husk);
    }

    private static TurnOrder turnOrder(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before its turn order can be inspected");
        }
        return encounter.turnOrder();
    }

    private static void requireCurrent(GameTestHelper helper, Player player, LivingEntity expected) {
        UUID current = turnOrder(helper, player).currentTurn().orElse(null);
        if (!expected.getUUID().equals(current)) {
            helper.fail("the current turn must belong to " + expected.getUUID() + " but was " + current);
        }
    }

    private static void requireRooted(GameTestHelper helper, Player player, String what) {
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (speed != 0.0) {
            helper.fail(what + " must be rooted but its movement speed was " + speed);
        }
        if (!OffTurnRestriction.isHeld(player.getUUID())) {
            helper.fail(what + " must be held");
        }
        if (!OffTurnRestriction.isRooted(player)) {
            helper.fail(what + " must carry the root the client keys its camera off");
        }
    }

    private static void requireFree(GameTestHelper helper, Player player, String what) {
        if (!movesAtItsOwnSpeed(player)) {
            helper.fail(what + " must move at its own speed but its movement speed was "
                    + player.getAttributeValue(Attributes.MOVEMENT_SPEED));
        }
    }

    private static void requireReleased(GameTestHelper helper, Player player, String exitPath) {
        if (!movesAtItsOwnSpeed(player)) {
            helper.fail(exitPath + " must give the player its speed back but left it at "
                    + player.getAttributeValue(Attributes.MOVEMENT_SPEED));
        }
        if (OffTurnRestriction.isHeld(player.getUUID())) {
            helper.fail(exitPath + " must leave no hold behind");
        }
    }

    /** Both halves of "free": the value the client walks at, and the modifier its camera keys off. */
    private static boolean movesAtItsOwnSpeed(Player player) {
        return !OffTurnRestriction.isRooted(player)
                && player.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        == player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
    }

    private static void requireInteraction(GameTestHelper helper, Player player, boolean allowed, String what) {
        boolean prevented = OffTurnRestriction.preventsInteraction(helper.getLevel(), player);
        if (prevented == allowed) {
            helper.fail(what + " must " + (allowed ? "be able to" : "not be able to") + " interact with the world");
        }
    }

    /**
     * Positions are judged against half a block, not exactly: every step these scenarios take is
     * whole blocks, so a fraction of one — the pull-back's own settling, a knockback — must not
     * decide whether a step was kept or undone.
     */
    private static void requireNear(GameTestHelper helper, Player player, Vec3 expected, String what) {
        Vec3 actual = player.position();
        if (Math.hypot(actual.x - expected.x, actual.z - expected.z) > 0.5) {
            helper.fail(what + " must end up at " + expected + " but was at " + actual);
        }
    }

    /** Heights are judged loosely for the same reason positions are, but tighter than the drop they judge. */
    private static void requireHeight(GameTestHelper helper, Player player, double expected, String what) {
        if (Math.abs(player.getY() - expected) > 0.15) {
            helper.fail(what + " must end up at height " + expected + " but was at " + player.getY());
        }
    }

    /** A free vertical only ever takes a player further down, since gravity keeps pulling on the drop. */
    private static void requireNoHigherThan(GameTestHelper helper, Player player, double dropped, String what) {
        if (player.getY() > dropped + 0.15) {
            helper.fail(what + " must keep its drop to " + dropped + " but was back up at " + player.getY());
        }
    }

    private static void requireAwayFrom(GameTestHelper helper, Player player, Vec3 origin, String what) {
        Vec3 actual = player.position();
        if (Math.hypot(actual.x - origin.x, actual.z - origin.z) < 0.5) {
            helper.fail(what + " must be kept but the player is back at " + origin);
        }
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
