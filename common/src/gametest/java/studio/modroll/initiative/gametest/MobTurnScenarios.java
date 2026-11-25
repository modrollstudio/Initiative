package studio.modroll.initiative.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.BuiltinActions;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.api.ActionTargeting.Side;
import studio.modroll.initiative.api.Attacks;
import studio.modroll.initiative.api.MobTurn;
import studio.modroll.initiative.api.MobTurns;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;

/**
 * A mob's turn beyond Initiative's one attack: several attacks, a mod's own turn handler taking its
 * own actions and an action as a bonus action, a reaction out of turn, and a throw. The husk stands
 * in the player's reach with no AI; a handler moves it by hand where a mod's goals would walk it.
 * Every die after the initiative rolls is a natural 1, so attacks miss and nobody dies.
 */
public final class MobTurnScenarios {

    private static final ResourceLocation STRIKE = ResourceLocation.fromNamespaceAndPath("initiativetest", "strike");
    private static final ResourceLocation SWING = ResourceLocation.fromNamespaceAndPath("initiativetest", "swing");
    private static final ResourceLocation FLING = ResourceLocation.fromNamespaceAndPath("initiativetest", "fling");
    private static final Vec3 FLING_MOTION = new Vec3(0.0, 0.1, 0.4);

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, false, 1.0, 30.0, true);
    private static final TurnConfig TURNS =
            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of(), true, true);
    private static final TurnConfig SHORT_MOB_TURNS =
            new TurnConfig(true, 1000, 30, 20.0, 12, false, false, false, Set.of(), true, true);
    private static final TurnConfig HANDLERS_OFF =
            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of(), false, true);
    private static final TurnConfig HOLDING_PLAYERS =
            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, true, Set.of(), true, true);
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, true, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0, true, 24.0, 64.0, 20);
    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> HUSK_FIRST = List.of(1, 20);

    private static final Set<UUID> TWO_ATTACKS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, MobTurn> HANDLERS = new ConcurrentHashMap<>();
    private static final Set<UUID> TEST_MOBS = ConcurrentHashMap.newKeySet();
    private static int strikes;

    static {
        Predicate<LivingEntity> testMob = actor -> TEST_MOBS.contains(actor.getUUID());
        Attacks.register(creature -> TWO_ATTACKS.contains(creature.getUUID()) ? 2 : 1);
        MobTurns.register(mob -> HANDLERS.containsKey(mob.getUUID()), (mob, turn) -> HANDLERS.get(mob.getUUID())
                .tick(mob, turn));
        ActionRegistry.register(Action.builder(STRIKE)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.entity(Side.ENEMY, () -> 4.0))
                .availableTo(testMob)
                .effect(context -> {
                    strikes++;
                    return ActionResult.performed();
                })
                .build());
        ActionRegistry.register(Action.builder(SWING)
                .cost(ActionCost.REACTION)
                .targeting(ActionTargeting.none())
                .availableTo(testMob)
                .effect(context -> ActionResult.performed())
                .build());
        ActionRegistry.register(Action.builder(FLING)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.entity(Side.ENEMY, () -> 4.0))
                .availableTo(testMob)
                .effect(context -> {
                    context.launch(context.target().orElseThrow(), FLING_MOTION);
                    return ActionResult.performed();
                })
                .build());
    }

    private MobTurnScenarios() {}

    public static void aMobWithTwoAttacksMakesBothOnItsTurn(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        player.setNoGravity(true);
        Husk husk = standingHusk(helper);
        TWO_ATTACKS.add(husk.getUUID());
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        List<Long> attacks = attacksBy(helper, husk);
        Encounter encounter = encounter(helper, player);
        ScenarioSupport.forceCountedDice(false);
        encounter.turnOrder().endTurn();
        helper.startSequence()
                .thenWaitUntil(() -> requireRound(helper, encounter, 2))
                .thenExecute(() -> {
                    if (attacks.size() != 2) {
                        helper.fail("a mob with two attacks must make two on its turn but made " + attacks.size());
                    }
                    long gap = attacks.get(1) - attacks.get(0);
                    if (gap < ACTIONS.mobAttackSpacingTicks()) {
                        helper.fail("a mob's attacks must be " + ACTIONS.mobAttackSpacingTicks()
                                + " ticks apart but were " + gap);
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void aMobWithOneAttackMakesOneAsBefore(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = standingHusk(helper);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        List<Long> attacks = attacksBy(helper, husk);
        Encounter encounter = encounter(helper, player);
        ScenarioSupport.forceCountedDice(false);
        encounter.turnOrder().endTurn();
        helper.startSequence()
                .thenWaitUntil(() -> requireRound(helper, encounter, 2))
                .thenExecute(() -> {
                    if (attacks.size() != 1) {
                        helper.fail("a mob with one attack must make one on its turn but made " + attacks.size());
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /**
     * The handler strikes, takes Disengage as its bonus action and walks out of the player's reach.
     * Each step goes through the gate, so spending what it already spent is refused.
     */
    public static void aHandlerTakesItsOwnActionsThroughTheGate(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = standingHusk(helper);
        TEST_MOBS.add(husk.getUUID());
        List<ActionStatus> statuses = new ArrayList<>();
        Vec3 start = husk.position();
        HANDLERS.put(husk.getUUID(), (mob, turn) -> {
            if (turn.ticks() == 0) {
                statuses.add(invoke(helper, mob, STRIKE, ActionRequest.of(player), null));
                return false;
            }
            if (turn.ticks() == 1) {
                statuses.add(
                        invoke(helper, mob, BuiltinActions.DISENGAGE, ActionRequest.none(), ActionCost.BONUS_ACTION));
                statuses.add(invoke(helper, mob, STRIKE, ActionRequest.of(player), null));
                statuses.add(invoke(helper, mob, BuiltinActions.DASH, ActionRequest.none(), ActionCost.BONUS_ACTION));
                return false;
            }
            mob.setPos(mob.getX() + 0.25, mob.getY(), mob.getZ());
            return start.distanceTo(mob.position()) >= 3.5;
        });
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        ScenarioSupport.forceCountedDice(false);
        strikes = 0;
        encounter.turnOrder().endTurn();
        helper.startSequence()
                .thenWaitUntil(() -> requireRound(helper, encounter, 2))
                .thenExecute(() -> {
                    List<ActionStatus> expected = List.of(
                            ActionStatus.PERFORMED,
                            ActionStatus.PERFORMED,
                            ActionStatus.NO_ACTION,
                            ActionStatus.NO_BONUS_ACTION);
                    if (!statuses.equals(expected)) {
                        helper.fail("strike, Disengage as a bonus action, then both spent: expected " + expected
                                + " but got " + statuses);
                    }
                    if (strikes != 1) {
                        helper.fail("the handler's own action must run once but ran " + strikes + " times");
                    }
                    if (!MobTurns.enabled()) {
                        helper.fail("MobTurns.enabled() must be true while a handler takes the turn");
                    }
                    if (!encounter.flags().hasReaction(player.getUUID())) {
                        helper.fail("a mob that disengaged must provoke no opportunity attack as it walks off");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void aHandlerThatNeverFinishesEndsOnTheTimeout(GameTestHelper helper) {
        prepare(helper, SHORT_MOB_TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = standingHusk(helper);
        int[] calls = new int[1];
        HANDLERS.put(husk.getUUID(), (mob, turn) -> {
            calls[0]++;
            return false;
        });
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        encounter.turnOrder().endTurn();
        helper.startSequence()
                .thenWaitUntil(() -> requireRound(helper, encounter, 2))
                .thenExecute(() -> {
                    if (calls[0] < SHORT_MOB_TURNS.mobTurnTimeoutTicks() - 1) {
                        helper.fail("a handler that never finishes must hold the turn to the timeout, but ran "
                                + calls[0] + " ticks");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void aHandlerThatThrowsEndsTheTurn(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = standingHusk(helper);
        int[] calls = new int[1];
        HANDLERS.put(husk.getUUID(), (mob, turn) -> {
            calls[0]++;
            throw new IllegalStateException("a broken handler, on purpose");
        });
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        encounter.turnOrder().endTurn();
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    requireRound(helper, encounter, 2);
                    if (calls[0] != 1) {
                        helper.fail("a handler that throws must end the turn at once but ran " + calls[0] + " times");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void withCustomMobTurnsOffTheMobTakesInitiativesTurn(GameTestHelper helper) {
        prepare(helper, HANDLERS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = standingHusk(helper);
        int[] calls = new int[1];
        HANDLERS.put(husk.getUUID(), (mob, turn) -> {
            calls[0]++;
            return true;
        });
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        List<Long> attacks = attacksBy(helper, husk);
        Encounter encounter = encounter(helper, player);
        ScenarioSupport.forceCountedDice(false);
        encounter.turnOrder().endTurn();
        helper.startSequence()
                .thenWaitUntil(() -> requireRound(helper, encounter, 2))
                .thenExecute(() -> {
                    if (calls[0] != 0) {
                        helper.fail("with custom_mob_turns off no handler may run, but one ran " + calls[0] + " times");
                    }
                    if (attacks.size() != 1) {
                        helper.fail("with custom_mob_turns off the mob must make Initiative's one attack, made "
                                + attacks.size());
                    }
                    if (MobTurns.enabled()) {
                        helper.fail("MobTurns.enabled() must be false with custom_mob_turns off");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** A mob's reaction, taken on the player's turn the way a mod's own tick would take it. */
    public static void aReactionOutOfTurnSpendsTheMobsReactionOnly(GameTestHelper helper) {
        prepare(helper, TURNS);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = standingHusk(helper);
        TEST_MOBS.add(husk.getUUID());
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        ActionStatus first = invoke(helper, husk, SWING, ActionRequest.none(), null);
        ActionStatus second = invoke(helper, husk, SWING, ActionRequest.none(), null);
        if (first != ActionStatus.PERFORMED || second != ActionStatus.NO_REACTION) {
            helper.fail("a reaction off turn must be taken once a round: got " + first + " then " + second);
        }
        if (encounter.flags().hasReaction(husk.getUUID())) {
            helper.fail("the reaction must spend the mob's reaction");
        }
        if (!player.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))
                || !ActionEconomy.budgetOf(encounter, player).actionAvailable()) {
            helper.fail("a mob's reaction must leave the player's turn alone");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /**
     * The husk throws the player on its own turn and holds the turn while the player flies and lands.
     * Lifted out of the floor layer, the player stands on NeoForge's plot floor and drops to the ground
     * on Fabric's, which has none; the throw waits until it stands, and is low enough to stay under
     * NeoForge's plot ceiling.
     */
    public static void aLaunchedPlayerFliesAndIsHeldWhereItLands(GameTestHelper helper) {
        prepare(helper, HOLDING_PLAYERS);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        player.setPos(player.getX(), player.getY() + 1.0, player.getZ());
        Husk husk = standingHusk(helper);
        TEST_MOBS.add(husk.getUUID());
        List<ActionStatus> flung = new ArrayList<>();
        Vec3[] start = new Vec3[1];
        HANDLERS.put(husk.getUUID(), (mob, turn) -> {
            if (flung.isEmpty() && player.onGround()) {
                start[0] = player.position();
                flung.add(invoke(helper, mob, FLING, ActionRequest.of(player), null));
            }
            return false;
        });
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        Vec3[] inTheAir = new Vec3[1];
        double[] landingStep = {-1.0};
        helper.onEachTick(() -> {
            if (start[0] == null || landingStep[0] >= 0.0) {
                return;
            }
            if (!player.onGround()) {
                inTheAir[0] = player.position();
            } else if (inTheAir[0] != null) {
                landingStep[0] = horizontal(inTheAir[0], player.position());
            }
        });
        Vec3[] landed = new Vec3[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (flung.isEmpty()) {
                        helper.fail("waiting for the player to stand, and the throw");
                    }
                })
                .thenExecuteAfter(2, () -> {
                    if (!flung.equals(List.of(ActionStatus.PERFORMED))) {
                        helper.fail("the throw must be performed on the husk's turn, got " + flung);
                    }
                    if (horizontal(start[0], player.position()) < 0.5) {
                        helper.fail("a thrown player off turn must not be pulled back mid flight");
                    }
                })
                .thenWaitUntil(() -> {
                    if (!player.onGround() || player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
                        helper.fail("waiting for the thrown player to land");
                    }
                    landed[0] = player.position();
                })
                .thenExecuteAfter(10, () -> {
                    if (landingStep[0] < 0.05) {
                        helper.fail("the step a thrown player lands with must stand, but it moved " + landingStep[0]);
                    }
                    if (horizontal(start[0], landed[0]) < 1.0) {
                        helper.fail("the throw must carry the player at least a block");
                    }
                    if (horizontal(landed[0], player.position()) > 0.05) {
                        helper.fail("a thrown player must be held where it landed, not pulled back");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /**
     * In the player's reach, and kept there between its attacks: the hit that forms the fight knocks
     * nothing back, and a husk with no AI stood in the floor layer would sink out of reach.
     */
    private static Husk standingHusk(GameTestHelper helper) {
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        husk.setNoGravity(true);
        return husk;
    }

    private static ActionStatus invoke(
            GameTestHelper helper, LivingEntity actor, ResourceLocation id, ActionRequest request, ActionCost as) {
        ActionResult result = as == null
                ? ActionRegistry.invoke(helper.getLevel(), actor, id, request)
                : ActionRegistry.invoke(helper.getLevel(), actor, id, request, as);
        return result.status();
    }

    /** The game tick of every attack roll this attacker shows. */
    private static List<Long> attacksBy(GameTestHelper helper, LivingEntity attacker) {
        String name = attacker.getDisplayName().getString();
        List<Long> ticks = new ArrayList<>();
        RollAnimationSync.overrideSenderForTesting((watcher, animation) -> {
            if (animation.kind() == RollAnimation.Kind.ATTACK
                    && animation.actor().roller().equals(name)) {
                ticks.add(helper.getTick());
            }
        });
        return ticks;
    }

    private static void requireRound(GameTestHelper helper, Encounter encounter, int round) {
        if (encounter.turnOrder().round() < round) {
            helper.fail("waiting for round " + round);
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
        TWO_ATTACKS.clear();
        HANDLERS.clear();
        TEST_MOBS.clear();
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
        TWO_ATTACKS.clear();
        HANDLERS.clear();
        TEST_MOBS.clear();
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
