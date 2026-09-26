package studio.modroll.initiative.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TurnOrderSnapshotTest {

    private static final UUID ENCOUNTER = UUID.fromString("7f3a2b10-9c4d-4e5f-8a6b-1c2d3e4f5a6b");

    private static TurnOrderSnapshot sample() {
        return new TurnOrderSnapshot(
                ENCOUNTER,
                List.of(
                        new TurnOrderSnapshot.Entry(42, "Husk", "minecraft:husk", false),
                        new TurnOrderSnapshot.Entry(7, "NightsHigh", "minecraft:player", true)),
                42,
                9);
    }

    @Test
    void theCountdownRidesOnEverySnapshot() {
        assertEquals(9, sample().secondsRemaining());
    }

    @Test
    void snapshotRoundTripsThroughStreamCodec() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TurnOrderSnapshot.STREAM_CODEC.encode(buf, sample());
        assertEquals(sample(), TurnOrderSnapshot.STREAM_CODEC.decode(buf));
    }

    @Test
    void emptySnapshotRoundTripsAndReportsEmpty() {
        TurnOrderSnapshot empty = TurnOrderSnapshot.empty(ENCOUNTER);
        assertTrue(empty.isEmpty());
        assertEquals(TurnOrderSnapshot.NO_ACTOR, empty.currentActorId());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TurnOrderSnapshot.STREAM_CODEC.encode(buf, empty);
        assertEquals(empty, TurnOrderSnapshot.STREAM_CODEC.decode(buf));
    }

    @Test
    void payloadRoundTripsThroughStreamCodec() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TurnOrderPayload.STREAM_CODEC.encode(buf, new TurnOrderPayload(sample()));
        assertEquals(sample(), TurnOrderPayload.STREAM_CODEC.decode(buf).snapshot());
    }

    @Test
    void payloadTypeIsNamespaced() {
        assertEquals("initiative:turn_order", TurnOrderPayload.TYPE.id().toString());
    }
}
