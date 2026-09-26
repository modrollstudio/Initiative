package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.BuiltinActions;
import studio.modroll.initiative.action.NativeActions;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ActionUiConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.GrappleConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.ui.ActionUiSnapshot;
import studio.modroll.initiative.ui.ActionUiSnapshots;

/**
 * The unarmed grapple and the escape from it. A grapple is a condition with a lifetime, so these
 * scenarios care as much about how a hold ends — escaped, broken, dropped when a side leaves — as
 * about how it lands.
 */
public final class GrappleScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 40.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, false, true, true, true, true, true, true, false, 3.0, 0, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    private static final GrappleConfig GRAPPLE = new GrappleConfig(true, 3.0, 0, 0, 5.0, true, 0, 0);
    private static final GrappleConfig GRAPPLE_OFF = new GrappleConfig(false, 3.0, 0, 0, 5.0, true, 0, 0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> PLAYER_FIRST_TWO_HOSTILES = List.of(20, 5, 1);
    private static final List<Integer> ACTOR_WINS = List.of(15, 5);
    private static final List<Integer> ACTOR_LOSES = List.of(5, 15);

    private GrappleScenarios() {}

    public static void grappleWinHoldsTheTarget(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        contest(ACTOR_WINS, () -> {
            NativeActions.GrappleAttempt attempt = NativeActions.grapple(helper.getLevel(), player, husk);
            if (attempt.status() != ActionEconomy.ActionOutcome.PERFORMED || !attempt.grappled()) {
                helper.fail("a won grapple must take hold but was " + attempt.status() + "/" + attempt.grappled());
            }
        });
        if (!encounter.flags().isGrappled(husk.getUUID())) {
            helper.fail("a won grapple must leave the target grappled");
        }
        if (!player.getUUID().equals(encounter.flags().grapplerOf(husk.getUUID()))) {
            helper.fail("the hold must record who is holding");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("a grapple spends the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void grappleLossLeavesTheTargetFree(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        contest(ACTOR_LOSES, () -> {
            NativeActions.GrappleAttempt attempt = NativeActions.grapple(helper.getLevel(), player, husk);
            if (attempt.status() != ActionEconomy.ActionOutcome.PERFORMED || attempt.grappled()) {
                helper.fail("a lost grapple must not take hold but was " + attempt.status());
            }
        });
        if (encounter.flags().isGrappled(husk.getUUID())) {
            helper.fail("a lost grapple must leave the target free");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("a lost grapple still spends the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /** The hold is made with a hand, so a full grip cannot make one — and nothing is rolled. */
    public static void grappleWithBothHandsFullIsRejected(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_SWORD));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        require(helper, grapple(helper, player, husk), ActionStatus.HANDS_FULL, "a grapple with no free hand");
        if (encounter(helper, player).flags().isGrappled(husk.getUUID())) {
            helper.fail("a rejected grapple must not take hold");
        }
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionEconomy.ActionOutcome.PERFORMED) {
            helper.fail("a rejected grapple must leave the turn's action intact");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void disabledGrappleIsRejected(GameTestHelper helper) {
        prepare(helper, GRAPPLE_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        require(helper, grapple(helper, player, husk), ActionStatus.DISABLED, "a disabled grapple");
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionEconomy.ActionOutcome.PERFORMED) {
            helper.fail("disabling the grapple must leave the other actions working");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /** Speed 0: a held participant's turn begins with no movement to spend. */
    public static void grappledTargetHasNoMovementOnItsTurn(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        contest(ACTOR_WINS, () -> NativeActions.grapple(helper.getLevel(), player, husk));
        if (ActionEconomy.budgetOf(encounter, player.getUUID()).movementRemaining() <= 0.0) {
            helper.fail("the grappler keeps its own movement");
        }
        encounter.turnOrder().endTurn();
        if (ActionEconomy.budgetOf(encounter, husk.getUUID()).movementRemaining() != 0.0) {
            helper.fail("a grappled participant's turn must start with no movement");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /** Escape exists only for someone who is held — the UI never offers it to anyone else. */
    public static void escapeIsOnlyOfferedWhileGrappled(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        if (bar(helper, encounter, player).entry(BuiltinActions.ESCAPE).isPresent()) {
            helper.fail("a free participant must not be offered Escape");
        }
        require(helper, escape(helper, player), ActionStatus.DISABLED, "an escape with nothing holding you");
        encounter.flags().setGrappled(player.getUUID(), husk.getUUID());
        if (bar(helper, encounter, player).entry(BuiltinActions.ESCAPE).isEmpty()) {
            helper.fail("a held participant must be offered Escape");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void escapeWinClearsTheHoldAndReturnsMovement(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        encounter.flags().setGrappled(player.getUUID(), husk.getUUID());
        double heldMovement =
                ActionEconomy.budgetOf(encounter, player.getUUID()).movementRemaining();
        contest(ACTOR_WINS, () -> {
            NativeActions.EscapeAttempt attempt = NativeActions.escape(helper.getLevel(), player);
            if (attempt.status() != ActionEconomy.ActionOutcome.PERFORMED || !attempt.escaped()) {
                helper.fail("a won escape must break the hold but was " + attempt.status());
            }
        });
        if (encounter.flags().isGrappled(player.getUUID())) {
            helper.fail("a won escape must clear the condition");
        }
        if (encounter.budget().movementRemaining() <= heldMovement) {
            helper.fail("breaking free must hand the movement back");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("an escape spends the action");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    public static void escapeLossKeepsTheHoldAndSpendsTheAction(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        encounter.flags().setGrappled(player.getUUID(), husk.getUUID());
        double heldMovement =
                ActionEconomy.budgetOf(encounter, player.getUUID()).movementRemaining();
        contest(ACTOR_LOSES, () -> {
            NativeActions.EscapeAttempt attempt = NativeActions.escape(helper.getLevel(), player);
            if (attempt.status() != ActionEconomy.ActionOutcome.PERFORMED || attempt.escaped()) {
                helper.fail("a lost escape must still be performed but was " + attempt.status());
            }
        });
        if (!encounter.flags().isGrappled(player.getUUID())) {
            helper.fail("a lost escape must leave the hold in place");
        }
        if (encounter.budget().actionAvailable()) {
            helper.fail("a lost escape still spends the action");
        }
        if (encounter.budget().movementRemaining() != heldMovement) {
            helper.fail("a failed escape must not hand movement back");
        }
        cleanUp(player, husk);
        helper.succeed();
    }

    /** No leaked conditions: the grappler leaving the encounter frees whoever it held. */
    public static void aGrapplerLeavingFreesItsTarget(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk grappler = ScenarioSupport.spawnHusk(helper, 3, 1);
        Husk other = ScenarioSupport.spawnHusk(helper, 4, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST_TWO_HOSTILES, grappler, other);
        Encounter encounter = encounter(helper, player);
        encounter.flags().setGrappled(player.getUUID(), grappler.getUUID());
        helper.startSequence()
                .thenExecute(grappler::kill)
                .thenExecuteAfter(3, () -> {
                    if (encounter.flags().isGrappled(player.getUUID())) {
                        helper.fail("a dead grappler must not keep holding anyone");
                    }
                    cleanUp(player, other);
                })
                .thenSucceed();
    }

    /** Nothing holds across a room: knockback, a blink or a shove all break the hold on their own. */
    public static void aHoldBreaksBeyondItsDistance(GameTestHelper helper) {
        prepare(helper, GRAPPLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounter(helper, player);
        contest(ACTOR_WINS, () -> NativeActions.grapple(helper.getLevel(), player, husk));
        helper.startSequence()
                .thenExecute(() -> {
                    Vec3 far = helper.absoluteVec(new Vec3(8, 1, 1));
                    husk.moveTo(far.x, far.y, far.z, 0, 0);
                })
                .thenExecuteAfter(3, () -> {
                    if (encounter.flags().isGrappled(husk.getUUID())) {
                        helper.fail("a hold must break once the two are further apart than it reaches");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    private static ActionUiSnapshot bar(GameTestHelper helper, Encounter encounter, Player player) {
        return ActionUiSnapshots.build(helper.getLevel(), encounter, player);
    }

    private static ActionResult grapple(GameTestHelper helper, Player player, LivingEntity target) {
        return ActionRegistry.invoke(helper.getLevel(), player, BuiltinActions.GRAPPLE, ActionRequest.of(target));
    }

    private static ActionResult escape(GameTestHelper helper, Player player) {
        return ActionRegistry.invoke(helper.getLevel(), player, BuiltinActions.ESCAPE, ActionRequest.none());
    }

    private static void contest(List<Integer> faces, Runnable action) {
        ScenarioSupport.forceD20Faces(faces.get(0), faces.get(1));
        try {
            action.run();
        } finally {
            RollService.resetRoller();
        }
    }

    private static void require(GameTestHelper helper, ActionResult result, ActionStatus expected, String what) {
        if (result.status() != expected) {
            helper.fail(what + " must be " + expected + " but was " + result.status());
        }
    }

    private static Encounter encounter(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must exist while the grapple is inspected");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, GrappleConfig grapple) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideGrappleForTesting(grapple);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideActionUiForTesting(new ActionUiConfig(true));
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
