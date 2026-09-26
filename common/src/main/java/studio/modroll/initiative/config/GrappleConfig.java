package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;
import static studio.modroll.initiative.config.ConfigValue.INTEGER;
import static studio.modroll.initiative.config.ConfigValue.NON_NEGATIVE_DOUBLE;

/**
 * The unarmed grapple and the escape from it. Its own config group rather than more fields on
 * {@code actions}, the way cover has one: a grapple is a condition with a lifetime, not a one-shot
 * knock. Contest bonuses are flat placeholders until the Checks mod exists.
 */
public record GrappleConfig(
        boolean enabled,
        double reachBlocks,
        int attackerBonus,
        int defenderBonus,
        double breakDistanceBlocks,
        boolean escape,
        int escapeAttackerBonus,
        int escapeDefenderBonus) {

    public static GrappleConfig fromJson(ConfigSource source) {
        ConfigSection grapple = source.section("grapple");
        double reach = grapple.read("reach_blocks", NON_NEGATIVE_DOUBLE);
        double breakDistance = grapple.read("break_distance_blocks", NON_NEGATIVE_DOUBLE);
        if (breakDistance < reach) {
            String reason = "break_distance_blocks (" + breakDistance + ") is below reach_blocks (" + reach + ")";
            reach = grapple.revertToDefault("reach_blocks", NON_NEGATIVE_DOUBLE, reason);
            breakDistance = grapple.revertToDefault("break_distance_blocks", NON_NEGATIVE_DOUBLE, reason);
        }
        return new GrappleConfig(
                grapple.read("enabled", BOOLEAN),
                reach,
                grapple.read("attacker_bonus", INTEGER),
                grapple.read("defender_bonus", INTEGER),
                breakDistance,
                grapple.read("escape", BOOLEAN),
                grapple.read("escape_attacker_bonus", INTEGER),
                grapple.read("escape_defender_bonus", INTEGER));
    }
}
