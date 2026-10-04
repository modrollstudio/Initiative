package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;

/**
 * The optional Checks integration. With Checks installed and this on, Shove, Grapple, Escape and
 * Hide roll the participants' own skills; off, or without Checks, they keep the flat config bonuses.
 */
public record ChecksConfig(boolean enabled) {

    public static ChecksConfig fromJson(ConfigSource source) {
        return new ChecksConfig(source.section("checks").read("enabled", BOOLEAN));
    }
}
