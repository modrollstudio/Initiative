package studio.modroll.initiative.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.initiative.action.Concealment;
import studio.modroll.initiative.encounter.Provocation;

/**
 * Refuses a hidden target, and reports the neutral→hostile transition. {@code Mob.setTarget} is the
 * single funnel every anger and target-acquisition goal ends in, so watching it catches a panda
 * fighting back, a wolf pack turning and an iron golem picking a side alike. A mixin is required
 * because Fabric has no change-target event, where NeoForge fires {@code LivingChangeTargetEvent}
 * from this same method; cancelling the HEAD inject leaves the target field untouched, exactly as
 * cancelling that event does. Brain-driven mobs never call this method and NeoForge's event covers
 * them where Fabric's mixin cannot — see {@code Concealment}.
 */
@Mixin(Mob.class)
public abstract class MobTargetMixin {

    @Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void initiative$refuseHiddenOrReportProvocation(LivingEntity target, CallbackInfo ci) {
        if (Concealment.suppressesTarget(target)) {
            ci.cancel();
            return;
        }
        Provocation.mobTargeted((Mob) (Object) this, target);
    }
}
