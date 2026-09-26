package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;

/** The in-combat action UI. Off restores the command-only flow: no snapshots, no clicks accepted. */
public record ActionUiConfig(boolean enabled) {

    public static ActionUiConfig fromJson(ConfigSource source) {
        return new ActionUiConfig(source.section("action_ui").read("enabled", BOOLEAN));
    }
}
