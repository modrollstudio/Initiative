package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;
import static studio.modroll.initiative.config.ConfigValue.FINITE_DOUBLE;
import static studio.modroll.initiative.config.ConfigValue.POSITIVE_DOUBLE;

public record EncounterConfig(
        boolean enabled,
        boolean triggerOnPlayerAttacking,
        boolean triggerOnPlayerAttacked,
        boolean triggerOnProvokedNeutral,
        boolean holdRangedAttackers,
        boolean pullNearbyPlayers,
        double triggerRadius,
        double leaveRadius) {

    public static EncounterConfig fromJson(ConfigSource source) {
        ConfigSection encounters = source.section("encounters");
        double triggerRadius = encounters.read("trigger_radius", POSITIVE_DOUBLE);
        double leaveRadius = encounters.read("leave_radius", FINITE_DOUBLE);
        if (leaveRadius < triggerRadius) {
            String reason = "leave_radius (" + leaveRadius + ") is below trigger_radius (" + triggerRadius + ")";
            triggerRadius = encounters.revertToDefault("trigger_radius", POSITIVE_DOUBLE, reason);
            leaveRadius = encounters.revertToDefault("leave_radius", FINITE_DOUBLE, reason);
        }
        return new EncounterConfig(
                encounters.read("enabled", BOOLEAN),
                encounters.read("trigger_on_player_attacking", BOOLEAN),
                encounters.read("trigger_on_player_attacked", BOOLEAN),
                encounters.read("trigger_on_provoked_neutral", BOOLEAN),
                encounters.read("hold_ranged_attackers", BOOLEAN),
                encounters.read("pull_nearby_players", BOOLEAN),
                triggerRadius,
                leaveRadius);
    }
}
