package studio.modroll.initiative.roll;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * One resolved d20 roll, packaged for the client to replay as an animation. The value is already
 * decided and applied server-side — this is presentation only, and nothing in combat waits on it.
 * Every die comes from Critfall's {@link RollDetail}, so advantage and disadvantage carry both
 * naturals and the kept one; contests carry a roll per side, since Critfall has no contest readout.
 * Each side carries its roller's name: the whole encounter watches, so a die nobody at the exchange
 * threw still says whose it is.
 */
public record RollAnimation(Kind kind, Roll actor, Optional<Roll> opponent) {

    public enum Kind {
        ATTACK,
        CONTEST,
        INITIATIVE
    }

    public enum Emphasis {
        NONE,
        CRIT,
        FUMBLE
    }

    public record Roll(RollDetail detail, Emphasis emphasis, String roller) {}

    private static final int CRIT_FACE = 20;
    private static final int FUMBLE_FACE = 1;

    public static final StreamCodec<FriendlyByteBuf, RollAnimation> STREAM_CODEC = StreamCodec.of(
            (buf, animation) -> {
                buf.writeEnum(animation.kind());
                writeRoll(buf, animation.actor());
                buf.writeBoolean(animation.opponent().isPresent());
                animation.opponent().ifPresent(roll -> writeRoll(buf, roll));
            },
            buf -> {
                Kind kind = buf.readEnum(Kind.class);
                Roll actor = readRoll(buf);
                Optional<Roll> opponent = buf.readBoolean() ? Optional.of(readRoll(buf)) : Optional.empty();
                return new RollAnimation(kind, actor, opponent);
            });

    /** Attack emphasis follows Critfall's own outcome, so a widened crit range stays its call. */
    public static RollAnimation attack(AttackResult result, String attacker) {
        Emphasis emphasis =
                switch (result.outcome()) {
                    case CRIT -> Emphasis.CRIT;
                    case FUMBLE -> Emphasis.FUMBLE;
                    case HIT, MISS -> Emphasis.NONE;
                };
        return new RollAnimation(Kind.ATTACK, new Roll(result.roll(), emphasis, attacker), Optional.empty());
    }

    public static RollAnimation contest(ContestResult result, String initiator, String opponent) {
        return new RollAnimation(
                Kind.CONTEST,
                roll(result.initiatorRoll(), initiator),
                Optional.of(roll(result.opponentRoll(), opponent)));
    }

    public static RollAnimation initiative(RollDetail detail, String roller) {
        return new RollAnimation(Kind.INITIATIVE, roll(detail, roller), Optional.empty());
    }

    /** Outside an attack there is no Critfall outcome to read, so the die's own extremes carry the drama. */
    private static Roll roll(RollDetail detail, String roller) {
        Emphasis emphasis =
                switch (detail.kept()) {
                    case CRIT_FACE -> Emphasis.CRIT;
                    case FUMBLE_FACE -> Emphasis.FUMBLE;
                    default -> Emphasis.NONE;
                };
        return new Roll(detail, emphasis, roller);
    }

    private static void writeRoll(FriendlyByteBuf buf, Roll roll) {
        RollDetail detail = roll.detail();
        buf.writeEnum(detail.mode());
        buf.writeVarInt(detail.kept());
        buf.writeBoolean(detail.dropped().isPresent());
        detail.dropped().ifPresent(buf::writeVarInt);
        buf.writeEnum(roll.emphasis());
        buf.writeUtf(roll.roller());
    }

    private static Roll readRoll(FriendlyByteBuf buf) {
        RollMode mode = buf.readEnum(RollMode.class);
        int kept = buf.readVarInt();
        OptionalInt dropped = buf.readBoolean() ? OptionalInt.of(buf.readVarInt()) : OptionalInt.empty();
        return new Roll(new RollDetail(mode, kept, dropped), buf.readEnum(Emphasis.class), buf.readUtf());
    }
}
