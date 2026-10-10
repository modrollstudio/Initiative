package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;

/**
 * The optional Checks integration. With Checks installed and this on, initiative adds each
 * participant's Dexterity modifier and Shove, Grapple, Escape and Hide roll their own skills; off, or
 * without Checks, initiative keeps the movement-speed bonus and the actions their flat config bonuses.
 */
public record ChecksConfig(boolean enabled) {

    public static ChecksConfig fromJson(ConfigSource source) {
        return new ChecksConfig(source.section("checks").read("enabled", BOOLEAN));
    }
}
