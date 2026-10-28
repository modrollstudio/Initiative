package studio.modroll.initiative.turn;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.encounter.Encounter;

/**
 * Tells the table when the order passed over somebody who had already acted this round — an attacker
 * whose way into the fight was its turn. One line per skip, to every player in the encounter, on the
 * action bar like the other turn lines.
 */
public final class SkippedTurns {

    public interface Sender {
        void send(Player player, Component line);
    }

    private static final Sender ACTION_BAR = (player, line) -> player.displayClientMessage(line, true);
    private static Sender senderOverride;

    private SkippedTurns() {}

    /** Test-scope only, mirroring the sync classes' sender overrides. */
    public static void overrideSenderForTesting(Sender override) {
        senderOverride = override;
    }

    /** Test-scope only. */
    public static void clearSenderOverrideForTesting() {
        senderOverride = null;
    }

    public static void announce(ServerLevel level, Encounter encounter) {
        for (UUID skipped : encounter.turnOrder().drainSkippedTurns()) {
            Entity entity = level.getEntity(skipped);
            if (entity == null) {
                continue;
            }
            Component line = Component.translatable("initiative.turn.already_acted", entity.getDisplayName());
            for (UUID participant : encounter.participants()) {
                if (encounter.side(participant) == Encounter.Side.PLAYER
                        && level.getEntity(participant) instanceof Player player) {
                    activeSender().send(player, line);
                }
            }
        }
    }

    private static Sender activeSender() {
        return senderOverride != null ? senderOverride : ACTION_BAR;
    }
}
