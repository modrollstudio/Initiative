package studio.modroll.initiative.roll;

import java.util.Optional;
import studio.modroll.initiative.config.RollAnimationConfig;

/**
 * The roll the local player is currently watching. Deliberately free of client-only imports:
 * payload registration references it on both dists, so it must be classloadable on a dedicated
 * server. A finished animation forgets itself the next time a frame is asked for.
 */
public final class RollAnimationClientCache {

    private static final long MILLIS_PER_TICK = 50L;

    private record Playing(RollAnimation animation, long startedAtMillis) {}

    private static volatile Playing current;

    private RollAnimationClientCache() {}

    public static void accept(RollAnimation animation, long nowMillis) {
        current = new Playing(animation, nowMillis);
    }

    public static Optional<RollFrame> frame(long nowMillis, RollAnimationConfig config) {
        Playing playing = current;
        if (playing == null) {
            return Optional.empty();
        }
        RollFrame frame = RollFrame.at(playing.animation(), elapsedTicks(playing, nowMillis), config);
        if (frame.finished()) {
            current = null;
            return Optional.empty();
        }
        return Optional.of(frame);
    }

    /** True while dice are still cycling faces — the window in which the result must stay hidden. */
    public static boolean tumbling(long nowMillis, RollAnimationConfig config) {
        Playing playing = current;
        return playing != null && elapsedTicks(playing, nowMillis) < config.tumbleTicks();
    }

    public static void clear() {
        current = null;
    }

    private static int elapsedTicks(Playing playing, long nowMillis) {
        return (int) ((nowMillis - playing.startedAtMillis()) / MILLIS_PER_TICK);
    }
}
