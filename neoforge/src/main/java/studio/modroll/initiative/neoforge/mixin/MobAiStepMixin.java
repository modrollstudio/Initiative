package studio.modroll.initiative.neoforge.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.initiative.turn.AiFreeze;

/**
 * Gates every mob's AI step on the turn-freeze state. {@code Mob.serverAiStep} is final in 1.21.1
 * and is the single funnel for goal selectors, brains ({@code customServerAiStep}), navigation and
 * the move/look/jump controls, so cancelling it holds any AI implementation — vanilla or modded —
 * while the rest of the entity tick (physics, effects, timers, damage) runs untouched. A mixin is
 * required because NeoForge has no AI-step event; a HEAD inject is the narrowest option and
 * coexists with other mods' injections into the same method.
 */
@Mixin(Mob.class)
public abstract class MobAiStepMixin {

    @Inject(method = "serverAiStep()V", at = @At("HEAD"), cancellable = true)
    private void initiative$holdWhileFrozen(CallbackInfo ci) {
        if (AiFreeze.holdIfFrozen((Mob) (Object) this)) {
            ci.cancel();
        }
    }
}
