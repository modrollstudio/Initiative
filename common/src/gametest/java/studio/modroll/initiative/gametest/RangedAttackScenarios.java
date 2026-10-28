package studio.modroll.initiative.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.AttackDelivery;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackOutcome;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.api.event.CritfallEvents;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ChecksConfig;
import studio.modroll.initiative.config.CoverConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * Ranged attacks on a turn, fired the way a player or a mob really fires: a mock server player draws
 * through the server's own use path and releases at full draw, and a skeleton or pillager shoots from
 * its own mob turn. The arrows fly for real, so every combatant stands inside the plot's barrier box
 * on one line along z, and the ranges are shrunk to fit it: reach 1.5, normal range 2.5, long range
 * 3.5 — a target two blocks out is in normal range, three is past it, four is past long range. Each
 * roll is read back from Critfall's own post-roll event, which proves it was Critfall's roll, with
 * the projectile delivery, that decided the hit.
 */
public final class RangedAttackScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, true, 1.0, 30.0);
    /** No encounter can form, so a mob's shot is Critfall's own automatic roll. */
    private static final EncounterConfig NO_ENCOUNTERS =
            new EncounterConfig(false, true, true, false, true, true, true, true, 1.0, 30.0);

    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final TurnConfig TURNS_HOLDING_PLAYERS =
            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, true, Set.of());

    private static final double REACH = 1.5;
    private static final double NORMAL_RANGE = 2.5;
    private static final double LONG_RANGE = 3.5;

    private static final ActionConfig RANGED = actions(true, false, NORMAL_RANGE, LONG_RANGE);
    private static final ActionConfig RANGED_OFF = actions(false, false, NORMAL_RANGE, LONG_RANGE);
    /** The cover plot puts its target three blocks out, so its ranges are the shipped ones. */
    private static final ActionConfig RANGED_FAR = actions(true, false, 24.0, 64.0);

    private static final CoverConfig COVER_OFF = ScenarioSupport.COVER_OFF;
    private static final CoverConfig COVER_DEFAULT = new CoverConfig(true, 2, 5, 0.5, 0.75, true, true, 1.0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> PLAYER_FIRST_OF_THREE = List.of(20, 1, 1);
    private static final List<Integer> MOB_FIRST = List.of(1, 20);

    /** Feet on the plot's stone floor, the one height where an eye and a whole hitbox clear its barrier ceiling. */
    private static final int FEET_Y = 2;

    private static final double SHOOTER_Z = 0.5;
    private static final int LANE_X = 2;
    private static final int IN_RANGE_Z = 2;
    private static final int PAST_NORMAL_Z = 3;
    private static final int PAST_LONG_Z = 4;
    private static final int WALL_Z = 2;

    private static final int NATURAL_HIT = 19;
    private static final int NATURAL_MISS = 2;
    private static final int SETTLE_TICKS = 10;
    /** Past the hit's invulnerability and through at least one of fire's once-a-second hurts. */
    private static final int BURN_TICKS = 40;
    /** A server player that just joined ignores every hurt for its first 60 ticks. */
    private static final int SPAWN_PROTECTION_TICKS = 61;

    private static final boolean WITHOUT_CHECKS = Boolean.getBoolean("initiative.gametest.withoutChecks");

    private static final List<Rolled> ROLLED = new ArrayList<>();
    private static boolean recording;

    static {
        CritfallEvents.onPostAttackRoll(event -> {
            if (recording) {
                ROLLED.add(new Rolled(
                        event.attacker(), event.target(), event.context().delivery(), event.result()));
            }
        });
    }

    private record Rolled(LivingEntity attacker, LivingEntity target, AttackDelivery delivery, AttackResult result) {}

    private RangedAttackScenarios() {}

    /** The tentpole: the arrow is the attack, Critfall rolls it as a projectile, and a hit lands its damage. */
    public static void aPlayersArrowOnItsTurnRollsAndHits(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, IN_RANGE_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(NATURAL_HIT, NATURAL_HIT);
        loose(helper, player, husk);
        if (ActionEconomy.budgetOf(encounterOf(helper, player), player).actionAvailable()) {
            helper.fail("firing on your turn must spend your action");
        }
        helper.startSequence()
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(1, () -> {
                    Rolled rolled = ROLLED.get(0);
                    requireProjectileRoll(helper, rolled, player, husk);
                    if (rolled.result().outcome() != AttackOutcome.HIT) {
                        helper.fail("a natural " + NATURAL_HIT + " must hit but was "
                                + rolled.result().outcome());
                    }
                    if (rolled.result().roll().mode() != RollMode.NORMAL || dice.d20Draws != 1) {
                        helper.fail("a shot in normal range with nobody at the shooter's elbow must roll one d20");
                    }
                    if (husk.getHealth() >= before) {
                        helper.fail("a hit must land Critfall's damage on the target");
                    }
                    if (!arrowsAround(helper).isEmpty()) {
                        helper.fail("an arrow that hit must be spent in its target");
                    }
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /** Critfall 0.2.16's miss: the arrow carries on past the target instead of bouncing back at the shooter. */
    public static void aMissedArrowFliesPastItsTarget(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, IN_RANGE_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        float before = husk.getHealth();
        ScenarioSupport.forceD20Faces(NATURAL_MISS, NATURAL_MISS);
        loose(helper, player, husk);
        helper.startSequence()
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(2, () -> {
                    Rolled rolled = ROLLED.get(0);
                    requireProjectileRoll(helper, rolled, player, husk);
                    if (rolled.result().outcome() != AttackOutcome.MISS) {
                        helper.fail("a natural " + NATURAL_MISS + " must miss but was "
                                + rolled.result().outcome());
                    }
                    if (husk.getHealth() < before) {
                        helper.fail("a missed arrow must deal no damage");
                    }
                    List<AbstractArrow> arrows = arrowsAround(helper);
                    if (arrows.size() != 1 || arrows.get(0).getZ() <= husk.getZ()) {
                        helper.fail("a missed arrow must fly on past its target, not bounce back toward the shooter");
                    }
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /**
     * A Flame bow's arrow that hits sets the target alight, as vanilla's does, and the fire burns on
     * in real time. Checks runs fully here, so the burning cannot be what rolls the attack again: one
     * d20 for the shot, however many fire hurts follow.
     */
    public static void aFlamingArrowThatHitsSetsTheTargetAlight(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        InitiativeConfig.overrideChecksForTesting(new ChecksConfig(true));
        if (!WITHOUT_CHECKS && !ChecksIntegration.present()) {
            helper.fail("the GameTest runs must have Checks installed, or be started with -PwithoutChecks");
        }
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        enchantWithFlame(helper, player.getMainHandItem());
        Husk husk = spawnTarget(helper, LANE_X, IN_RANGE_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        float[] afterHit = new float[1];
        ScenarioSupport.ScriptedDice dice =
                ScenarioSupport.forceD20Faces(NATURAL_HIT, NATURAL_HIT, NATURAL_HIT, NATURAL_HIT);
        loose(helper, player, husk);
        helper.startSequence()
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(1, () -> {
                    if (ROLLED.get(0).result().outcome() != AttackOutcome.HIT) {
                        helper.fail("a natural " + NATURAL_HIT + " must hit");
                    }
                    if (!husk.isOnFire()) {
                        helper.fail("a burning arrow that hits must set its target alight");
                    }
                    afterHit[0] = husk.getHealth();
                })
                .thenExecuteAfter(BURN_TICKS, () -> {
                    if (husk.getHealth() >= afterHit[0]) {
                        helper.fail("the target must burn after the hit");
                    }
                    if (ROLLED.size() != 1 || dice.d20Draws != 1) {
                        helper.fail("the hit must roll once, however long the target burns, but rolled " + ROLLED.size()
                                + " attack(s) and " + dice.d20Draws + " d20(s)");
                    }
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /** A miss stays a miss: the burning arrow flies on, and the target it passed is not alight. */
    public static void aFlamingArrowThatMissesSetsNothingAlight(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        enchantWithFlame(helper, player.getMainHandItem());
        Husk husk = spawnTarget(helper, LANE_X, IN_RANGE_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ScenarioSupport.forceD20Faces(NATURAL_MISS, NATURAL_MISS);
        loose(helper, player, husk);
        helper.startSequence()
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(1, () -> {
                    if (ROLLED.get(0).result().outcome() != AttackOutcome.MISS) {
                        helper.fail("a natural " + NATURAL_MISS + " must miss");
                    }
                    if (husk.isOnFire()) {
                        helper.fail("a burning arrow that missed must not set its target alight");
                    }
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /**
     * Off turn a draw and a loaded crossbow's shot are both refused, and a bow released anyway — a
     * draw held past the turn — looses nothing. Loading the crossbow goes through. The off-turn hold
     * is off here, so the refusals are the ranged attack's own and not the hold's.
     */
    public static void firingOffTurnIsRefusedButLoadingIsNot(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, PAST_LONG_Z);
        ScenarioSupport.formEncounter(helper, player, MOB_FIRST, husk);
        ItemStack bow = player.getMainHandItem();
        aimAt(player, husk);
        useMainHand(helper, player);
        if (player.isUsingItem()) {
            helper.fail("drawing a bow off turn must be refused");
        }
        bow.releaseUsing(helper.getLevel(), player, 0);
        if (!arrowsAround(helper).isEmpty()) {
            helper.fail("a bow released off turn must loose no arrow");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CROSSBOW));
        ItemStack crossbow = player.getMainHandItem();
        useMainHand(helper, player);
        if (!player.isUsingItem()) {
            helper.fail("loading a crossbow off turn must be allowed");
        }
        crossbow.releaseUsing(helper.getLevel(), player, 0);
        player.stopUsingItem();
        if (!CrossbowItem.isCharged(crossbow)) {
            helper.fail("the crossbow must be loaded once the draw is held to full charge");
        }
        useMainHand(helper, player);
        if (!CrossbowItem.isCharged(crossbow) || !arrowsAround(helper).isEmpty()) {
            helper.fail("a loaded crossbow must not fire off turn");
        }
        if (!ROLLED.isEmpty()) {
            helper.fail("nothing fired off turn may roll");
        }
        finish(helper, player, husk);
        helper.succeed();
    }

    /** A shot is the turn's attack: with the action spent, the draw is refused like a second swing. */
    public static void aSpentActionRefusesTheDraw(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, IN_RANGE_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ActionEconomy.budgetOf(encounterOf(helper, player), player).useAction();
        aimAt(player, husk);
        useMainHand(helper, player);
        if (player.isUsingItem()) {
            helper.fail("drawing with the action already spent must be refused");
        }
        finish(helper, player, husk);
        helper.succeed();
    }

    /** The off-turn hold lowers a bow still drawn when the turn passes, so it cannot be loosed late. */
    public static void aDrawHeldPastTheTurnIsLowered(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS_HOLDING_PLAYERS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, PAST_LONG_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        aimAt(player, husk);
        useMainHand(helper, player);
        if (!player.isUsingItem()) {
            helper.fail("drawing on your own turn must be allowed");
        }
        encounterOf(helper, player).turnOrder().endTurn();
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (player.isUsingItem()) {
                        helper.fail("a draw held past the turn must be lowered");
                    }
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /** 5e's ranged attack in melee: a hostile at the shooter's elbow imposes disadvantage. */
    public static void aHostileInMeleeReachImposesDisadvantage(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, IN_RANGE_Z);
        Husk atTheElbow = spawnTarget(helper, LANE_X + 1, 0);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST_OF_THREE, husk, atTheElbow);
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(NATURAL_HIT, NATURAL_HIT);
        loose(helper, player, husk);
        helper.startSequence()
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(1, () -> {
                    requireMode(helper, ROLLED.get(0), RollMode.DISADVANTAGE, "a hostile within melee reach");
                    if (dice.d20Draws != 2) {
                        helper.fail("disadvantage must roll two d20s but rolled " + dice.d20Draws);
                    }
                    finish(helper, player, husk, atTheElbow);
                })
                .thenSucceed();
    }

    public static void beyondNormalRangeImposesDisadvantage(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, PAST_NORMAL_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ScenarioSupport.forceD20Faces(NATURAL_HIT, NATURAL_HIT);
        loose(helper, player, husk);
        helper.startSequence()
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(1, () -> {
                    requireProjectileRoll(helper, ROLLED.get(0), player, husk);
                    requireMode(helper, ROLLED.get(0), RollMode.DISADVANTAGE, "a target past normal range");
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /** Past long range there is no attack at all: no roll, no damage, and the arrow carries on. */
    public static void beyondLongRangeThereIsNoShot(GameTestHelper helper) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, PAST_LONG_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(NATURAL_HIT, NATURAL_HIT);
        loose(helper, player, husk);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    List<AbstractArrow> arrows = arrowsAround(helper);
                    if (arrows.isEmpty() || arrows.get(0).getZ() <= husk.getZ()) {
                        helper.fail("the arrow must reach and pass the target");
                    }
                })
                .thenExecuteAfter(1, () -> {
                    if (!ROLLED.isEmpty() || dice.d20Draws != 0) {
                        helper.fail("a target past long range must not be rolled against");
                    }
                    if (husk.getHealth() < before) {
                        helper.fail("a shot past long range must deal no damage");
                    }
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /**
     * The cover plot: a one-block wall in front of the target hides its lower half from the shooter's
     * eye, the arrow, aimed high, clears the wall, and the half cover reaches Critfall as +2 AC.
     */
    public static void coverRaisesTheTargetsArmorClass(GameTestHelper helper) {
        prepare(helper, RANGED_FAR, COVER_DEFAULT, TURNS);
        for (int x = LANE_X - 1; x <= LANE_X + 1; x++) {
            helper.setBlock(new BlockPos(x, FEET_Y, WALL_Z), Blocks.STONE);
        }
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, PAST_NORMAL_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ScenarioSupport.forceD20Faces(NATURAL_HIT, NATURAL_HIT);
        loose(helper, player, husk);
        helper.startSequence()
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(1, () -> {
                    AttackResult result = ROLLED.get(0).result();
                    if (result.defenderAcBonus() != 2) {
                        helper.fail("half cover must feed +2 to Critfall but fed +" + result.defenderAcBonus());
                    }
                    if (result.armorClass() != result.baseArmorClass() + 2) {
                        helper.fail("half cover must raise the target's AC by 2");
                    }
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /** Off, a shot between participants is what it always was: canceled, unrolled, and no action spent. */
    public static void rangedAttacksOffLeaveTheShotCanceled(GameTestHelper helper) {
        prepare(helper, RANGED_OFF, COVER_OFF, TURNS);
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        Husk husk = spawnTarget(helper, LANE_X, IN_RANGE_Z);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        float before = husk.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(NATURAL_HIT);
        loose(helper, player, husk);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    if (!ROLLED.isEmpty() || dice.d20Draws != 0) {
                        helper.fail("with ranged attacks off no shot may be rolled");
                    }
                    if (husk.getHealth() < before) {
                        helper.fail("with ranged attacks off a shot between participants must stay canceled");
                    }
                    if (!ActionEconomy.budgetOf(encounterOf(helper, player), player)
                            .actionAvailable()) {
                        helper.fail("with ranged attacks off firing must not spend the action");
                    }
                    finish(helper, player, husk);
                })
                .thenSucceed();
    }

    /** A skeleton's turn: one arrow, rolled by Critfall, and no second one while the turn lasts. */
    public static void aSkeletonShootsOnceOnItsTurn(GameTestHelper helper) {
        aMobShootsOnceOnItsTurn(helper, EntityType.SKELETON, Items.BOW);
    }

    /** A pillager's turn: it loads, fires one bolt, rolled by Critfall, and fires no more. */
    public static void aPillagerShootsOnceOnItsTurn(GameTestHelper helper) {
        aMobShootsOnceOnItsTurn(helper, EntityType.PILLAGER, Items.CROSSBOW);
    }

    /**
     * Mob turns don't end when spent here, so the turn stays the mob's after the shot and only the
     * spent action can keep it from shooting again. The target is two blocks out, in normal range and
     * out of reach, so one d20 is the whole roll.
     */
    private static void aMobShootsOnceOnItsTurn(GameTestHelper helper, EntityType<? extends Mob> type, Item weapon) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, LANE_X + 0.5, SHOOTER_Z);
        standOnTheFloor(helper, player);
        player.setNoGravity(true);
        Mob shooter = helper.spawn(type, new BlockPos(LANE_X, 1, IN_RANGE_Z));
        standOnTheFloor(helper, shooter);
        shooter.setNoAi(true);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
        ScenarioSupport.formEncounter(helper, player, MOB_FIRST, shooter);
        float before = player.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(NATURAL_HIT, NATURAL_HIT, NATURAL_HIT);
        helper.startSequence()
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    Rolled rolled = ROLLED.get(0);
                    requireProjectileRoll(helper, rolled, shooter, player);
                    if (ROLLED.size() != 1 || dice.d20Draws != 1) {
                        helper.fail("a mob must shoot exactly once on its turn but rolled " + dice.d20Draws + " d20s");
                    }
                    Encounter encounter = encounterOf(helper, player);
                    if (!shooter.getUUID()
                            .equals(encounter.turnOrder().currentTurn().orElse(null))) {
                        helper.fail("the scenario must still be on the mob's turn");
                    }
                    if (rolled.result().isHit() && player.getHealth() >= before) {
                        helper.fail("the mob's hit must land Critfall's damage");
                    }
                    finish(helper, null, shooter, player);
                })
                .thenSucceed();
    }

    /** The in-game bug: a mob's missed shot bounced off the player it missed instead of flying past. */
    public static void aSkeletonsMissFliesPastThePlayer(GameTestHelper helper) {
        aMobsMissFliesPastThePlayer(helper, EntityType.SKELETON, Items.BOW);
    }

    public static void aPillagersMissFliesPastThePlayer(GameTestHelper helper) {
        aMobsMissFliesPastThePlayer(helper, EntityType.PILLAGER, Items.CROSSBOW);
    }

    /**
     * The mob shoots from its own turn at a real server player, which is what it shoots at in game,
     * and the miss must carry the arrow on past the player, away from the shooter, exactly like a
     * player's missed arrow does.
     */
    private static void aMobsMissFliesPastThePlayer(
            GameTestHelper helper, EntityType<? extends Mob> type, Item weapon) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnShotAtPlayer(helper);
        Mob shooter = spawnRangedMob(helper, type, weapon);
        helper.startSequence()
                .thenExecuteAfter(SPAWN_PROTECTION_TICKS, () -> {
                    ScenarioSupport.formEncounter(helper, player, MOB_FIRST, shooter);
                    ScenarioSupport.forceD20Faces(NATURAL_MISS, NATURAL_MISS);
                })
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(2, () -> {
                    Rolled rolled = ROLLED.get(0);
                    requireProjectileRoll(helper, rolled, shooter, player);
                    if (rolled.result().outcome() != AttackOutcome.MISS) {
                        helper.fail("a natural " + NATURAL_MISS + " must miss but was "
                                + rolled.result().outcome());
                    }
                    requireFlewPast(helper, player);
                    finish(helper, player, shooter);
                })
                .thenSucceed();
    }

    public static void aSkeletonsArrowInFlightWhenTheFightFormsFliesPast(GameTestHelper helper) {
        aMobsArrowInFlightWhenTheFightFormsFliesPast(helper, EntityType.SKELETON, Items.BOW);
    }

    public static void aPillagersArrowInFlightWhenTheFightFormsFliesPast(GameTestHelper helper) {
        aMobsArrowInFlightWhenTheFightFormsFliesPast(helper, EntityType.PILLAGER, Items.CROSSBOW);
    }

    /**
     * A mob shoots in real time until it is in the fight, so its last arrow is often still in the air
     * when it joins. That arrow was fired as no ranged attack, so it reaches the player unrolled and
     * its hurt is canceled — and it must still fly on past the player rather than bounce off. The
     * player goes first, so the mob takes no turn and fires no rolled shot of its own meanwhile.
     */
    private static void aMobsArrowInFlightWhenTheFightFormsFliesPast(
            GameTestHelper helper, EntityType<? extends Mob> type, Item weapon) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        ServerPlayer player = spawnShotAtPlayer(helper);
        Mob shooter = spawnRangedMob(helper, type, weapon);
        float[] before = new float[1];
        helper.startSequence()
                .thenExecuteAfter(SPAWN_PROTECTION_TICKS, () -> {
                    fireUnscattered(helper, shooter, player);
                    ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, shooter);
                    before[0] = player.getHealth();
                })
                .thenWaitUntil(() -> {
                    List<AbstractArrow> arrows = arrowsAround(helper);
                    if (arrows.isEmpty() || arrows.get(0).getZ() >= player.getZ()) {
                        helper.fail("the arrow must reach and pass the player");
                    }
                })
                .thenExecuteAfter(2, () -> {
                    if (!ROLLED.isEmpty()) {
                        helper.fail("an arrow fired before the fight must not be rolled");
                    }
                    if (player.getHealth() < before[0]) {
                        helper.fail("an arrow fired before the fight must be canceled between participants");
                    }
                    requireFlewPast(helper, player);
                    finish(helper, player, shooter);
                })
                .thenSucceed();
    }

    public static void aSkeletonsMissOutsideAnEncounterFliesPast(GameTestHelper helper) {
        aMobsMissOutsideAnEncounterFliesPast(helper, EntityType.SKELETON, Items.BOW);
    }

    public static void aPillagersMissOutsideAnEncounterFliesPast(GameTestHelper helper) {
        aMobsMissOutsideAnEncounterFliesPast(helper, EntityType.PILLAGER, Items.CROSSBOW);
    }

    /**
     * With no encounter Initiative stands aside and the shot is Critfall's own automatic roll, so its
     * own missed-shot handling must carry the arrow past the player. The mob fires through its own
     * ranged attack, aimed without scatter so the arrow is sure to reach the player.
     */
    private static void aMobsMissOutsideAnEncounterFliesPast(
            GameTestHelper helper, EntityType<? extends Mob> type, Item weapon) {
        prepare(helper, RANGED, COVER_OFF, TURNS);
        InitiativeConfig.overrideEncountersForTesting(NO_ENCOUNTERS);
        ServerPlayer player = spawnShotAtPlayer(helper);
        Mob shooter = spawnRangedMob(helper, type, weapon);
        helper.startSequence()
                .thenExecuteAfter(SPAWN_PROTECTION_TICKS, () -> {
                    ScenarioSupport.forceD20Faces(NATURAL_MISS, NATURAL_MISS);
                    fireUnscattered(helper, shooter, player);
                })
                .thenWaitUntil(() -> requireRolled(helper, 1))
                .thenExecuteAfter(2, () -> {
                    Rolled rolled = ROLLED.get(0);
                    requireProjectileRoll(helper, rolled, shooter, player);
                    if (rolled.result().outcome() != AttackOutcome.MISS) {
                        helper.fail("a natural " + NATURAL_MISS + " must miss but was "
                                + rolled.result().outcome());
                    }
                    if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                            .isPresent()) {
                        helper.fail("with encounters off the shot must stay Critfall's own");
                    }
                    requireFlewPast(helper, player);
                    finish(helper, player, shooter);
                })
                .thenSucceed();
    }

    /** A survival server player, the way a player stands in game, at the shooter's end of the lane. */
    private static ServerPlayer spawnShotAtPlayer(GameTestHelper helper) {
        ServerPlayer player = spawnArcher(helper, Items.BOW);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static Mob spawnRangedMob(GameTestHelper helper, EntityType<? extends Mob> type, Item weapon) {
        Mob shooter = helper.spawn(type, new BlockPos(LANE_X, 1, IN_RANGE_Z));
        standOnTheFloor(helper, shooter);
        shooter.setNoAi(true);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
        return shooter;
    }

    /** The mob's own ranged attack: a pillager loads and fires without scatter, a skeleton's arrow is re-aimed true. */
    private static void fireUnscattered(GameTestHelper helper, Mob shooter, LivingEntity target) {
        if (shooter instanceof CrossbowAttackMob && shooter.getMainHandItem().getItem() instanceof CrossbowItem item) {
            ItemStack crossbow = shooter.getMainHandItem();
            crossbow.releaseUsing(helper.getLevel(), shooter, 0);
            item.performShooting(
                    helper.getLevel(),
                    shooter,
                    InteractionHand.MAIN_HAND,
                    crossbow,
                    CrossbowItem.MOB_ARROW_POWER,
                    0.0F,
                    target);
            return;
        }
        ((RangedAttackMob) shooter).performRangedAttack(target, 1.0F);
        for (AbstractArrow arrow : arrowsAround(helper)) {
            double dx = target.getX() - arrow.getX();
            double dz = target.getZ() - arrow.getZ();
            double dy = target.getY(1.0 / 3.0) - arrow.getY() + Math.sqrt(dx * dx + dz * dz) * 0.2;
            arrow.shoot(dx, dy, dz, (float) arrow.getDeltaMovement().length(), 0.0F);
        }
    }

    /**
     * The mob shoots toward lower z, so an arrow that flew on is behind the player and one that
     * bounced off is back on the shooter's side.
     */
    private static void requireFlewPast(GameTestHelper helper, LivingEntity target) {
        List<AbstractArrow> arrows = arrowsAround(helper);
        if (arrows.size() != 1) {
            helper.fail("the one missed arrow must still be flying but found " + arrows.size());
        }
        if (arrows.get(0).getZ() >= target.getZ()) {
            helper.fail("a missed arrow must fly on past the player, not bounce back toward the shooter");
        }
    }

    private static ActionConfig actions(boolean ranged, boolean mobEndTurnWhenSpent, double normal, double far) {
        return new ActionConfig(
                true,
                6.0,
                true,
                true,
                true,
                REACH,
                false,
                mobEndTurnWhenSpent,
                true,
                true,
                true,
                true,
                true,
                false,
                3.0,
                0,
                0,
                true,
                true,
                3.0,
                1.0,
                0,
                0,
                true,
                8.0,
                true,
                1.0,
                0,
                0,
                ranged,
                normal,
                far);
    }

    /** A real server player, so the draw goes through the loaders' use hooks; standing on the floor, aimed down the lane. */
    private static ServerPlayer spawnArcher(GameTestHelper helper, Item weapon) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 spawn = helper.absoluteVec(new Vec3(LANE_X + 0.5, FEET_Y, SHOOTER_Z));
        player.moveTo(spawn.x, spawn.y, spawn.z, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon));
        player.getInventory().add(new ItemStack(Items.ARROW, 16));
        return player;
    }

    private static void enchantWithFlame(GameTestHelper helper, ItemStack bow) {
        bow.enchant(
                helper.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FLAME),
                1);
    }

    private static Husk spawnTarget(GameTestHelper helper, int x, int z) {
        Husk husk = ScenarioSupport.spawnHusk(helper, x, z);
        standOnTheFloor(helper, husk);
        return husk;
    }

    /** No AI, a mock server player and no gravity all keep an entity exactly where this puts it. */
    private static void standOnTheFloor(GameTestHelper helper, LivingEntity entity) {
        double feetY = helper.absoluteVec(new Vec3(0, FEET_Y, 0)).y;
        entity.moveTo(entity.getX(), feetY, entity.getZ(), entity.getYRot(), entity.getXRot());
    }

    /** Three quarters up the target, which clears a one-block wall in front of it and stays under the ceiling. */
    private static void aimAt(ServerPlayer player, LivingEntity target) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getY(0.75), target.getZ()));
    }

    /** Draws through the server's use path and lets go at full draw, which is what a full-length hold amounts to. */
    private static void loose(GameTestHelper helper, ServerPlayer player, LivingEntity target) {
        aimAt(player, target);
        useMainHand(helper, player);
        if (!player.isUsingItem()) {
            helper.fail("drawing on your own turn with your action left must be allowed");
        }
        player.getUseItem().releaseUsing(helper.getLevel(), player, 0);
        player.stopUsingItem();
    }

    private static void useMainHand(GameTestHelper helper, ServerPlayer player) {
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    private static void requireRolled(GameTestHelper helper, int count) {
        if (ROLLED.size() < count) {
            helper.fail("waiting for " + count + " ranged roll(s)");
        }
    }

    private static void requireProjectileRoll(
            GameTestHelper helper, Rolled rolled, LivingEntity attacker, LivingEntity target) {
        if (rolled.attacker() != attacker || rolled.target() != target) {
            helper.fail("the roll must be the shooter's against the participant its arrow reached");
        }
        if (rolled.delivery() != AttackDelivery.PROJECTILE) {
            helper.fail("an arrow must roll as a projectile but rolled as " + rolled.delivery());
        }
    }

    private static void requireMode(GameTestHelper helper, Rolled rolled, RollMode expected, String why) {
        if (rolled.result().roll().mode() != expected) {
            helper.fail(why + " must roll with " + expected + " but rolled "
                    + rolled.result().roll().mode());
        }
    }

    private static List<AbstractArrow> arrowsAround(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(AbstractArrow.class, sweep(helper));
    }

    private static AABB sweep(GameTestHelper helper) {
        return AABB.ofSize(helper.absoluteVec(new Vec3(2, 1, 2)), 40, 40, 40);
    }

    private static Encounter encounterOf(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, ActionConfig actions, CoverConfig cover, TurnConfig turns) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(turns);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(cover);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        for (int x = LANE_X - 1; x <= LANE_X + 1; x++) {
            helper.setBlock(new BlockPos(x, FEET_Y, WALL_Z), Blocks.AIR);
        }
        ScenarioSupport.discardLeftoverMobsNearby(helper);
        arrowsAround(helper).forEach(AbstractArrow::discard);
        ROLLED.clear();
        recording = true;
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, LivingEntity... others) {
        recording = false;
        RollService.resetRoller();
        arrowsAround(helper).forEach(AbstractArrow::discard);
        if (player != null) {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        for (LivingEntity entity : others) {
            entity.discard();
        }
    }
}
