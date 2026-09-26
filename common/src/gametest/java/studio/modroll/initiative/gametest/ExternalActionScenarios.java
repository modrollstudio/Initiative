package studio.modroll.initiative.gametest;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import studio.modroll.critfall.api.ContestContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionSettings;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.api.ActionTargeting.Side;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.RollAnimationConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.gametest.RollAnimationScenarios.CapturingSender;
import studio.modroll.initiative.roll.RollAnimation.Kind;
import studio.modroll.initiative.roll.RollAnimationSync;

/**
 * The platform proof: an action registered the way a third-party mod would register it — foreign
 * namespace, datapack-supplied toggle, no Initiative code path of its own — is gated, targeted,
 * charged and animated exactly like a built-in.
 */
public final class ExternalActionScenarios {

    private static final ResourceLocation MARK = ResourceLocation.fromNamespaceAndPath("initiativetest", "mark");

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    private static final RollAnimationConfig ANIMATION_ON = new RollAnimationConfig(true, 10, 10, 2, true, true);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> HUSK_FIRST = List.of(1, 20);
    private static final List<Integer> ALLY_LAST = List.of(20, 5, 1);

    private static int marks;

    static {
        ActionRegistry.register(Action.builder(MARK)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.entity(Side.ENEMY))
                .enabled(() -> ActionSettings.enabled(MARK, true))
                .effect(ExternalActionScenarios::mark)
                .build());
    }

    private ExternalActionScenarios() {}

    /** A mod's effect: drive a Critfall roll, present it through the context, report whether it landed. */
    private static ActionResult mark(studio.modroll.initiative.api.ActionContext context) {
        marks++;
        LivingEntity target = context.target().orElseThrow();
        ContestResult result = RollService.contest(context.actor(), target, ContestContext.of(0, 0));
        context.showRoll(result, target);
        return ActionResult.performed(result.initiatorWins());
    }

    public static void externalActionIsRejectedOutOfTurn(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        int before = marks;
        require(helper, invoke(helper, player, husk), ActionStatus.NOT_YOUR_TURN, "an out-of-turn external action");
        if (marks != before) {
            helper.fail("a rejected external action must not run its effect");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void externalActionRunsAndSpendsTheAction(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        int before = marks;
        ScenarioSupport.forceD20Faces(15, 5);
        try {
            require(helper, invoke(helper, player, husk), ActionStatus.PERFORMED, "an external action on your turn");
        } finally {
            RollService.resetRoller();
        }
        if (marks != before + 1) {
            helper.fail("the external action must run its effect exactly once");
        }
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionEconomy.ActionOutcome.NO_ACTION) {
            helper.fail("the external action must spend the turn's action like any built-in");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void externalActionRejectsAFriendlyTarget(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player ally = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, player, ally, husk);
        int before = marks;
        require(
                helper,
                ActionRegistry.invoke(helper.getLevel(), player, MARK, ActionRequest.of(ally)),
                ActionStatus.INVALID_TARGET,
                "an enemy-targeting action aimed at an ally");
        if (marks != before) {
            helper.fail("a bad target must be refused before the effect runs");
        }
        cleanUp(player, ally, husk);
        helper.succeed();
    }

    /** The datapack toggle is the same seam a built-in's config flag uses: off, and only it is gone. */
    public static void disabledExternalActionIsRejected(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        JsonObject off = new JsonObject();
        off.addProperty("enabled", false);
        ActionSettings.replaceAll(Map.of(MARK, off));
        int before = marks;
        try {
            require(helper, invoke(helper, player, husk), ActionStatus.DISABLED, "a disabled external action");
        } finally {
            ActionSettings.clearForTesting();
        }
        if (marks != before) {
            helper.fail("a disabled external action must not run its effect");
        }
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionEconomy.ActionOutcome.PERFORMED) {
            helper.fail("disabling one action must leave the others working");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void externalActionRollAnimatesLikeABuiltin(GameTestHelper helper) {
        prepare(helper);
        CapturingSender sender = new CapturingSender();
        RollAnimationSync.overrideSenderForTesting(sender);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ScenarioSupport.forceD20Faces(15, 5);
        try {
            require(helper, invoke(helper, player, husk), ActionStatus.PERFORMED, "the animated external action");
        } finally {
            RollService.resetRoller();
        }
        if (sender.lastFor(helper, player).kind() != Kind.CONTEST) {
            helper.fail("an external action's roll must animate like a built-in's");
        }
        RollAnimationSync.clearSenderOverrideForTesting();
        cleanUp(player, husk);
        helper.succeed();
    }

    private static ActionResult invoke(GameTestHelper helper, Player actor, LivingEntity target) {
        return ActionRegistry.invoke(helper.getLevel(), actor, MARK, ActionRequest.of(target));
    }

    private static void require(GameTestHelper helper, ActionResult result, ActionStatus expected, String what) {
        if (result.status() != expected) {
            helper.fail(what + " must be " + expected + " but was " + result.status());
        }
    }

    private static void prepare(GameTestHelper helper) {
        RollService.resetRoller();
        ActionSettings.clearForTesting();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideRollAnimationForTesting(ANIMATION_ON);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
