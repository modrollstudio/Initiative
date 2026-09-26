package studio.modroll.initiative.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.initiative.Initiative;

/** Server to client: one player's action bar for this turn, sent only when it changes. */
public record ActionUiPayload(ActionUiSnapshot snapshot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActionUiPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "action_ui"));

    public static final StreamCodec<FriendlyByteBuf, ActionUiPayload> STREAM_CODEC =
            ActionUiSnapshot.STREAM_CODEC.map(ActionUiPayload::new, ActionUiPayload::snapshot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
