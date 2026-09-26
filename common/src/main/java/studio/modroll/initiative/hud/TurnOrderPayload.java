package studio.modroll.initiative.hud;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.initiative.Initiative;

/** Server to client: the encounter's turn strip. An empty snapshot clears the bar. */
public record TurnOrderPayload(TurnOrderSnapshot snapshot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TurnOrderPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "turn_order"));

    public static final StreamCodec<FriendlyByteBuf, TurnOrderPayload> STREAM_CODEC =
            TurnOrderSnapshot.STREAM_CODEC.map(TurnOrderPayload::new, TurnOrderPayload::snapshot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
