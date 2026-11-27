package studio.modroll.initiative.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.AttackContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.api.Encounters;
import studio.modroll.initiative.api.Turns;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.ui.ActionUiSnapshots;

/**
 * What 0.3.3 adds for a mod such as Critfall: Spells: an unlisted action, the gate's answer without performing, turn
 * listeners, a fight another mod starts, a teleport that costs nothing and an attack prepared to land later.
 */
public final class SpellSupportScenarios {

    private static final ResourceLocation STEP = ResourceLocation.fromNamespaceAndPath("initiativetest", "step");
    private static final ResourceLocation AIM = ResourceLocation.fromNamespaceAndPath("initiativetest", "aim");

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, true, 1.0, 30.0, true);
    private static final TurnConfig LONG_TURNS =
            new TurnConfig(true, 1000, 60, 20.0, 12, false, false, false, Set.of(), true, true);
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, true, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0, true, 24.0, 64.0, 20);

    private static final List<String> TURNS = new ArrayList<>();
    private static Vec3 stepTo = Vec3.ZERO;
    private static Optional<AttackContext> prepared = Optional.empty();

    static {
        ActionRegistry.register(Action.builder(STEP)
                .cost(ActionCost.BONUS_ACTION)
                .targeting(ActionTargeting.none())
                .listed(false)
                .effect(context -> {
                    context.teleport(context.actor(), stepTo);
                    return ActionResult.performed();
                })
                .build());
        ActionRegistry.register(Action.builder(AIM)
                .cost(ActionCost.FREE)
                .targeting(ActionTargeting.entity(ActionTargeting.Side.ENEMY))
                .listed(false)
                .effect(context -> {
                    prepared = context.prepareAttack(
                            context.target().orElseThrow(),
                            AttackContext.spell(context.level().damageSources().magic(), ItemStack.EMPTY));
                    return ActionResult.performed();
                })
                .build());
        Turns.onTurnStart(entity -> TURNS.add("start " + entity.getUUID()));
        Turns.onTurnEnd(entity -> TURNS.add("end " + entity.getUUID()));
    }

    private SpellSupportScenarios() {}

    /** An unlisted action has no button, the gate answers for it without performing, and its teleport costs nothing. */
    public static void anUnlistedActionChecksAndStepsForFree(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1.5, 1.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 6, 6);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Encounter encounter = encounter(helper, player);
        if (ActionUiSnapshots.build(helper.getLevel(), encounter, player).entries().stream()
                .anyMatch(entry -> entry.id().equals(STEP))) {
            helper.fail("an unlisted action must have no button");
        }
        Vec3 start = player.position();
        Optional<ActionStatus> before = check(helper, player);
        if (before.isPresent() || !player.position().equals(start)) {
            helper.fail("on its turn the check must allow the step and perform nothing, was " + before);
        }
        Optional<ActionStatus> offTurn = check(helper, husk);
        if (offTurn.orElse(null) != ActionStatus.NOT_YOUR_TURN) {
            helper.fail("off its turn the check must refuse, was " + offTurn);
        }
        double movement = ActionEconomy.budgetOf(encounter, player).movementRemaining();
        stepTo = start.add(2, 0, 0);
        ActionStatus stepped = ActionRegistry.invoke(
                        helper.getLevel(), player, STEP, ActionRequest.none(), ActionCost.BONUS_ACTION)
                .status();
        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    Vec3 at = player.position();
                    if (stepped != ActionStatus.PERFORMED || Math.hypot(at.x - stepTo.x, at.z - stepTo.z) > 0.01) {
                        helper.fail("the step must take the player there and keep it there, was " + stepped + " at "
                                + at + " for " + stepTo);
                    }
                    double left = ActionEconomy.budgetOf(encounter, player).movementRemaining();
                    if (Math.abs(left - movement) > 0.001) {
                        helper.fail("the step must cost no movement: " + movement + " before, " + left + " after");
                    }
                    if (check(helper, player).orElse(null) != ActionStatus.NO_BONUS_ACTION) {
                        helper.fail("once the bonus action is spent the check must say so");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** The listeners hear a turn end, then the next one start, once the turn passes. */
    public static void turnListenersFollowTheTurns(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1.5, 1.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 6, 6);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        Encounter encounter = encounter(helper, player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    TURNS.clear();
                    encounter.turnOrder().endTurn();
                })
                .thenExecuteAfter(1, () -> {
                    List<String> heard = heard(player.getUUID(), husk.getUUID());
                    List<String> expected = List.of("end " + player.getUUID(), "start " + husk.getUUID());
                    if (!heard.equals(expected)) {
                        helper.fail("expected " + expected + ", heard " + heard);
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** A fight another mod starts takes in both, by the rules a vanilla hit follows. */
    public static void engageStartsAFight(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1.5, 1.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 6, 6);
        if (Encounters.inFight(player) || Encounters.inFight(husk)) {
            helper.fail("nobody is in a fight yet");
        }
        ScenarioSupport.forceNaturals(helper, List.of(20, 1), () -> Encounters.engage(player, husk));
        if (!Encounters.inFight(player) || !Encounters.inFight(husk)) {
            helper.fail("engaging must start a fight with both in it");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /** A prepared attack carries the fight's rules: against a dodging creature it rolls with disadvantage. */
    public static void aPreparedAttackCarriesTheFightsRules(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1.5, 1.5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 6, 6);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20), husk);
        Encounter encounter = encounter(helper, player);
        ActionEconomy.dodge(helper.getLevel(), husk);
        encounter.turnOrder().endTurn();
        prepared = Optional.empty();
        ActionRegistry.invoke(helper.getLevel(), player, AIM, ActionRequest.of(husk));
        RollMode mode = prepared.map(AttackContext::mode).orElse(null);
        if (mode != RollMode.DISADVANTAGE) {
            helper.fail("an attack on a dodging husk must roll with disadvantage, was " + mode);
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    private static Optional<ActionStatus> check(GameTestHelper helper, LivingEntity actor) {
        return ActionRegistry.check(helper.getLevel(), actor, STEP, ActionRequest.none(), ActionCost.BONUS_ACTION);
    }

    private static List<String> heard(UUID... participants) {
        List<String> ours = new ArrayList<>();
        for (String line : TURNS) {
            for (UUID participant : participants) {
                if (line.endsWith(participant.toString())) {
                    ours.add(line);
                }
            }
        }
        return ours;
    }

    private static Encounter encounter(GameTestHelper helper, Player player) {
        return EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElseThrow();
    }

    private static void prepare(GameTestHelper helper) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(LONG_TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
