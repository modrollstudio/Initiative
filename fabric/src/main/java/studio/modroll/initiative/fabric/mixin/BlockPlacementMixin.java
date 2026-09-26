package studio.modroll.initiative.fabric.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.modroll.initiative.action.BlockPlacement;
import studio.modroll.initiative.turn.OffTurnRestriction;

/**
 * Charges a participant's movement budget for placing a block during their turn, and blocks the
 * placement outright with no movement left. A mixin is required because Fabric has no cancelable
 * server block-place event; {@code BlockItem.place} is the single funnel for player block placement,
 * so a HEAD inject is the narrowest hook and coexists with other mods' injections. NeoForge uses its
 * official {@code BlockEvent.EntityPlaceEvent} instead — both drive the same {@link BlockPlacement}.
 */
@Mixin(BlockItem.class)
public abstract class BlockPlacementMixin {

    @Inject(
            method =
                    "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void initiative$chargeMovement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof Player player)) {
            return;
        }
        if (OffTurnRestriction.preventsInteraction(level, player)) {
            OffTurnRestriction.sendRefusal(player);
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (!BlockPlacement.onPlaceAttempt(level, player)) {
            player.displayClientMessage(Component.translatable("initiative.action.no_movement_to_build"), true);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
