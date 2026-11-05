package studio.modroll.initiative.checks;

import net.minecraft.server.level.ServerPlayer;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * Whether the optional Checks mod drives Initiative's skill rolls. Callers ask {@link #active} before
 * touching {@link ChecksBridge}, which is the only class naming a Checks type: without Checks it is
 * never loaded, and every caller keeps its flat config bonuses.
 */
public final class ChecksIntegration {

    public static final String MOD_ID = "checks";

    private static volatile boolean present;

    private ChecksIntegration() {}

    /** Loader wiring reports at mod init whether Checks is installed. */
    public static void setPresent(boolean installed) {
        present = installed;
    }

    public static boolean present() {
        return present;
    }

    /** Checks is installed and turned on in the config: enough for initiative, which rolls no skill. */
    public static boolean enabled() {
        return present && InitiativeConfig.checks().enabled();
    }

    /** {@link #enabled} and has the skills these rolls use loaded. */
    public static boolean active() {
        return enabled() && ChecksBridge.skillsLoaded();
    }

    /** Loader wiring calls this once at mod init, after {@link #setPresent}. */
    public static void registerRestBlocker() {
        if (present) {
            ChecksBridge.addRestBlocker();
        }
    }

    /** Checks asks this every tick a player rests, so a player who leaves the encounter can rest again at once. */
    static boolean blocksRest(ServerPlayer player) {
        return enabled()
                && InitiativeConfig.checks().blockShortRests()
                && EncounterManager.encounterContaining(player.serverLevel(), player.getUUID())
                        .isPresent();
    }
}
