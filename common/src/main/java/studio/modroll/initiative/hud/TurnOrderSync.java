package studio.modroll.initiative.hud;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;

/**
 * Sends turn-order snapshots to player participants. Snapshots are diffed at the same reconcile
 * point that drives freeze, so the HUD can never desync from turn state; nothing is sent while
 * the state is unchanged. Empty snapshots (bar clears) are sent only from the end paths.
 */
public final class TurnOrderSync {

    public interface Sender {
        void send(Player player, TurnOrderSnapshot snapshot);
    }

    private static Sender sender = (player, snapshot) -> {};
    private static Sender senderOverride;

    private TurnOrderSync() {}

    /** Loader wiring installs the real network send here at mod init. */
    public static void setSender(Sender networkSender) {
        sender = networkSender;
    }

    /** Test-scope only, mirroring Critfall's {@code RollService.setRoller} convention. */
    public static void overrideSenderForTesting(Sender override) {
        senderOverride = override;
    }

    /** Test-scope only. */
    public static void clearSenderOverrideForTesting() {
        senderOverride = null;
    }

    public static void sync(ServerLevel level, Encounter encounter) {
        if (!enabled()) {
            return;
        }
        TurnOrderSnapshot snapshot = TurnOrderSnapshots.build(level, encounter);
        if (snapshot.isEmpty() || snapshot.equals(encounter.lastHudSnapshot())) {
            return;
        }
        encounter.setLastHudSnapshot(snapshot);
        sendToPlayers(level, encounter, snapshot);
    }

    public static void encounterEnding(ServerLevel level, Encounter encounter) {
        if (encounter.lastHudSnapshot() == null) {
            return;
        }
        sendToPlayers(level, encounter, TurnOrderSnapshot.empty(encounter.id()));
    }

    public static void participantRemoved(ServerLevel level, Encounter encounter, UUID participant) {
        if (encounter.lastHudSnapshot() == null) {
            return;
        }
        Player player = resolvePlayer(level, participant);
        if (player != null) {
            activeSender().send(player, TurnOrderSnapshot.empty(encounter.id()));
        }
    }

    private static boolean enabled() {
        return InitiativeConfig.hud().enabled() && InitiativeConfig.turns().enabled();
    }

    private static void sendToPlayers(ServerLevel level, Encounter encounter, TurnOrderSnapshot snapshot) {
        for (UUID participant : encounter.participants()) {
            if (level.getEntity(participant) instanceof Player player) {
                activeSender().send(player, snapshot);
            }
        }
    }

    /** PlayerList first (survives death and dimension change), level lookup for gametest mocks. */
    private static Player resolvePlayer(ServerLevel level, UUID participant) {
        Player online = level.getServer().getPlayerList().getPlayer(participant);
        if (online != null) {
            return online;
        }
        return level.getEntity(participant) instanceof Player player ? player : null;
    }

    private static Sender activeSender() {
        return senderOverride != null ? senderOverride : sender;
    }
}
