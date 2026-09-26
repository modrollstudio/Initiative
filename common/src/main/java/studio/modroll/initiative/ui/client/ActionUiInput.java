package studio.modroll.initiative.ui.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.ui.ActionInvokePayload;
import studio.modroll.initiative.ui.ActionUiClientCache;
import studio.modroll.initiative.ui.ActionUiSnapshot;

/**
 * The client half of a button press. An action that needs nothing pointed at it goes straight to
 * the server; one that declares a target puts the player into targeting mode, where the next attack
 * click confirms and a use click cancels. Which mode an action enters is read from its declared
 * targeting, so a pack's action targets like a built-in without being known here.
 */
public final class ActionUiInput {

    public interface Sender {
        void send(ActionInvokePayload payload);
    }

    private static final double PICK_RANGE = 32.0;

    private static Sender sender = payload -> {};
    private static ResourceLocation pendingAction;
    private static ActionUiSnapshot.TargetKind pendingKind;
    private static List<Integer> pendingTargets = List.of();

    private ActionUiInput() {}

    /** Loader client wiring installs the real network send here. */
    public static void setSender(Sender networkSender) {
        sender = networkSender;
    }

    public static void openPanel() {
        if (ActionUiClientCache.current().isPresent()) {
            Minecraft.getInstance().setScreen(new ActionPanelScreen());
        }
    }

    /** A click on a button: run it now, or start pointing it somewhere. */
    public static void choose(ActionUiSnapshot.Entry entry) {
        if (!entry.isAvailable()) {
            return;
        }
        Minecraft.getInstance().setScreen(null);
        switch (entry.targeting()) {
            case NONE, SELF -> sender.send(ActionInvokePayload.untargeted(entry.id()));
            case ENTITY, POSITION -> {
                pendingAction = entry.id();
                pendingKind = entry.targeting();
                pendingTargets = entry.validTargetIds();
            }
        }
    }

    public static boolean isTargeting() {
        return pendingAction != null;
    }

    public static void cancel() {
        pendingAction = null;
        pendingKind = null;
        pendingTargets = List.of();
    }

    /** Consumes the targeting clicks before vanilla sees them, so aiming never swings or places. */
    public static void tick(Minecraft minecraft) {
        if (!isTargeting()) {
            return;
        }
        if (ActionUiClientCache.current().isEmpty() || minecraft.player == null) {
            cancel();
            return;
        }
        boolean confirm = false;
        while (minecraft.options.keyAttack.consumeClick()) {
            confirm = true;
        }
        boolean abort = false;
        while (minecraft.options.keyUse.consumeClick()) {
            abort = true;
        }
        if (confirm) {
            confirm(minecraft);
        } else if (abort) {
            cancel();
        }
    }

    /** What the prompt above the bar says while the player is aiming. */
    public static Component targetingHint() {
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingKind == ActionUiSnapshot.TargetKind.POSITION) {
            return Component.translatable("initiative.ui.pick_position", ActionBarRenderer.label(pendingAction));
        }
        Entity target = pickEntity(minecraft);
        if (target == null) {
            return Component.translatable("initiative.ui.pick_target", ActionBarRenderer.label(pendingAction));
        }
        return Component.translatable("initiative.ui.target_ready", target.getDisplayName())
                .withStyle(ChatFormatting.GREEN);
    }

    private static void confirm(Minecraft minecraft) {
        ActionInvokePayload payload = pendingKind == ActionUiSnapshot.TargetKind.POSITION
                ? atLookedAtBlock(minecraft)
                : atLookedAtEntity(minecraft);
        if (payload == null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("initiative.action.invalid_target").withStyle(ChatFormatting.RED), true);
            return;
        }
        sender.send(payload);
        cancel();
    }

    private static ActionInvokePayload atLookedAtEntity(Minecraft minecraft) {
        Entity target = pickEntity(minecraft);
        return target == null ? null : ActionInvokePayload.on(pendingAction, target.getId());
    }

    private static ActionInvokePayload atLookedAtBlock(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult block) || hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        return ActionInvokePayload.at(
                pendingAction, Vec3.atCenterOf(block.getBlockPos().above()));
    }

    /** Only the server's valid targets are pickable, so an aim that lands is one the gate accepts. */
    private static Entity pickEntity(Minecraft minecraft) {
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 end = eye.add(minecraft.player.getViewVector(1.0f).scale(PICK_RANGE));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                minecraft.player,
                eye,
                end,
                new AABB(eye, end).inflate(1.0),
                entity -> pendingTargets.contains(entity.getId()),
                PICK_RANGE * PICK_RANGE);
        return hit == null ? null : hit.getEntity();
    }
}
