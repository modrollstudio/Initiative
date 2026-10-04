package studio.modroll.initiative.checks;

import studio.modroll.initiative.config.InitiativeConfig;

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

    /** Checks is installed, turned on in the config, and has the skills these rolls use loaded. */
    public static boolean active() {
        return present && InitiativeConfig.checks().enabled() && ChecksBridge.skillsLoaded();
    }
}
