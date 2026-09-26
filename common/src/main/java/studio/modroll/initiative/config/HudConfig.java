package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;

public record HudConfig(boolean enabled) {

    public static HudConfig fromJson(ConfigSource source) {
        return new HudConfig(source.section("hud").read("enabled", BOOLEAN));
    }
}
