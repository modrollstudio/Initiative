package studio.modroll.initiative.roll;

import java.util.ArrayList;
import java.util.List;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.initiative.config.RollAnimationConfig;
import studio.modroll.initiative.roll.RollAnimation.Emphasis;
import studio.modroll.initiative.roll.RollAnimation.Roll;

/**
 * What the renderer draws for one animation at a given point in its life: the dice tumble through
 * faces while {@code tumbleTicks} run, then settle on their real naturals for {@code holdTicks}.
 * Pure and deterministic — the renderer holds no state of its own and the tests read frames, not
 * pixels.
 */
public record RollFrame(List<Die> dice, int elapsedTicks, boolean finished) {

    public enum Side {
        ACTOR,
        OPPONENT
    }

    public record Die(Side side, int face, boolean kept, boolean settled, Emphasis emphasis, String roller) {}

    private static final int D20_FACES = 20;

    /** Co-prime with the face count, so a tumbling die visits every face before repeating one. */
    private static final int FACE_STRIDE = 7;

    public static RollFrame at(RollAnimation animation, int elapsedTicks, RollAnimationConfig config) {
        List<Die> dice = new ArrayList<>();
        appendSide(dice, Side.ACTOR, animation.actor(), elapsedTicks, config);
        animation.opponent().ifPresent(roll -> appendSide(dice, Side.OPPONENT, roll, elapsedTicks, config));
        return new RollFrame(List.copyOf(dice), elapsedTicks, elapsedTicks >= config.totalTicks());
    }

    private static void appendSide(List<Die> dice, Side side, Roll roll, int elapsedTicks, RollAnimationConfig config) {
        boolean settled = elapsedTicks >= config.tumbleTicks();
        RollDetail detail = roll.detail();
        int keptFace = settled ? detail.kept() : tumblingFace(dice.size(), elapsedTicks, config);
        dice.add(new Die(side, keptFace, true, settled, settled ? roll.emphasis() : Emphasis.NONE, roll.roller()));
        if (detail.dropped().isPresent()) {
            int droppedFace = settled ? detail.dropped().getAsInt() : tumblingFace(dice.size(), elapsedTicks, config);
            dice.add(new Die(side, droppedFace, false, settled, Emphasis.NONE, roll.roller()));
        }
    }

    private static int tumblingFace(int dieIndex, int elapsedTicks, RollAnimationConfig config) {
        int step = elapsedTicks / config.faceChangeTicks();
        return Math.floorMod(step * FACE_STRIDE + dieIndex, D20_FACES) + 1;
    }
}
