package studio.modroll.initiative.roll;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.initiative.Initiative;

/** Server to client: a roll that has already resolved, for the client to replay as dice. */
public record RollAnimationPayload(RollAnimation animation) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RollAnimationPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "roll_animation"));

    public static final StreamCodec<FriendlyByteBuf, RollAnimationPayload> STREAM_CODEC =
            RollAnimation.STREAM_CODEC.map(RollAnimationPayload::new, RollAnimationPayload::animation);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
