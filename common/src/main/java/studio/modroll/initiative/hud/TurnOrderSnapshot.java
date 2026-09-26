package studio.modroll.initiative.hud;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Immutable server-built view of one encounter's turn order for the HUD. The current actor is
 * keyed by entity network id, never by list index — removals shift indices but not ids. The
 * countdown rides along so every player watching the strip sees how long the acting one has left,
 * not only the player holding the turn.
 */
public record TurnOrderSnapshot(UUID encounterId, List<Entry> entries, int currentActorId, int secondsRemaining) {

    public static final int NO_ACTOR = -1;

    public static final StreamCodec<FriendlyByteBuf, TurnOrderSnapshot> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            TurnOrderSnapshot::encounterId,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            TurnOrderSnapshot::entries,
            ByteBufCodecs.VAR_INT,
            TurnOrderSnapshot::currentActorId,
            ByteBufCodecs.VAR_INT,
            TurnOrderSnapshot::secondsRemaining,
            TurnOrderSnapshot::new);

    public static TurnOrderSnapshot empty(UUID encounterId) {
        return new TurnOrderSnapshot(encounterId, List.of(), NO_ACTOR, 0);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public record Entry(int entityId, String displayName, String entityTypeId, boolean isPlayer) {

        public static final StreamCodec<FriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                Entry::entityId,
                ByteBufCodecs.STRING_UTF8,
                Entry::displayName,
                ByteBufCodecs.STRING_UTF8,
                Entry::entityTypeId,
                ByteBufCodecs.BOOL,
                Entry::isPlayer,
                Entry::new);
    }
}
