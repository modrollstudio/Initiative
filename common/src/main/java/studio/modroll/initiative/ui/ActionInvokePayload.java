package studio.modroll.initiative.ui;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.Initiative;

/**
 * A click on an action button. Carries only what the player pointed at — the server resolves it and
 * runs the very same {@code ActionRegistry.invoke} the command does, so nothing here is trusted.
 */
public record ActionInvokePayload(ResourceLocation actionId, int targetEntityId, Optional<Vec3> position)
        implements CustomPacketPayload {

    public static final int NO_TARGET = -1;

    public static final CustomPacketPayload.Type<ActionInvokePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "action_invoke"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    public static final StreamCodec<FriendlyByteBuf, ActionInvokePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ActionInvokePayload::actionId,
            ByteBufCodecs.VAR_INT,
            ActionInvokePayload::targetEntityId,
            ByteBufCodecs.optional(VEC3),
            ActionInvokePayload::position,
            ActionInvokePayload::new);

    public static ActionInvokePayload untargeted(ResourceLocation actionId) {
        return new ActionInvokePayload(actionId, NO_TARGET, Optional.empty());
    }

    public static ActionInvokePayload at(ResourceLocation actionId, Vec3 position) {
        return new ActionInvokePayload(actionId, NO_TARGET, Optional.of(position));
    }

    public static ActionInvokePayload on(ResourceLocation actionId, int targetEntityId) {
        return new ActionInvokePayload(actionId, targetEntityId, Optional.empty());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
