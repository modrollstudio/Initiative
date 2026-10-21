package studio.modroll.initiative.gametest;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.CombatSuppression;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.DiceRoller;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * M1 encounter-bubble scenarios. Each runs in its own GameTest batch: encounters scan a radius of
 * real world space, so concurrent tests in one batch could pull each other's mobs into a bubble.
 * Turns are disabled here — bubble mechanics must hold on their own (see TurnOrderScenarios).
 */
public final class EncounterScenarios {

    private static final EncounterConfig TEST_CONFIG =
            new EncounterConfig(true, true, true, false, true, true, true, true, 3.0, 5.0);
    private static final EncounterConfig SIGHT_NOT_REQUIRED =
            new EncounterConfig(true, true, true, false, true, true, false, true, 3.0, 5.0);
    private static final TurnConfig TURNS_DISABLED =
            new TurnConfig(false, 100, 100, 20.0, 12, false, false, false, Set.of());

    /**
     * The sight scenarios: the player at (1, 1) hits a husk at (2, 2), and a stone wall along z = 3,
     * at eye height, cuts off the back row z = 4 — still inside the trigger radius of the fight.
     */
    private static final int WALL_Z = 3;

    private static final int WALL_MIN_X = 0;
    private static final int WALL_MAX_X = 4;
    private static final int WALL_MIN_Y = 2;
    private static final int WALL_MAX_Y = 3;
    private static final int BACK_ROW_Z = 4;

    private EncounterScenarios() {}

    public static void attackFormsEncounterSuppressingBoth(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Player player = spawnPlayer(helper, 1, 1);
        Husk husk = spawnHusk(helper, 2, 2);
        husk.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("a player attacking a hostile must form an encounter");
        }
        if (!encounter.contains(husk.getUUID())) {
            helper.fail("the attacked hostile must be in the encounter");
        }
        if (!RollService.isSuppressed(player)) {
            helper.fail("the player must have the suppression flag inside the bubble");
        }
        if (!RollService.isSuppressed(husk)) {
            helper.fail("the hostile must have the suppression flag inside the bubble");
        }
        player.discard();
        husk.discard();
        helper.succeed();
    }

    public static void everyExitPathClearsSuppression(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Player player = spawnPlayer(helper, 1, 1);
        Husk dies = spawnHusk(helper, 1, 2);
        Husk flees = spawnHusk(helper, 2, 1);
        dies.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        if (!RollService.isSuppressed(flees)) {
            helper.fail("a hostile inside the trigger radius must be pulled into the encounter");
        }
        Vec3 outside = helper.absoluteVec(new Vec3(1, 1, 10));
        helper.startSequence()
                .thenExecute(() -> flees.teleportTo(outside.x, outside.y, outside.z))
                .thenExecuteAfter(2, () -> {
                    if (RollService.isSuppressed(flees)) {
                        helper.fail("fleeing beyond the leave radius must clear the suppression flag");
                    }
                    flees.discard();
                    dies.kill();
                })
                .thenExecuteAfter(2, () -> {
                    if (RollService.isSuppressed(dies)) {
                        helper.fail("death must clear the suppression flag");
                    }
                    player.discard();
                })
                .thenExecuteAfter(2, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                            .isPresent()) {
                        helper.fail("the encounter must end once no valid participants remain");
                    }
                    Set<UUID> leaked = CombatSuppression.suppressedUuids();
                    if (!leaked.isEmpty()) {
                        helper.fail("no suppressed UUID may remain after all encounters end, but found " + leaked);
                    }
                })
                .thenSucceed();
    }

    public static void entityOutsideRadiusIsUntouched(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Player player = spawnPlayer(helper, 1, 1);
        Husk target = spawnHusk(helper, 2, 2);
        Husk outsider = spawnHusk(helper, 2, 10);
        target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        if (RollService.isSuppressed(outsider)) {
            helper.fail("an entity outside the trigger radius must keep Critfall's real-time combat");
        }
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (RollService.isSuppressed(outsider)) {
                        helper.fail("an entity that stays outside the radius must never join");
                    }
                    if (EncounterManager.encounterContaining(helper.getLevel(), outsider.getUUID())
                            .isPresent()) {
                        helper.fail("an entity outside the radius must not be an encounter member");
                    }
                    player.discard();
                    target.discard();
                    outsider.discard();
                })
                .thenSucceed();
    }

    /**
     * The trigger listens to Critfall's CombatInteractionEvent, which fires before all of
     * Critfall's own gating — so a swing Critfall would resolve as a miss or fumble (forced here
     * via minimum dice) must still start an encounter.
     */
    public static void whiffedAttackStillFormsEncounter(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Player player = spawnPlayer(helper, 1, 1);
        Husk husk = spawnHusk(helper, 2, 2);
        RollService.setRoller(new DiceRoller(minimumDice()));
        try {
            husk.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        } finally {
            RollService.resetRoller();
        }
        if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .isEmpty()) {
            helper.fail("a hit Critfall would cancel must still form an encounter");
        }
        if (!RollService.isSuppressed(player) || !RollService.isSuppressed(husk)) {
            helper.fail("both participants must be suppressed even when the triggering hit whiffs");
        }
        player.discard();
        husk.discard();
        helper.succeed();
    }

    public static void disabledFlagFormsNoEncounter(GameTestHelper helper) {
        prepare(helper, new EncounterConfig(false, true, true, false, true, true, true, true, 3.0, 5.0));
        Player player = spawnPlayer(helper, 1, 1);
        Husk husk = spawnHusk(helper, 2, 2);
        husk.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .isPresent()) {
            helper.fail("no encounter may form while the feature flag is off");
        }
        if (RollService.isSuppressed(player) || RollService.isSuppressed(husk)) {
            helper.fail("nothing may be suppressed while the feature flag is off");
        }
        player.discard();
        husk.discard();
        helper.succeed();
    }

    public static void wallKeepsAHostileOutOfTheFight(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        buildWall(helper);
        Player player = spawnPlayer(helper, 1, 1);
        Husk target = spawnHusk(helper, 2, 2);
        Husk inView = spawnHusk(helper, 0, 2);
        Husk behindWall = spawnHusk(helper, 2, BACK_ROW_Z);
        target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        if (!RollService.isSuppressed(inView)) {
            helper.fail("a hostile in plain view of the fight must be pulled in");
        }
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (isInAnEncounter(helper, behindWall)) {
                        helper.fail("a hostile behind a solid wall must not join the fight");
                    }
                    discardAll(player, target, inView, behindWall);
                    clearWall(helper);
                })
                .thenSucceed();
    }

    public static void attackerBehindAWallJoins(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        buildWall(helper);
        Player player = spawnPlayer(helper, 1, 1);
        Husk target = spawnHusk(helper, 2, 2);
        Husk behindWall = spawnHusk(helper, 2, BACK_ROW_Z);
        target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        player.hurt(helper.getLevel().damageSources().mobAttack(behindWall), 1.0f);
        if (!isInAnEncounter(helper, behindWall)) {
            helper.fail("a mob that attacks a participant must join even without line of sight");
        }
        discardAll(player, target, behindWall);
        clearWall(helper);
        helper.succeed();
    }

    /**
     * Both back-row husks start behind the wall, a divider between them so neither can see the other.
     * Opening the wall in front of one lets it see the player; the other must stay out even once its
     * neighbour has joined.
     */
    public static void gainingSightMidFightJoins(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        buildWall(helper);
        placeColumn(helper, 2, WALL_Z + 1, Blocks.STONE);
        Player player = spawnPlayer(helper, 1, 1);
        Husk target = spawnHusk(helper, 2, 2);
        Husk revealed = spawnHusk(helper, 0, BACK_ROW_Z);
        Husk hidden = spawnHusk(helper, 4, BACK_ROW_Z);
        target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (isInAnEncounter(helper, revealed) || isInAnEncounter(helper, hidden)) {
                        helper.fail("no hostile behind the wall may join before it can see the fight");
                    }
                    placeColumn(helper, 0, WALL_Z, Blocks.AIR);
                    placeColumn(helper, 1, WALL_Z, Blocks.AIR);
                })
                .thenExecuteAfter(2, () -> {
                    if (!isInAnEncounter(helper, revealed)) {
                        helper.fail("a hostile that gains sight of a participant mid-fight must join");
                    }
                    if (isInAnEncounter(helper, hidden)) {
                        helper.fail("a hostile that stays hidden must not join");
                    }
                    discardAll(player, target, revealed, hidden);
                    placeColumn(helper, 2, WALL_Z + 1, Blocks.AIR);
                    clearWall(helper);
                })
                .thenSucceed();
    }

    /**
     * pull_nearby_players sweeps players in by the same sight rule as hostiles: a friend on the far
     * side of the wall stays out, and joins on the tick an opening lets them see the husk.
     */
    public static void playerBehindAWallJoinsOnceTheyCanSee(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        buildWall(helper);
        Player striker = spawnPlayer(helper, 1, 1);
        Husk target = spawnHusk(helper, 2, 2);
        Player behindWall = spawnPlayer(helper, 2.5, BACK_ROW_Z + 0.5);
        target.hurt(helper.getLevel().damageSources().playerAttack(striker), 1.0f);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (isInAnEncounter(helper, behindWall)) {
                        helper.fail("a player behind a solid wall must not be pulled into the fight");
                    }
                    placeColumn(helper, 2, WALL_Z, Blocks.AIR);
                })
                .thenExecuteAfter(2, () -> {
                    if (!isInAnEncounter(helper, behindWall)) {
                        helper.fail("a player who gains sight of a participant mid-fight must join");
                    }
                    discardAll(striker, target, behindWall);
                    clearWall(helper);
                })
                .thenSucceed();
    }

    /**
     * The shipped tag is empty, so the illusioner — a hostile no other scenario spawns — is bound
     * into it for this test alone. It stands in plain view and attacks a participant, so every join
     * path is offered to it; the bindings are restored before the verdict so a failure cannot leak.
     */
    public static void taggedMobNeverJoins(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Map<TagKey<EntityType<?>>, List<Holder<EntityType<?>>>> original = entityTypeTags();
        tagNeverJoins(original, EntityType.ILLUSIONER);
        Player player = spawnPlayer(helper, 1, 1);
        Husk target = spawnHusk(helper, 2, 2);
        Illusioner tagged = helper.spawn(EntityType.ILLUSIONER, new BlockPos(0, 1, 2));
        tagged.setNoAi(true);
        target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        player.hurt(helper.getLevel().damageSources().mobAttack(tagged), 1.0f);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    boolean joined = isInAnEncounter(helper, tagged);
                    BuiltInRegistries.ENTITY_TYPE.bindTags(original);
                    discardAll(player, target, tagged);
                    if (joined) {
                        helper.fail("an entity in #initiative:never_joins must never join an encounter");
                    }
                })
                .thenSucceed();
    }

    public static void sightNotRequiredPullsThroughWalls(GameTestHelper helper) {
        prepare(helper, SIGHT_NOT_REQUIRED);
        buildWall(helper);
        Player player = spawnPlayer(helper, 1, 1);
        Husk target = spawnHusk(helper, 2, 2);
        Husk behindWall = spawnHusk(helper, 2, BACK_ROW_Z);
        target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        if (!isInAnEncounter(helper, behindWall)) {
            helper.fail("with require_line_of_sight off a hostile behind a wall must join as before");
        }
        discardAll(player, target, behindWall);
        clearWall(helper);
        helper.succeed();
    }

    private static boolean isInAnEncounter(GameTestHelper helper, LivingEntity entity) {
        return EncounterManager.encounterContaining(helper.getLevel(), entity.getUUID())
                .isPresent();
    }

    private static void discardAll(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }

    private static void buildWall(GameTestHelper helper) {
        for (int x = WALL_MIN_X; x <= WALL_MAX_X; x++) {
            placeColumn(helper, x, WALL_Z, Blocks.STONE);
        }
    }

    /** Walls reach above the small template, which the framework does not clear between tests. */
    private static void clearWall(GameTestHelper helper) {
        for (int x = WALL_MIN_X; x <= WALL_MAX_X; x++) {
            placeColumn(helper, x, WALL_Z, Blocks.AIR);
        }
    }

    private static void placeColumn(GameTestHelper helper, int x, int z, Block block) {
        for (int y = WALL_MIN_Y; y <= WALL_MAX_Y; y++) {
            helper.setBlock(new BlockPos(x, y, z), block);
        }
    }

    private static Map<TagKey<EntityType<?>>, List<Holder<EntityType<?>>>> entityTypeTags() {
        return BuiltInRegistries.ENTITY_TYPE
                .getTags()
                .collect(Collectors.toMap(
                        Pair::getFirst, pair -> pair.getSecond().stream().toList()));
    }

    /** The 1.21.1 reload path itself: every tag rebound, with one type added to never_joins. */
    private static void tagNeverJoins(
            Map<TagKey<EntityType<?>>, List<Holder<EntityType<?>>>> original, EntityType<?> type) {
        Map<TagKey<EntityType<?>>, List<Holder<EntityType<?>>>> tagged = new HashMap<>(original);
        List<Holder<EntityType<?>>> members =
                new ArrayList<>(original.getOrDefault(EncounterManager.NEVER_JOINS, List.of()));
        members.add(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type));
        tagged.put(EncounterManager.NEVER_JOINS, members);
        BuiltInRegistries.ENTITY_TYPE.bindTags(tagged);
    }

    private static void prepare(GameTestHelper helper, EncounterConfig config) {
        InitiativeConfig.overrideEncountersForTesting(config);
        InitiativeConfig.overrideTurnsForTesting(TURNS_DISABLED);
        InitiativeConfig.overrideActionsForTesting(ScenarioSupport.ACTIONS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    /** Every bounded draw returns 0, so every die shows its lowest face (a d20 rolls 1). */
    private static RandomGenerator minimumDice() {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new IllegalStateException("forced dice must only draw bounded ints");
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private static Player spawnPlayer(GameTestHelper helper, double x, double z) {
        return ScenarioSupport.spawnPlayer(helper, x, z);
    }

    private static Husk spawnHusk(GameTestHelper helper, int x, int z) {
        return ScenarioSupport.spawnHusk(helper, x, z);
    }
}
