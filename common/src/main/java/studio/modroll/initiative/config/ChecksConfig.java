package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;

/**
 * The optional Checks integration. With Checks installed and this on, initiative adds each
 * participant's Dexterity modifier and Shove, Grapple, Escape and Hide roll their own skills; off, or
 * without Checks, initiative keeps the movement-speed bonus and the actions their flat config bonuses.
 * {@code blockShortRests} keeps a player in an encounter from taking Checks' short rest.
 */
public record ChecksConfig(boolean enabled, boolean blockShortRests) {

    public static ChecksConfig fromJson(ConfigSource source) {
        ConfigSection checks = source.section("checks");
        return new ChecksConfig(checks.read("enabled", BOOLEAN), checks.read("block_short_rests", BOOLEAN));
    }
}
