package studio.modroll.initiative.action;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.ContestContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.initiative.action.ActionEconomy.ActionOutcome;
import studio.modroll.initiative.api.ActionContext;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.checks.ChecksBridge;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.GrappleConfig;
import studio.modroll.initiative.config.InitiativeConfig;

/**
 * The contested melee actions. Shove, Reel and Grapple all resolve through Critfall's
 * {@code RollService.contest} (actor is the initiator, target the opponent); with Checks active,
 * Shove, Grapple and Escape roll the participants' Athletics and Acrobatics through Checks instead
 * of the flat config bonuses. Shove and Reel apply vanilla knockback so falls, lava and cliffs
 * behave naturally, while Grapple applies a condition that holds the target in place until it
 * escapes or the hold breaks. They are ordinary registry
 * entries; the methods here are the callers' front door and narrow the registry status back down.
 */
public final class NativeActions {

    private NativeActions() {}

    public record ShoveAttempt(ActionOutcome status, boolean knockedBack) {}

    public record ReelAttempt(ActionOutcome status, boolean pulled) {}

    public record GrappleAttempt(ActionOutcome status, boolean grappled) {}

    public record EscapeAttempt(ActionOutcome status, boolean escaped) {}

    public static ShoveAttempt shove(ServerLevel level, LivingEntity actor, LivingEntity target) {
        ActionResult result = invoke(level, actor, BuiltinActions.SHOVE, ActionRequest.of(target));
        return new ShoveAttempt(ActionEconomy.outcome(result), result.succeeded());
    }

    public static ActionOutcome blink(ServerLevel level, LivingEntity actor) {
        return ActionEconomy.outcome(invoke(level, actor, BuiltinActions.BLINK, ActionRequest.none()));
    }

    public static ReelAttempt reel(ServerLevel level, LivingEntity actor, LivingEntity target) {
        ActionResult result = invoke(level, actor, BuiltinActions.REEL, ActionRequest.of(target));
        return new ReelAttempt(ActionEconomy.outcome(result), result.succeeded());
    }

    public static GrappleAttempt grapple(ServerLevel level, LivingEntity actor, LivingEntity target) {
        ActionResult result = invoke(level, actor, BuiltinActions.GRAPPLE, ActionRequest.of(target));
        return new GrappleAttempt(ActionEconomy.outcome(result), result.succeeded());
    }

    public static EscapeAttempt escape(ServerLevel level, LivingEntity actor) {
        ActionResult result = invoke(level, actor, BuiltinActions.ESCAPE, ActionRequest.none());
        return new EscapeAttempt(ActionEconomy.outcome(result), result.succeeded());
    }

    private static ActionResult invoke(
            ServerLevel level, LivingEntity actor, ResourceLocation id, ActionRequest request) {
        return ActionRegistry.invoke(level, actor, id, request);
    }

    /** Shove: contested roll; on a win knock the target back away from the shover, spending the action either way. */
    static ActionResult performShove(ActionContext context) {
        ActionConfig actions = InitiativeConfig.actions();
        LivingEntity target = context.target().orElseThrow();
        boolean won = athleticsContestWon(context, target, actions.shoveAttackerBonus(), actions.shoveDefenderBonus());
        if (won) {
            knockAwayFrom(target, context.actor().position(), actions.shoveKnockbackStrength());
        }
        return ActionResult.performed(won);
    }

    /** Ender-pearl blink: consume a pearl and teleport up to the configured distance along facing. */
    static ActionResult performBlink(ActionContext context) {
        ActionConfig actions = InitiativeConfig.actions();
        if (!consumeOne(context.actor(), Items.ENDER_PEARL)) {
            return ActionResult.rejected(ActionStatus.MISSING_ITEM);
        }
        teleportAlongFacing(context.actor(), actions.blinkMaxBlocks());
        return ActionResult.performed();
    }

    /** Reel: the fishing-rod pull. Contested roll; on a win the target is dragged toward the actor. */
    static ActionResult performReel(ActionContext context) {
        ActionConfig actions = InitiativeConfig.actions();
        LivingEntity actor = context.actor();
        LivingEntity target = context.target().orElseThrow();
        ItemStack rod = heldFishingRod(actor);
        if (rod.isEmpty()) {
            return ActionResult.rejected(ActionStatus.MISSING_ITEM);
        }
        boolean won = contestWon(context, target, actions.reelAttackerBonus(), actions.reelDefenderBonus());
        if (won) {
            pullToward(target, actor.position(), actions.reelPullStrength());
        }
        useDurability(rod);
        return ActionResult.performed(won);
    }

    /**
     * Grapple: the unarmed hold. A free hand and a won contest put the target in a grapple, which
     * leaves it no movement on its turn until it escapes or the hold breaks. The contest is spent
     * either way, like every other contested action.
     */
    static ActionResult performGrapple(ActionContext context) {
        GrappleConfig grapple = InitiativeConfig.grapple();
        LivingEntity actor = context.actor();
        LivingEntity target = context.target().orElseThrow();
        if (!hasFreeHand(actor)) {
            return ActionResult.rejected(ActionStatus.HANDS_FULL);
        }
        boolean won = athleticsContestWon(context, target, grapple.attackerBonus(), grapple.defenderBonus());
        if (won) {
            Grapples.hold(context.level(), target, actor);
        }
        return ActionResult.performed(won);
    }

    /**
     * Escape: the grappled participant's own contest against whoever holds it. A win frees it and
     * hands back the movement its turn started without, since its speed is its own again.
     */
    static ActionResult performEscape(ActionContext context) {
        GrappleConfig grapple = InitiativeConfig.grapple();
        LivingEntity actor = context.actor();
        LivingEntity grappler = Grapples.grapplerOf(context.level(), actor);
        if (grappler == null) {
            return ActionResult.rejected(ActionStatus.INVALID_TARGET);
        }
        boolean won = escapeContestWon(context, grappler, grapple);
        if (won) {
            Grapples.release(context.level(), actor);
            context.grantMovement(InitiativeConfig.actions().movementBudgetBlocks());
        }
        return ActionResult.performed(won);
    }

    /** The grapple needs one hand free to hold with; a full grip cannot take hold of anything. */
    private static boolean hasFreeHand(LivingEntity actor) {
        return actor.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                || actor.getItemInHand(InteractionHand.OFF_HAND).isEmpty();
    }

    private static boolean consumeOne(LivingEntity actor, Item item) {
        if (!(actor instanceof Player player)) {
            return false;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void teleportAlongFacing(LivingEntity actor, double maxBlocks) {
        Vec3 look = actor.getLookAngle();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 1.0e-4) {
            return; // looking straight up or down: no horizontal direction to step
        }
        double x = actor.getX() + (look.x / horizontal) * maxBlocks;
        double z = actor.getZ() + (look.z / horizontal) * maxBlocks;
        actor.teleportTo(x, actor.getY(), z);
    }

    private static ItemStack heldFishingRod(LivingEntity actor) {
        ItemStack main = actor.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.is(Items.FISHING_ROD)) {
            return main;
        }
        ItemStack off = actor.getItemInHand(InteractionHand.OFF_HAND);
        return off.is(Items.FISHING_ROD) ? off : ItemStack.EMPTY;
    }

    private static void useDurability(ItemStack rod) {
        if (rod.isDamageableItem()) {
            rod.setDamageValue(rod.getDamageValue() + 1);
        }
    }

    private static boolean contestWon(ActionContext context, LivingEntity target, int actorBonus, int defenderBonus) {
        return shownWon(
                context,
                target,
                RollService.contest(context.actor(), target, ContestContext.of(actorBonus, defenderBonus)));
    }

    private static boolean athleticsContestWon(
            ActionContext context, LivingEntity target, int actorBonus, int defenderBonus) {
        if (!ChecksIntegration.active()) {
            return contestWon(context, target, actorBonus, defenderBonus);
        }
        return shownWon(context, target, ChecksBridge.athleticsContest(context.actor(), target));
    }

    private static boolean escapeContestWon(ActionContext context, LivingEntity grappler, GrappleConfig grapple) {
        if (!ChecksIntegration.active()) {
            return contestWon(context, grappler, grapple.escapeAttackerBonus(), grapple.escapeDefenderBonus());
        }
        return shownWon(context, grappler, ChecksBridge.escapeContest(context.actor(), grappler));
    }

    private static boolean shownWon(ActionContext context, LivingEntity opponent, ContestResult result) {
        context.showRoll(result, opponent);
        return result.initiatorWins();
    }

    /** A Checks contest a check event listener canceled rolled nothing: it is lost, and no dice are shown. */
    private static boolean shownWon(ActionContext context, LivingEntity opponent, Optional<ContestResult> result) {
        return result.map(rolled -> shownWon(context, opponent, rolled)).orElse(false);
    }

    /** Vanilla knockback pushes the target away from {@code source}. */
    private static void knockAwayFrom(LivingEntity target, Vec3 source, double strength) {
        target.knockback(strength, source.x - target.getX(), source.z - target.getZ());
    }

    /** Pulling toward the actor is knockback away from the point beyond the target, opposite the actor. */
    private static void pullToward(LivingEntity target, Vec3 actor, double strength) {
        target.knockback(strength, target.getX() - actor.x, target.getZ() - actor.z);
    }
}
