package studio.modroll.initiative.fabric.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.initiative.action.StruckArrow;

/**
 * Exposes the half of an arrow hit that vanilla runs only after the hurt lands ({@link StruckArrow}).
 * Initiative cancels a participant's arrow hurt and drives a rolled one in its place, so a hit needs
 * its knockback and the arrow's own effects run by hand. Both are protected and overridden per arrow
 * (a tipped arrow, a spectral arrow), so a mixin calls them where nothing else can.
 *
 * <p>A burning arrow is the one effect that cannot be run from {@link #initiative$struck}: vanilla
 * sets the target alight before the hurt and, because the canceled hurt reads as a miss, puts the
 * target's fire back the way it was right after. So a hit is only remembered there, and the target
 * is set alight once vanilla has put its fire back — before the bounce Critfall's missed-shot
 * handling cancels, so neither depends on the other.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements StruckArrow {

    @Unique
    private Entity initiative$burning;

    @Shadow
    protected abstract void doKnockback(LivingEntity target, DamageSource source);

    @Shadow
    protected abstract void doPostHurtEffects(LivingEntity target);

    @Override
    public void initiative$struck(LivingEntity target, DamageSource source) {
        doKnockback(target, source);
        doPostHurtEffects(target);
        if (((AbstractArrow) (Object) this).isOnFire()) {
            initiative$burning = target;
        }
    }

    @Inject(
            method = "onHitEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/Entity;setRemainingFireTicks(I)V",
                            shift = At.Shift.AFTER))
    private void initiative$igniteStruck(EntityHitResult hit, CallbackInfo ci) {
        if (initiative$burning != null && initiative$burning == hit.getEntity()) {
            hit.getEntity().igniteForSeconds(5.0F);
        }
        initiative$burning = null;
    }
}
