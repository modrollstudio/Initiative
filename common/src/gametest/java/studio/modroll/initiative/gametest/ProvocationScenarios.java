package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.turn.AiFreeze;

/**
 * Neutral mobs that turn hostile. A panda carries the plain target-acquisition case (it is not a
 * {@code NeutralMob}; its hurt-by-target goal simply picks a target), a wolf the persistent-anger
 * case, and a cow the passive control that must stay in real time however hard it is farmed.
 * Freeze stays off: membership and the turn order are what these prove.
 */
public final class ProvocationScenarios {

    private static final EncounterConfig PROVOKED = new EncounterConfig(true, true, true, true, true, true, 6.0, 30.0);
    private static final EncounterConfig PROVOKED_OFF =
            new EncounterConfig(true, true, true, false, true, true, 6.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final TurnConfig TURNS_FREEZING =
            new TurnConfig(true, 1000, 1000, 20.0, 12, true, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    /** Drawn in join order (player, then the mob), so (20, 1) puts the player's turn first. */
    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);

    private static final int PROVOCATION_TICKS = 10;

    /**
     * A mob hurt on its very first tick stamps the hurt at tick 0, which is where its hurt-by-target
     * goal starts too — so the goal reads it as already handled and the mob never fights back. Let
     * it tick before hitting it, the way a mob a player walked up to has.
     */
    private static final int SETTLE_TICKS = 2;

    private ProvocationScenarios() {}

    /**
     * The bug this exists for: the hit itself lands in real time, exactly as it always has, and the
     * encounter forms right after off the panda's own decision to fight back.
     */
    public static void hittingANeutralMobStartsAnEncounter(GameTestHelper helper) {
        prepare(helper, PROVOKED, ScenarioSupport.ACTIONS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Panda panda = helper.spawn(EntityType.PANDA, new BlockPos(3, 1, 3));
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> landOneHit(helper, player, panda))
                .thenExecuteAfter(PROVOCATION_TICKS, () -> {
                    Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), panda.getUUID())
                            .orElse(null);
                    if (encounter == null) {
                        helper.fail("provoking a neutral mob must form an encounter");
                    }
                    if (!encounter.contains(player.getUUID())) {
                        helper.fail("the player it turned on must be in the encounter");
                    }
                    if (!RollService.isSuppressed(panda) || !RollService.isSuppressed(player)) {
                        helper.fail("both participants must carry the suppression flag");
                    }
                    cleanUp(player, panda);
                })
                .thenSucceed();
    }

    /** Once in, a former neutral is an ordinary participant: turn order, driven d20, damage. */
    public static void aProvokedNeutralFightsThroughTheTurnOrder(GameTestHelper helper) {
        prepare(helper, PROVOKED, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Panda panda = spawnHeldPanda(helper, 3, 3);
        ScenarioSupport.forceD20Faces(20, 1);
        panda.setTarget(player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    RollService.resetRoller();
                    Encounter encounter = encounterOf(helper, panda);
                    if (!player.getUUID()
                            .equals(encounter.turnOrder().currentTurn().orElse(null))) {
                        helper.fail("the natural 20 must give the player the first turn");
                    }
                    driveOneAttack(helper, player, panda);
                    cleanUp(player, panda);
                })
                .thenSucceed();
    }

    /** Farming stays real time: a cow has no target selector, so it never turns on anyone. */
    public static void farmingAPassiveMobStartsNothing(GameTestHelper helper) {
        prepare(helper, PROVOKED, ScenarioSupport.ACTIONS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 1, 3));
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> landOneHit(helper, player, cow))
                .thenExecuteAfter(PROVOCATION_TICKS, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), cow.getUUID())
                            .isPresent()) {
                        helper.fail("a mob that never turns hostile must not be pulled into initiative");
                    }
                    if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                            .isPresent()) {
                        helper.fail("hitting livestock must not start an encounter");
                    }
                    if (RollService.isSuppressed(cow) || RollService.isSuppressed(player)) {
                        helper.fail("farming must leave Critfall's real-time pipeline alone");
                    }
                    cleanUp(player, cow);
                })
                .thenSucceed();
    }

    public static void aProvokedNeutralJoinsTheFightAlreadyRunning(GameTestHelper helper) {
        prepare(helper, PROVOKED, ScenarioSupport.ACTIONS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Panda panda = spawnHeldPanda(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter running = encounterOf(helper, player);
        panda.setTarget(player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    Encounter joined = encounterOf(helper, panda);
                    if (joined != running) {
                        helper.fail("a provoked neutral must join the fight already running, not open a second one");
                    }
                    cleanUp(player, husk, panda);
                })
                .thenSucceed();
    }

    /**
     * The fight must stay open to the mobs not in it yet. A neutral bystander is not a participant,
     * so the targeting gate would refuse the very swing that provokes it — and since it never turns
     * hostile on its own, that is a deadlock no hostile mob has (those are pulled in on sight).
     */
    public static void attackingAnotherNeutralPullsItIntoTheFight(GameTestHelper helper) {
        prepare(helper, PROVOKED, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Panda fighting = spawnHeldPanda(helper, 2, 2);
        Panda bystander = helper.spawn(EntityType.PANDA, new BlockPos(3, 1, 3));
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    ScenarioSupport.forceD20Faces(20, 1);
                    fighting.setTarget(player);
                })
                .thenExecuteAfter(1, () -> {
                    RollService.resetRoller();
                    encounterOf(helper, player);
                    ScenarioSupport.requireAttack(
                            helper,
                            ActionEconomy.attack(helper.getLevel(), player, bystander)
                                    .status(),
                            AttackStatus.TARGET_OUTSIDE_ENCOUNTER,
                            "a swing at an unprovoked neutral");
                    landOneHit(helper, player, bystander);
                })
                .thenExecuteAfter(PROVOCATION_TICKS, () -> {
                    if (!encounterOf(helper, player).contains(bystander.getUUID())) {
                        helper.fail("the swing must provoke the second neutral into the same fight; health="
                                + bystander.getHealth() + " lastHurtBy=" + bystander.getLastHurtByMob() + " target="
                                + bystander.getTarget() + " dist="
                                + Math.sqrt(bystander.distanceToSqr(player)));
                    }
                    cleanUp(player, fighting, bystander);
                })
                .thenSucceed();
    }

    /** The same opening must not become a way of dragging livestock into a turn-based duel. */
    public static void livestockStaysHittableAndOutOfTheFight(GameTestHelper helper) {
        prepare(helper, PROVOKED, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Panda fighting = spawnHeldPanda(helper, 2, 2);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 1, 3));
        float[] before = new float[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    ScenarioSupport.forceD20Faces(20, 1);
                    fighting.setTarget(player);
                })
                .thenExecuteAfter(1, () -> {
                    RollService.resetRoller();
                    encounterOf(helper, player);
                    ScenarioSupport.requireAttack(
                            helper,
                            ActionEconomy.attack(helper.getLevel(), player, cow).status(),
                            AttackStatus.TARGET_OUTSIDE_ENCOUNTER,
                            "a swing at livestock");
                    before[0] = cow.getHealth();
                    landOneHit(helper, player, cow);
                })
                .thenExecuteAfter(PROVOCATION_TICKS, () -> {
                    if (cow.getHealth() >= before[0]) {
                        helper.fail("farming mid-fight must still land its damage in real time");
                    }
                    if (EncounterManager.encounterContaining(helper.getLevel(), cow.getUUID())
                            .isPresent()) {
                        helper.fail("livestock must never be pulled into the fight");
                    }
                    cleanUp(player, fighting, cow);
                })
                .thenSucceed();
    }

    /** With the flag off the gate is the pre-provocation one again: outsiders are not targets. */
    public static void disabledFlagStillRefusesAnOutsiderSwing(GameTestHelper helper) {
        prepare(helper, PROVOKED_OFF, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 1, 3));
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ScenarioSupport.requireAttack(
                helper,
                ActionEconomy.attack(helper.getLevel(), player, cow).status(),
                AttackStatus.INVALID_TARGET,
                "a swing at an outsider with the flag off");
        cleanUp(player, husk, cow);
        helper.succeed();
    }

    /** Calming down is a leave path like any other: nothing of the mob may outlive it. */
    public static void aNeutralThatCalmsDownLeavesCleanly(GameTestHelper helper) {
        prepare(helper, PROVOKED, ScenarioSupport.ACTIONS_OFF, TURNS_FREEZING);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Panda panda = spawnHeldPanda(helper, 3, 3);
        ScenarioSupport.forceD20Faces(20, 1);
        panda.setTarget(player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    RollService.resetRoller();
                    encounterOf(helper, panda);
                    if (!AiFreeze.isFrozen(panda.getUUID())) {
                        helper.fail("the panda must be frozen off-turn before the leave path is worth testing");
                    }
                    panda.setTarget(null);
                })
                .thenExecuteAfter(2, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), panda.getUUID())
                            .isPresent()) {
                        helper.fail("a neutral that stopped being hostile must leave the encounter");
                    }
                    if (RollService.isSuppressed(panda) || AiFreeze.isFrozen(panda.getUUID())) {
                        helper.fail("leaving must release the suppression flag and thaw the AI");
                    }
                    if (RollService.isSuppressed(player)) {
                        helper.fail("the encounter must end with its only hostile gone, releasing the player");
                    }
                    cleanUp(player, panda);
                })
                .thenSucceed();
    }

    /**
     * Anger outlives the target field: a wolf whose goal drops its target for a tick is still angry
     * at the player, and dropping it out of the fight there would flip it in and out every round.
     */
    public static void angerKeepsAWolfInTheFightWithoutATarget(GameTestHelper helper) {
        prepare(helper, PROVOKED, ScenarioSupport.ACTIONS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Wolf wolf = spawnHeldWolf(helper, 3, 3);
        wolf.setPersistentAngerTarget(player.getUUID());
        wolf.setRemainingPersistentAngerTime(200);
        wolf.setTarget(player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    encounterOf(helper, wolf);
                    wolf.setTarget(null);
                })
                .thenExecuteAfter(2, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), wolf.getUUID())
                            .isEmpty()) {
                        helper.fail("a still-angry wolf must stay in the encounter without a target");
                    }
                    cleanUp(player, wolf);
                })
                .thenSucceed();
    }

    public static void disabledFlagLeavesProvocationInRealTime(GameTestHelper helper) {
        prepare(helper, PROVOKED_OFF, ScenarioSupport.ACTIONS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Panda panda = spawnHeldPanda(helper, 3, 3);
        panda.setTarget(player);
        helper.startSequence()
                .thenExecuteAfter(PROVOCATION_TICKS, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), panda.getUUID())
                            .isPresent()) {
                        helper.fail("no encounter may form from provocation while the flag is off");
                    }
                    if (RollService.isSuppressed(panda) || RollService.isSuppressed(player)) {
                        helper.fail("nothing may be suppressed while the flag is off");
                    }
                    cleanUp(player, panda);
                })
                .thenSucceed();
    }

    /**
     * Forced-max dice so Critfall's real-time swing certainly connects, and i-frames cleared first:
     * a swing swallowed by either would never set the victim's last-hurt-by, and it is the mob's
     * reaction to being hit that is under test.
     */
    private static void landOneHit(GameTestHelper helper, Player attacker, LivingEntity victim) {
        ScenarioSupport.forceCountedDice(true);
        victim.invulnerableTime = 0;
        try {
            victim.hurt(helper.getLevel().damageSources().playerAttack(attacker), 1.0f);
        } finally {
            RollService.resetRoller();
        }
    }

    private static void driveOneAttack(GameTestHelper helper, Player player, LivingEntity target) {
        float before = target.getHealth();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceCountedDice(true);
        try {
            ActionEconomy.AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, target);
            ScenarioSupport.requireAttack(helper, attempt.status(), AttackStatus.PERFORMED, "the on-turn attack");
            AttackResult result = attempt.result().orElseThrow();
            if (dice.d20Draws != 1) {
                helper.fail("the exchange must be one driven d20 but drew " + dice.d20Draws);
            }
            if (result.damage() <= 0 || target.getHealth() >= before) {
                helper.fail("the driven hit must apply its damage (rolled " + result.damage() + ", health " + before
                        + " -> " + target.getHealth() + ")");
            }
        } finally {
            RollService.resetRoller();
        }
    }

    /** No AI, so the scenario alone decides what the mob is hostile to and for how long. */
    private static Panda spawnHeldPanda(GameTestHelper helper, int x, int z) {
        return held(helper.spawn(EntityType.PANDA, new BlockPos(x, 1, z)));
    }

    private static Wolf spawnHeldWolf(GameTestHelper helper, int x, int z) {
        return held(helper.spawn(EntityType.WOLF, new BlockPos(x, 1, z)));
    }

    private static <T extends Mob> T held(T mob) {
        mob.setNoAi(true);
        return mob;
    }

    private static Encounter encounterOf(GameTestHelper helper, LivingEntity entity) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), entity.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, EncounterConfig encounters, ActionConfig actions) {
        prepare(helper, encounters, actions, TURNS);
    }

    private static void prepare(
            GameTestHelper helper, EncounterConfig encounters, ActionConfig actions, TurnConfig turns) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(encounters);
        InitiativeConfig.overrideTurnsForTesting(turns);
        InitiativeConfig.overrideActionsForTesting(actions);
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
