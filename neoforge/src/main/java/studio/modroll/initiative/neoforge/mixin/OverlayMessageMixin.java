package studio.modroll.initiative.neoforge.mixin;

import net.minecraft.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.roll.RollReadoutHold;

/**
 * Holds an action-bar readout that lands while a roll is still tumbling, so the text cannot spell
 * out the result before the die settles; the held line is re-shown from the client HUD hook. There
 * is no event for "an overlay message was set" on either loader, and Critfall sends its readout in
 * the same tick the roll resolves, so this vanilla-side mixin is the only coordination point.
 */
@Mixin(Gui.class)
public abstract class OverlayMessageMixin {

    @Inject(
            method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void initiative$holdWhileRolling(Component message, boolean animateColor, CallbackInfo ci) {
        if (RollReadoutHold.hold(message, Util.getMillis(), InitiativeConfig.rollAnimation())) {
            ci.cancel();
        }
    }
}
