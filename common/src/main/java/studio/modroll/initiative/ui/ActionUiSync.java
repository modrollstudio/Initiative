package studio.modroll.initiative.ui;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;

/**
 * Sends each player participant its own action UI, diffed against the last one it received, at the
 * same reconcile point that drives freeze and the turn-order bar. Per-player from the start: a
 * second player in the encounter simply gets its own snapshot.
 */
public final class ActionUiSync {

    public interface Sender {
        void send(Player player, ActionUiSnapshot snapshot);
    }

    private static Sender sender = (player, snapshot) -> {};
    private static Sender senderOverride;

    private ActionUiSync() {}

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

    public static boolean enabled() {
        return InitiativeConfig.actionUi().enabled() && ActionEconomy.active();
    }

    public static void sync(ServerLevel level, Encounter encounter) {
        if (!enabled()) {
            return;
        }
        for (UUID participant : encounter.participants()) {
            if (level.getEntity(participant) instanceof Player player) {
                send(encounter, player, ActionUiSnapshots.build(level, encounter, player));
            }
        }
    }

    public static void encounterEnding(ServerLevel level, Encounter encounter) {
        for (UUID participant : encounter.participants()) {
            if (level.getEntity(participant) instanceof Player player) {
                send(encounter, player, ActionUiSnapshot.INACTIVE);
            }
        }
    }

    public static void participantRemoved(ServerLevel level, Encounter encounter, UUID participant) {
        if (level.getEntity(participant) instanceof Player player) {
            send(encounter, player, ActionUiSnapshot.INACTIVE);
        }
    }

    private static void send(Encounter encounter, Player player, ActionUiSnapshot snapshot) {
        if (snapshot.equals(encounter.lastActionUiSnapshot(player.getUUID()))) {
            return;
        }
        encounter.setLastActionUiSnapshot(player.getUUID(), snapshot);
        activeSender().send(player, snapshot);
    }

    private static Sender activeSender() {
        return senderOverride != null ? senderOverride : sender;
    }
}
