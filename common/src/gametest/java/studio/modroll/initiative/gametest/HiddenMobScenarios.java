package studio.modroll.initiative.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.modroll.critfall.api.AttackContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.api.ActionTargeting.Side;
import studio.modroll.initiative.api.MobTurn;
import studio.modroll.initiative.api.MobTurns;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ChecksConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.hud.TurnOrderSnapshot;
import studio.modroll.initiative.hud.TurnOrderSnapshots;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;
import studio.modroll.initiative.turn.InitiativeEntry;
import studio.modroll.initiative.ui.ActionUiSnapshot;
import studio.modroll.initiative.ui.ActionUiSnapshots;

/**
 * Mobs that start a fight hidden: a mod's hider joining one, and an ambush starting one. Husks stand
 * in without Checks; with Checks a spider (Stealth +4) hides from the player (passive Perception 10),
 * so a scripted Stealth die decides whether it is found. Initiative dice are scripted in join order.
 */
public final class HiddenMobScenarios {

    private static final boolean WITHOUT_CHECKS = Boolean.getBoolean("initiative.gametest.withoutChecks");
    private static final ResourceLocation POUNCE = ResourceLocation.fromNamespaceAndPath("initiativetest", "pounce");

    /** Wide enough that the hider standing off to the side is swept into the fight as it forms. */
    private static final EncounterConfig WIDE_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, false, false, 6.0, 30.0, true);

    private static final EncounterConfig NO_SURPRISE =
            new EncounterConfig(true, true, true, false, true, true, false, false, 6.0, 30.0, false);
    private static final EncounterConfig ENCOUNTERS_OFF =
            new EncounterConfig(false, true, true, false, true, true, false, false, 6.0, 30.0, true);
    private static final TurnConfig TURNS =
            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of(), true, true);
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, true, true, 4.0, false, true, true, true, true, true, true, false, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0, true, 24.0, 64.0, 20);
    private static final ChecksConfig CHECKS_ON = new ChecksConfig(true, true, true);

    private static final Set<UUID> HIDERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, MobTurn> HANDLERS = new ConcurrentHashMap<>();
    private static final List<AttackResult> POUNCES = new ArrayList<>();

    static {
        MobTurns.joinsHidden(mob -> HIDERS.contains(mob.getUUID()));
        MobTurns.register(mob -> HANDLERS.containsKey(mob.getUUID()), (mob, turn) -> HANDLERS.get(mob.getUUID())
                .tick(mob, turn));
        ActionRegistry.register(Action.builder(POUNCE)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.entity(Side.ENEMY, () -> 4.0))
                .availableTo(actor -> HIDERS.contains(actor.getUUID()))
                .effect(context -> {
                    LivingEntity target = context.target().orElseThrow();
                    AttackResult result = context.attack(
                            target,
                            AttackContext.melee(
                                    context.level().damageSources().mobAttack(context.actor()), ItemStack.EMPTY));
                    POUNCES.add(result);
                    return ActionResult.attacked(result);
                })
                .build());
    }

    private HiddenMobScenarios() {}

    /**
     * The hider is swept into the player's fight with the husk and comes in hidden: off the strip,
     * off the acting marker and off the player's targets, until its own attack, with advantage, ends it.
     */
    public static void aHiddenJoinerIsKeptFromThePlayersUntilItAttacks(GameTestHelper helper) {
        prepare(helper, WIDE_BUBBLE, ScenarioSupport.CHECKS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 3);
        Husk hider = ScenarioSupport.spawnHusk(helper, 2, 1);
        HIDERS.add(hider.getUUID());
        HANDLERS.put(hider.getUUID(), (mob, turn) -> {
            ActionRegistry.invoke(helper.getLevel(), mob, POUNCE, ActionRequest.of(player));
            return true;
        });
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1, 10), husk);
        Encounter encounter = encounter(helper, player);
        if (!encounter.contains(hider.getUUID()) || !MobTurns.hidden(hider)) {
            helper.fail("the hider must be swept into the fight, and come in hidden");
        }
        TurnOrderSnapshot strip = TurnOrderSnapshots.build(helper.getLevel(), encounter);
        if (strip.entries().stream().anyMatch(entry -> entry.entityId() == hider.getId())) {
            helper.fail("a hidden mob must be left off the players' turn strip");
        }
        if (strip.entries().stream().noneMatch(entry -> entry.entityId() == husk.getId())) {
            helper.fail("a mob in the open must stay on the turn strip");
        }
        List<Integer> targets = attackTargets(helper, encounter, player);
        if (targets.contains(hider.getId()) || !targets.contains(husk.getId())) {
            helper.fail("the player's targets must leave out the hidden mob only, but were " + targets);
        }
        ScenarioSupport.forceCountedDice(false);
        int[] actorOnHisTurn = {Integer.MIN_VALUE};
        helper.startSequence()
                .thenExecute(() -> encounter.turnOrder().endTurn())
                .thenExecute(() -> actorOnHisTurn[0] = actingId(helper, encounter, hider))
                .thenWaitUntil(() -> {
                    if (POUNCES.isEmpty()) {
                        helper.fail("waiting for the hider's turn");
                    }
                })
                .thenExecute(() -> {
                    if (actorOnHisTurn[0] != TurnOrderSnapshot.NO_ACTOR) {
                        helper.fail("nobody may be shown acting on a hidden mob's turn");
                    }
                    AttackResult pounce = POUNCES.getFirst();
                    if (pounce.roll().mode() != RollMode.ADVANTAGE
                            || !pounce.roll().hasTwoDice()) {
                        helper.fail("an attack from hiding must roll two dice with advantage, rolled " + pounce.roll());
                    }
                    if (MobTurns.hidden(hider)) {
                        helper.fail("attacking must end the hider's hiding");
                    }
                    cleanUp(player, husk, hider);
                })
                .thenSucceed();
    }

    /** The player can still swing at where the hidden mob stands: with disadvantage, and a hit finds it. */
    public static void aHitFindsAHiddenMob(GameTestHelper helper) {
        prepare(helper, WIDE_BUBBLE, ScenarioSupport.CHECKS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 3);
        Husk hider = ScenarioSupport.spawnHusk(helper, 2, 1);
        HIDERS.add(hider.getUUID());
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1, 1), husk);
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(20, 20);
        try {
            ActionEconomy.AttackAttempt swing = ActionEconomy.attack(helper.getLevel(), player, hider);
            ScenarioSupport.requireAttack(helper, swing.status(), AttackStatus.PERFORMED, "a swing at the hidden mob");
            if (dice.d20Draws != 2 || swing.result().orElseThrow().roll().mode() != RollMode.DISADVANTAGE) {
                helper.fail("a swing at a hidden mob must roll with disadvantage");
            }
        } finally {
            RollService.resetRoller();
        }
        if (MobTurns.hidden(hider)) {
            helper.fail("a hit must find the hidden mob");
        }
        cleanUp(player, husk, hider);
        helper.succeed();
    }

    /** With Checks a hider that rolled low on Stealth is found by the player's passive Perception on the player's turn. */
    public static void withChecksAKeenEyeFindsAHiddenMob(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 3);
        Mob lowRoller = spawnStill(helper, EntityType.SPIDER, 2, 1);
        HIDERS.add(lowRoller.getUUID());
        // Player, husk, then the spider's Stealth (1 + 4, under the player's 10) and its initiative.
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1, 1, 1), husk);
        if (!MobTurns.hidden(lowRoller)) {
            helper.fail("the spider must join hidden");
        }
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (MobTurns.hidden(lowRoller)) {
                        helper.fail("a Stealth of 5 must be found by a passive Perception of 10 on the player's turn");
                    }
                    cleanUp(player, husk, lowRoller);
                })
                .thenSucceed();
    }

    /** The husk springs the fight: it joins hidden and the player's initiative keeps the lower of two d20s. */
    public static void anAmbushCatchesThePlayerBySurprise(GameTestHelper helper) {
        prepare(helper, WIDE_BUBBLE, ScenarioSupport.CHECKS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        List<RollAnimation> initiative = initiativeRolls();
        boolean[] started = new boolean[1];
        ScenarioSupport.forceNaturals(helper, List.of(10, 15, 4), () -> started[0] = MobTurns.ambush(husk, player));
        if (!started[0]) {
            helper.fail("an ambush with encounters on must start a fight");
        }
        Encounter encounter = encounter(helper, player);
        if (!MobTurns.hidden(husk)) {
            helper.fail("the ambusher must join hidden");
        }
        requireInitiative(helper, encounter, player, 4, "a surprised player keeps the lower d20");
        RollAnimation shown = initiative.getLast();
        if (shown.actor().detail().mode() != RollMode.DISADVANTAGE
                || !shown.actor().detail().hasTwoDice()) {
            helper.fail("a surprised player's initiative must show both dice, showed "
                    + shown.actor().detail());
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void withSurpriseOffAnAmbushRollsOneDie(GameTestHelper helper) {
        prepare(helper, NO_SURPRISE, ScenarioSupport.CHECKS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.forceNaturals(helper, List.of(10, 15), () -> MobTurns.ambush(husk, player));
        requireInitiative(helper, encounter(helper, player), player, 15, "with surprise off, one d20");
        cleanUp(player, husk);
        helper.succeed();
    }

    /** With Checks, a player whose passive Perception (10) meets the spider's Stealth (1 + 4) isn't surprised. */
    public static void withChecksASharpPlayerIsNotSurprised(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 2, 1);
        ScenarioSupport.forceNaturals(helper, List.of(1, 10, 15), () -> MobTurns.ambush(spider, player));
        requireInitiative(helper, encounter(helper, player), player, 15, "a player who saw it coming");
        cleanUp(player, spider);
        helper.succeed();
    }

    public static void anAmbushWithEncountersOffStartsNothing(GameTestHelper helper) {
        prepare(helper, ENCOUNTERS_OFF, ScenarioSupport.CHECKS_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 2.0, 2.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        if (MobTurns.ambush(husk, player)
                || EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                        .isPresent()) {
            helper.fail("with encounters off an ambush must start nothing and say so");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /** The natural the player's initiative kept, read back from its total. */
    private static void requireInitiative(
            GameTestHelper helper, Encounter encounter, Player player, int natural, String what) {
        InitiativeEntry entry = encounter.turnOrder().order().stream()
                .filter(one -> one.participant().equals(player.getUUID()))
                .findFirst()
                .orElseThrow();
        if (entry.total() - entry.bonus() != natural) {
            helper.fail(what + ": the player's initiative die must be " + natural + " but was "
                    + (entry.total() - entry.bonus()));
        }
    }

    private static List<Integer> attackTargets(GameTestHelper helper, Encounter encounter, Player player) {
        ActionUiSnapshot bar = ActionUiSnapshots.build(helper.getLevel(), encounter, player);
        return bar.entries().stream()
                .filter(entry -> entry.id().getPath().equals("attack"))
                .findFirst()
                .orElseThrow()
                .validTargetIds();
    }

    private static int actingId(GameTestHelper helper, Encounter encounter, Mob hider) {
        if (!hider.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the hider's turn must come after the player's");
        }
        return TurnOrderSnapshots.build(helper.getLevel(), encounter).currentActorId();
    }

    private static List<RollAnimation> initiativeRolls() {
        List<RollAnimation> rolls = new ArrayList<>();
        RollAnimationSync.overrideSenderForTesting((watcher, animation) -> {
            if (animation.kind() == RollAnimation.Kind.INITIATIVE) {
                rolls.add(animation);
            }
        });
        return rolls;
    }

    private static <T extends Mob> T spawnStill(GameTestHelper helper, EntityType<T> type, int x, int z) {
        T mob = helper.spawn(type, new BlockPos(x, 1, z));
        mob.setNoAi(true);
        return mob;
    }

    private static Encounter encounter(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, EncounterConfig encounters, ChecksConfig checks) {
        RollService.resetRoller();
        RollAnimationSync.clearSenderOverrideForTesting();
        HIDERS.clear();
        HANDLERS.clear();
        POUNCES.clear();
        InitiativeConfig.overrideEncountersForTesting(encounters);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideRollAnimationForTesting(ScenarioSupport.ANIMATION_ON);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideChecksForTesting(checks);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
        if (WITHOUT_CHECKS && ChecksIntegration.present()) {
            helper.fail("a run started with -PwithoutChecks must not have Checks installed");
        }
    }

    /** In a run without Checks this passes the scenario untested and answers false. */
    private static boolean prepareWithChecks(GameTestHelper helper) {
        prepare(helper, WIDE_BUBBLE, CHECKS_ON);
        if (WITHOUT_CHECKS) {
            helper.succeed();
            return false;
        }
        if (!ChecksIntegration.present()) {
            helper.fail("the GameTest runs must have Checks installed, or be started with -PwithoutChecks");
        }
        return true;
    }

    private static void cleanUp(LivingEntity... entities) {
        RollService.resetRoller();
        RollAnimationSync.clearSenderOverrideForTesting();
        HIDERS.clear();
        HANDLERS.clear();
        POUNCES.clear();
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
