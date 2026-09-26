package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackOutcome;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.AttackAttempt;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.action.BlockPlacement;
import studio.modroll.initiative.action.Reactions;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.CoverConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * M5b line-of-sight cover. The attacker and target are placed on a shared axis with a stone wall
 * built between them; a one-block wall occludes the target's lower hitbox corners (half of the eight
 * rays) and a two-block wall occludes all of them (total). Assertions read the {@link AttackResult}
 * Critfall echoes back so the tests prove the exact defender AC bonus reached Critfall, that the base
 * AC is untouched, and that the same forced roll flips from a hit to a miss once cover applies.
 */
public final class CoverScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 32.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    /** Attacking spends no action, so a scenario can drive several attacks across one turn. */
    private static final ActionConfig ACTIONS_MULTI = new ActionConfig(
            true, 6.0, false, 32.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    private static final CoverConfig COVER_DEFAULT = new CoverConfig(true, 2, 5, 0.5, 0.75, true, true, 1.0);
    /** Thresholds lowered so the same half-blocked geometry promotes to three-quarter cover (+5). */
    private static final CoverConfig COVER_TQ_AT_HALF = new CoverConfig(true, 2, 5, 0.25, 0.5, true, true, 1.0);

    private static final CoverConfig COVER_DISABLED = new CoverConfig(false, 2, 5, 0.5, 0.75, true, true, 1.0);
    private static final CoverConfig COVER_BUILD_FREE = new CoverConfig(true, 2, 5, 0.5, 0.75, true, false, 1.0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);

    private CoverScenarios() {}

    public static void openLineOfSightAppliesNoCover(GameTestHelper helper) {
        prepare(helper, ACTIONS, COVER_DEFAULT);
        Player player = spawnAttacker(helper, 1);
        Husk husk = spawnTarget(helper, 6);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        AttackResult result = driveAttack(helper, player, husk, 15);
        if (result.defenderAcBonus() != 0) {
            helper.fail("an open line of sight must grant no cover but gave +" + result.defenderAcBonus());
        }
        if (result.armorClass() != result.baseArmorClass()) {
            helper.fail("no cover must leave the effective AC equal to the base AC");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void halfCoverTurnsAHitIntoAMiss(GameTestHelper helper) {
        prepare(helper, ACTIONS_MULTI, COVER_DEFAULT);
        Player player = spawnAttacker(helper, 1);
        Husk husk = spawnTarget(helper, 6);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        // Probe with an open line to learn the husk's base AC and the attacker's flat bonus.
        AttackResult open = driveAttack(helper, player, husk, 10);
        int baseAc = open.baseArmorClass();
        int attackBonus = open.attackTotal() - open.natural();
        int gapNatural = baseAc - attackBonus; // a roll whose total lands exactly on the base AC
        if (gapNatural < 2 || gapNatural > 19) {
            helper.fail("scenario needs a mid-range natural to sit on the base AC but computed " + gapNatural);
        }
        placeWall(helper, 5, 2, 4, 1); // one block tall: occludes the lower four hitbox corners
        AttackResult covered = driveAttack(helper, player, husk, gapNatural);
        if (covered.defenderAcBonus() != 2) {
            helper.fail("half cover must feed +2 to Critfall but fed +" + covered.defenderAcBonus());
        }
        if (covered.baseArmorClass() != baseAc) {
            helper.fail("cover must not change the base AC: was " + baseAc + " now " + covered.baseArmorClass());
        }
        if (covered.armorClass() != baseAc + 2) {
            helper.fail("half cover must raise the effective AC by 2 but was " + covered.armorClass());
        }
        if (covered.attackTotal() < baseAc) {
            helper.fail("the covered roll must still clear the base AC to prove the flip is cover's doing");
        }
        if (covered.outcome() != AttackOutcome.MISS) {
            helper.fail("the roll that clears base AC must miss once cover raises it but was " + covered.outcome());
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void loweredThresholdsPromoteHalfToThreeQuarter(GameTestHelper helper) {
        prepare(helper, ACTIONS, COVER_TQ_AT_HALF);
        Player player = spawnAttacker(helper, 1);
        Husk husk = spawnTarget(helper, 6);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        placeWall(helper, 5, 2, 4, 1); // same half-blocked geometry as the +2 case
        AttackResult result = driveAttack(helper, player, husk, 15);
        if (result.defenderAcBonus() != 5) {
            helper.fail("with the three-quarter threshold at the half-blocked fraction the bonus must be +5 but was +"
                    + result.defenderAcBonus());
        }
        if (result.armorClass() != result.baseArmorClass() + 5) {
            helper.fail("three-quarter cover must raise the effective AC by 5");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void totalCoverRejectsTheAttackAndKeepsTheAction(GameTestHelper helper) {
        prepare(helper, ACTIONS, COVER_DEFAULT);
        Player player = spawnAttacker(helper, 1);
        Husk husk = spawnTarget(helper, 6);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        placeWall(helper, 5, 2, 4, 2); // two blocks tall: occludes every hitbox corner
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(true);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
            if (attempt.status() != AttackStatus.TOTAL_COVER) {
                helper.fail("total cover must reject the attack but was " + attempt.status());
            }
            if (dice.d20Draws != 0) {
                helper.fail("a rejected attack must not roll but drew " + dice.d20Draws);
            }
        } finally {
            RollService.resetRoller();
        }
        if (husk.getHealth() != before) {
            helper.fail("a rejected attack must not damage the target");
        }
        // The action is untouched, so an enabled action still goes through — proof the reject spent nothing.
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionEconomy.ActionOutcome.PERFORMED) {
            helper.fail("total cover must leave the turn's action intact");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void coverRecomputesPerAttack(GameTestHelper helper) {
        prepare(helper, ACTIONS_MULTI, COVER_DEFAULT);
        Player player = spawnAttacker(helper, 1);
        Husk husk = spawnTarget(helper, 6);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        placeWall(helper, 5, 2, 4, 1);
        AttackResult behindWall = driveAttack(helper, player, husk, 2); // misses; leaves the husk alive
        if (behindWall.defenderAcBonus() != 2) {
            helper.fail("shooting through the wall must give half cover but gave +" + behindWall.defenderAcBonus());
        }
        clearWallZone(helper); // the geometry changes between attacks: the wall is gone
        AttackResult clearShot = driveAttack(helper, player, husk, 2);
        if (clearShot.defenderAcBonus() != 0) {
            helper.fail("cover must recompute from live geometry, not cache: without the wall it must be +0 but was +"
                    + clearShot.defenderAcBonus());
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void opportunityAttackRespectsTotalCover(GameTestHelper helper) {
        prepare(helper, ACTIONS, COVER_DEFAULT);
        Player player = spawnAttacker(helper, 4);
        Husk husk = spawnTarget(helper, 6); // reactor ~2.5 blocks away: within reach
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        placeWall(helper, 5, 2, 4, 2); // total cover between the reactor and the mover
        float before = player.getHealth();
        Vec3 from = player.position();
        // No dice scripted: total cover must suppress the opportunity attack before any roll.
        Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(-6, 0, 0));
        if (!encounter.flags().hasReaction(husk.getUUID())) {
            helper.fail("total cover must suppress the opportunity attack, leaving the reaction intact");
        }
        if (player.getHealth() != before) {
            helper.fail("a suppressed opportunity attack must not damage the mover");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void coverOffMatchesPreM5b(GameTestHelper helper) {
        prepare(helper, ACTIONS, COVER_DISABLED);
        Player player = spawnAttacker(helper, 1);
        Husk husk = spawnTarget(helper, 6);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        placeWall(helper, 5, 2, 4, 2); // a full wall that would be total cover if the feature were on
        AttackResult result = driveAttack(helper, player, husk, 15);
        if (result.defenderAcBonus() != 0) {
            helper.fail("cover disabled must ignore the wall entirely but gave +" + result.defenderAcBonus());
        }
        if (result.armorClass() != result.baseArmorClass()) {
            helper.fail("cover disabled must leave the effective AC at the base AC");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void buildingSpendsMovementAndBlocksWhenExhausted(GameTestHelper helper) {
        prepare(helper, ACTIONS, COVER_DEFAULT);
        Player player = spawnAttacker(helper, 1);
        Husk husk = spawnTarget(helper, 6);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        if (!BlockPlacement.onPlaceAttempt(helper.getLevel(), player)) {
            helper.fail("the first placement with a full budget must be allowed");
        }
        if (Math.abs(encounter.budget().movementRemaining() - 5.0) > 1.0e-6) {
            helper.fail("placing a block must spend one block of movement but left "
                    + encounter.budget().movementRemaining());
        }
        for (int i = 0; i < 5; i++) {
            BlockPlacement.onPlaceAttempt(helper.getLevel(), player); // drain the remaining five
        }
        if (!encounter.budget().movementExhausted()) {
            helper.fail("six placements must exhaust the six-block budget");
        }
        if (BlockPlacement.onPlaceAttempt(helper.getLevel(), player)) {
            helper.fail("placement with no movement left must be prevented");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void buildingRulesRespectToggleTurnAndMembership(GameTestHelper helper) {
        prepare(helper, ACTIONS, COVER_DEFAULT);
        Player participant = spawnAttacker(helper, 1);
        Husk husk = spawnTarget(helper, 6);
        ScenarioSupport.formEncounter(helper, participant, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, participant);
        // One charged placement establishes the budget (5 of 6 left) to measure the untouched cases against.
        BlockPlacement.onPlaceAttempt(helper.getLevel(), participant);
        double established = encounter.budget().movementRemaining();

        InitiativeConfig.overrideCoverForTesting(COVER_BUILD_FREE);
        if (!BlockPlacement.onPlaceAttempt(helper.getLevel(), participant)) {
            helper.fail("with the cost toggled off placement must always be allowed");
        }
        if (Math.abs(encounter.budget().movementRemaining() - established) > 1.0e-6) {
            helper.fail("free placement must not spend movement");
        }

        InitiativeConfig.overrideCoverForTesting(COVER_DEFAULT);
        encounter.turnOrder().endTurn(); // hand the turn to the hostile: not the player's turn
        if (!BlockPlacement.onPlaceAttempt(helper.getLevel(), participant)) {
            helper.fail("placing off your turn must be left untouched (allowed)");
        }
        if (Math.abs(encounter.budget().movementRemaining() - established) > 1.0e-6) {
            helper.fail("placing off your turn must not touch the acting participant's budget");
        }

        Player outsider = ScenarioSupport.spawnPlayer(helper, 20, 20);
        if (!BlockPlacement.onPlaceAttempt(helper.getLevel(), outsider)) {
            helper.fail("a non-participant's placement must be left untouched (allowed)");
        }
        cleanUp(participant, husk, outsider);
        helper.succeed();
    }

    private static AttackResult driveAttack(GameTestHelper helper, Player player, Husk husk, int natural) {
        ScenarioSupport.forceD20Faces(natural);
        try {
            AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
            if (attempt.status() != AttackStatus.PERFORMED) {
                helper.fail("the attack must be performed but was " + attempt.status());
            }
            return attempt.result().orElseThrow();
        } finally {
            RollService.resetRoller();
        }
    }

    /**
     * The "empty" plot is an enclosed box — a stone floor at rel y[1,2] and a barrier ceiling at
     * rel y[4,5] — so combatants stand at rel y=2 (feet on the floor top), the only level where the
     * whole hitbox and the attacker's eye clear both. Otherwise buried corners or an eye inside the
     * ceiling read as phantom cover.
     */
    private static final int FEET_Y = 2;

    private static final int WALL_X = 5;
    private static final int WALL_MIN_Z = 2;
    private static final int WALL_MAX_Z = 4;
    private static final int WALL_MAX_HEIGHT = 2;

    private static Player spawnAttacker(GameTestHelper helper, double x) {
        Player player = ScenarioSupport.spawnPlayer(helper, x, 3.5);
        lift(helper, player);
        return player;
    }

    private static Husk spawnTarget(GameTestHelper helper, int blockX) {
        Husk husk = ScenarioSupport.spawnHusk(helper, blockX, 3);
        lift(helper, husk);
        return husk;
    }

    private static void lift(GameTestHelper helper, LivingEntity entity) {
        double feetY = helper.absoluteVec(new Vec3(0, FEET_Y, 0)).y;
        entity.moveTo(entity.getX(), feetY, entity.getZ(), entity.getYRot(), entity.getXRot());
    }

    private static void placeWall(GameTestHelper helper, int x, int minZ, int maxZ, int height) {
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = FEET_Y; y < FEET_Y + height; y++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
    }

    /**
     * The wall footprint spills outside the small "empty" template, which the framework does not clear
     * between tests, so each scenario resets it to air first — otherwise a prior wall leaks into an
     * open-line-of-sight plot and reads as total cover.
     */
    private static void clearWallZone(GameTestHelper helper) {
        for (int z = WALL_MIN_Z; z <= WALL_MAX_Z; z++) {
            for (int y = FEET_Y; y < FEET_Y + WALL_MAX_HEIGHT; y++) {
                helper.setBlock(new BlockPos(WALL_X, y, z), Blocks.AIR);
            }
        }
    }

    private static void prepare(GameTestHelper helper, ActionConfig actions, CoverConfig cover) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(cover);
        clearWallZone(helper);
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
