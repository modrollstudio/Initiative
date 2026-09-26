package studio.modroll.initiative.roll;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import studio.modroll.initiative.config.RollAnimationConfig;

/**
 * Keeps the result from beating the dice to the screen. Critfall resolves an attack and sends its
 * action-bar readout in the same server tick the roll animation starts, so without this the text
 * would spell out the outcome while the die is still cycling faces. A readout that lands mid-tumble
 * is parked here and re-shown the moment the die settles; nothing about the resolution waits.
 */
public final class RollReadoutHold {

    private static volatile Component pending;

    private RollReadoutHold() {}

    /** True when the caller must not show {@code message} yet — it is kept for {@link #release}. */
    public static boolean hold(Component message, long nowMillis, RollAnimationConfig config) {
        if (!config.enabled() || !config.holdReadout() || !RollAnimationClientCache.tumbling(nowMillis, config)) {
            return false;
        }
        pending = message;
        return true;
    }

    /** The held readout, once the dice have settled; empty while they tumble or when nothing is held. */
    public static Optional<Component> release(long nowMillis, RollAnimationConfig config) {
        Component held = pending;
        if (held == null || RollAnimationClientCache.tumbling(nowMillis, config)) {
            return Optional.empty();
        }
        pending = null;
        return Optional.of(held);
    }

    public static void clear() {
        pending = null;
    }
}
