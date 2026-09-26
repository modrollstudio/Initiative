package studio.modroll.initiative.roll;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * Sends a resolved roll to the players who should watch it. Shared visibility (on by default)
 * gives every player in the encounter the same die, which is the communal tabletop moment; the
 * animation names its roller so a mob's swing at one player still reads to the rest. With it off the
 * roll goes to the one player at the exchange — the roller, or, when a mob rolled, the player it
 * rolled against. Either way the send happens after the roll has resolved and applied, so a disabled
 * animation, an offline player or a roll between two mobs changes nothing about the combat.
 */
public final class RollAnimationSync {

    public interface Sender {
        void send(Player player, RollAnimation animation);
    }

    private static Sender sender = (player, animation) -> {};
    private static Sender senderOverride;

    private RollAnimationSync() {}

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

    /** An unopposed roll — the initiative d20 — with no second side to identify. */
    public static void play(LivingEntity roller, RollAnimation animation) {
        play(roller, roller, animation);
    }

    public static void play(LivingEntity roller, LivingEntity opposing, RollAnimation animation) {
        if (!InitiativeConfig.rollAnimation().enabled()) {
            return;
        }
        Encounter encounter =
                InitiativeConfig.rollAnimation().sharedVisibility() ? encounterAround(roller, opposing) : null;
        if (encounter != null) {
            sendToEncounter((ServerLevel) roller.level(), encounter, animation);
            return;
        }
        Player watcher = watcher(roller, opposing);
        if (watcher != null) {
            activeSender().send(watcher, animation);
        }
    }

    /** The encounter the exchange belongs to; a roll outside one has no audience but its own. */
    private static Encounter encounterAround(LivingEntity roller, LivingEntity opposing) {
        if (!(roller.level() instanceof ServerLevel level)) {
            return null;
        }
        Encounter encounter =
                EncounterManager.encounterContaining(level, roller.getUUID()).orElse(null);
        if (encounter != null || opposing == null) {
            return encounter;
        }
        return EncounterManager.encounterContaining(level, opposing.getUUID()).orElse(null);
    }

    private static void sendToEncounter(ServerLevel level, Encounter encounter, RollAnimation animation) {
        for (UUID participant : encounter.participants()) {
            if (level.getEntity(participant) instanceof Player player) {
                activeSender().send(player, animation);
            }
        }
    }

    private static Player watcher(LivingEntity roller, LivingEntity opposing) {
        if (roller instanceof Player player) {
            return player;
        }
        return opposing instanceof Player player ? player : null;
    }

    private static Sender activeSender() {
        return senderOverride != null ? senderOverride : sender;
    }
}
