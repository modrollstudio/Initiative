package studio.modroll.initiative.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.modroll.initiative.action.RangedAttacks;

/**
 * Hands every new entity to {@link RangedAttacks#admits} before it enters the world, so a
 * participant's shot is gated by its turn and its action. A bow, a crossbow, a thrown trident and a
 * mob's ranged attack all add their projectile through this one method. A mixin is required because
 * Fabric's entity-load event fires after the entity is in and cannot refuse it; NeoForge's
 * cancellable join event does the same job there.
 */
@Mixin(ServerLevel.class)
public abstract class ProjectileSpawnMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void initiative$gateProjectile(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!RangedAttacks.admits((ServerLevel) (Object) this, entity)) {
            cir.setReturnValue(false);
        }
    }
}
