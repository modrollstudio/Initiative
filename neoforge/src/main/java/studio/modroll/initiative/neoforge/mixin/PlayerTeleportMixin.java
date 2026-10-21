package studio.modroll.initiative.neoforge.mixin;

import java.util.Set;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.initiative.action.Teleports;

/**
 * Hands every player teleport to {@link Teleports} before it happens. An ender pearl, chorus fruit,
 * {@code /tp} and any other mod's teleport all move a player through this one method, which sets the
 * server position and tells the client, so a HEAD inject that cancels leaves both where they were.
 * A mixin is required because no loader event covers every teleport: NeoForge fires one for a
 * pearl, a chorus fruit and the vanilla commands only, and Fabric has none at all. A teleport into
 * another dimension also arrives here, but by then the player is in the new level, where no
 * encounter holds it, so it is never refused halfway through.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerTeleportMixin {

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), cancellable = true)
    private void initiative$gateTeleport(
            double x, double y, double z, float yRot, float xRot, Set<RelativeMovement> relative, CallbackInfo ci) {
        if (Teleports.refuses(((ServerGamePacketListenerImpl) (Object) this).player, x, z)) {
            ci.cancel();
        }
    }
}
