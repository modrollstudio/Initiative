package studio.modroll.initiative.ui;

import java.util.Optional;

/**
 * The local player's latest action UI. Free of client-only imports: payload registration references
 * it on both dists, so it must be classloadable on a dedicated server.
 */
public final class ActionUiClientCache {

    private static volatile ActionUiSnapshot current;

    private ActionUiClientCache() {}

    public static void accept(ActionUiSnapshot snapshot) {
        current = snapshot.yourTurn() ? snapshot : null;
    }

    public static Optional<ActionUiSnapshot> current() {
        return Optional.ofNullable(current);
    }

    public static void clear() {
        current = null;
    }
}
