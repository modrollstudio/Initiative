package studio.modroll.initiative.fabric.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.modroll.initiative.turn.OffTurnRestriction;

/**
 * Vanilla scales the field of view by the player's movement speed, so the off-turn root — which
 * takes that speed to zero — would zoom the camera all the way in. A held player is standing still,
 * and standing still is exactly the unmodified view. Fabric has no FOV event (NeoForge drives the
 * same rule through {@code ComputeFovModifierEvent}), so this vanilla-side mixin is the hook.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class FieldOfViewMixin {

    @Inject(method = "getFieldOfViewModifier()F", at = @At("HEAD"), cancellable = true)
    private void initiative$keepTheViewWhileHeld(CallbackInfoReturnable<Float> cir) {
        if (OffTurnRestriction.isRooted((AbstractClientPlayer) (Object) this)) {
            cir.setReturnValue(1.0f);
        }
    }
}
