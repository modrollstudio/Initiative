package studio.modroll.initiative.hud;

import java.util.Optional;

/**
 * Latest snapshot for the local player's encounter. Deliberately free of client-only imports:
 * payload registration references it on both dists, so it must be classloadable on a dedicated
 * server.
 */
public final class TurnOrderClientCache {

    private static volatile TurnOrderSnapshot current;

    private TurnOrderClientCache() {}

    public static void accept(TurnOrderSnapshot snapshot) {
        if (!snapshot.isEmpty()) {
            current = snapshot;
            return;
        }
        TurnOrderSnapshot cached = current;
        if (cached != null && cached.encounterId().equals(snapshot.encounterId())) {
            current = null;
        }
    }

    public static Optional<TurnOrderSnapshot> current() {
        return Optional.ofNullable(current);
    }

    public static void clear() {
        current = null;
    }
}
