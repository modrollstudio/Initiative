package studio.modroll.initiative.ui;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionStatus;

/**
 * Server-built view of what one player may do this turn: every registered action that exists for
 * them, whether it is available and why not, what it may be pointed at, and what is left of the
 * turn. The client renders this and nothing else — it decides nothing, which is what keeps a
 * mod-registered action working without a line of UI code.
 */
public record ActionUiSnapshot(
        boolean yourTurn, List<Entry> entries, Budget budget, int turnSecondsRemaining, int turnSecondsTotal) {

    /** What an action needs pointed at it, flattened for the wire. */
    public enum TargetKind {
        NONE,
        SELF,
        POSITION,
        ENTITY
    }

    public static final ActionUiSnapshot INACTIVE =
            new ActionUiSnapshot(false, List.of(), new Budget(false, false, false, 0.0, 0.0), 0, 0);

    public static final StreamCodec<FriendlyByteBuf, ActionUiSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ActionUiSnapshot::yourTurn,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ActionUiSnapshot::entries,
            Budget.STREAM_CODEC,
            ActionUiSnapshot::budget,
            ByteBufCodecs.VAR_INT,
            ActionUiSnapshot::turnSecondsRemaining,
            ByteBufCodecs.VAR_INT,
            ActionUiSnapshot::turnSecondsTotal,
            ActionUiSnapshot::new);

    public Optional<Entry> entry(ResourceLocation id) {
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    /** What is left of the turn's resources, for the budget readout. */
    public record Budget(
            boolean action, boolean bonusAction, boolean reaction, double movementRemaining, double movementBudget) {

        public static final StreamCodec<ByteBuf, Budget> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                Budget::action,
                ByteBufCodecs.BOOL,
                Budget::bonusAction,
                ByteBufCodecs.BOOL,
                Budget::reaction,
                ByteBufCodecs.DOUBLE,
                Budget::movementRemaining,
                ByteBufCodecs.DOUBLE,
                Budget::movementBudget,
                Budget::new);
    }

    /**
     * One button. {@code unavailable} is the very status {@code ActionRegistry.invoke} would answer
     * with, so the greyed reason and the refusal are always the same sentence.
     */
    public record Entry(
            ResourceLocation id,
            ActionCost cost,
            TargetKind targeting,
            Optional<ActionStatus> unavailable,
            List<Integer> validTargetIds) {

        private static final StreamCodec<ByteBuf, ActionCost> COST =
                ByteBufCodecs.idMapper(index -> ActionCost.values()[index], ActionCost::ordinal);
        private static final StreamCodec<ByteBuf, TargetKind> TARGETING =
                ByteBufCodecs.idMapper(index -> TargetKind.values()[index], TargetKind::ordinal);
        private static final StreamCodec<ByteBuf, ActionStatus> STATUS =
                ByteBufCodecs.idMapper(index -> ActionStatus.values()[index], ActionStatus::ordinal);

        public static final StreamCodec<FriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                Entry::id,
                COST,
                Entry::cost,
                TARGETING,
                Entry::targeting,
                ByteBufCodecs.optional(STATUS),
                Entry::unavailable,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),
                Entry::validTargetIds,
                Entry::new);

        public boolean isAvailable() {
            return unavailable.isEmpty();
        }
    }
}
