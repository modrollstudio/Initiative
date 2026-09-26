package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertNothingReported;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import org.junit.jupiter.api.Test;

class EncounterConfigTest {

    private static String complete(String triggerRadius, String leaveRadius) {
        return """
                {"encounters": {
                    "enabled": true,
                    "trigger_on_player_attacking": true,
                    "trigger_on_player_attacked": false,
                    "trigger_on_provoked_neutral": true,
                    "hold_ranged_attackers": false,
                    "pull_nearby_players": false,
                    "trigger_radius": %s,
                    "leave_radius": %s
                }}""".formatted(triggerRadius, leaveRadius);
    }

    @Test
    void parsesAllFields() {
        ConfigSource source = source(complete("12.0", "20.0"));
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertTrue(config.enabled());
        assertTrue(config.triggerOnPlayerAttacking());
        assertFalse(config.triggerOnPlayerAttacked());
        assertTrue(config.triggerOnProvokedNeutral());
        assertFalse(config.holdRangedAttackers());
        assertFalse(config.pullNearbyPlayers());
        assertEquals(12.0, config.triggerRadius());
        assertEquals(20.0, config.leaveRadius());
        assertNothingReported(source);
    }

    /** The key a file written before ranged attackers were held does not have. */
    @Test
    void missingRangedHoldKeyFallsBackToOn() {
        ConfigSource source = source("""
                {"encounters": {
                    "enabled": true,
                    "trigger_on_player_attacking": true,
                    "trigger_on_player_attacked": true,
                    "trigger_on_provoked_neutral": true,
                    "trigger_radius": 12.0,
                    "leave_radius": 20.0
                }}""");
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertTrue(config.holdRangedAttackers());
        assertReported(source, ConfigIssue.Kind.MISSING, "encounters.hold_ranged_attackers");
    }

    /** The key a file written before nearby players were pulled into the fight does not have. */
    @Test
    void missingPullNearbyPlayersKeyFallsBackToOn() {
        ConfigSource source = source("""
                {"encounters": {
                    "enabled": true,
                    "trigger_on_player_attacking": true,
                    "trigger_on_player_attacked": true,
                    "trigger_on_provoked_neutral": true,
                    "hold_ranged_attackers": true,
                    "trigger_radius": 12.0,
                    "leave_radius": 20.0
                }}""");
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertTrue(config.pullNearbyPlayers());
        assertReported(source, ConfigIssue.Kind.MISSING, "encounters.pull_nearby_players");
    }

    @Test
    void missingProvokedNeutralKeyFallsBackToOn() {
        ConfigSource source = source("""
                {"encounters": {
                    "enabled": true,
                    "trigger_on_player_attacking": false,
                    "trigger_on_player_attacked": false,
                    "trigger_radius": 12.0,
                    "leave_radius": 20.0
                }}""");
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertTrue(config.triggerOnProvokedNeutral());
        assertFalse(config.triggerOnPlayerAttacking());
        assertReported(source, ConfigIssue.Kind.MISSING, "encounters.trigger_on_provoked_neutral");
    }

    @Test
    void missingFieldFallsBackToItsDefaultAlone() {
        ConfigSource source = source("""
                {"encounters": {
                    "enabled": false,
                    "trigger_on_player_attacking": true,
                    "trigger_on_player_attacked": true,
                    "leave_radius": 20.0
                }}""");
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertEquals(12.0, config.triggerRadius());
        assertFalse(config.enabled());
        assertReported(source, ConfigIssue.Kind.MISSING, "encounters.trigger_radius");
    }

    @Test
    void missingEncountersObjectFallsBackToEveryDefault() {
        ConfigSource source = source("{}");
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertTrue(config.enabled());
        assertEquals(12.0, config.triggerRadius());
        assertEquals(20.0, config.leaveRadius());
        assertReported(source, ConfigIssue.Kind.MISSING, "encounters");
    }

    @Test
    void nonPositiveTriggerRadiusFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("0.0", "25.0"));
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertEquals(12.0, config.triggerRadius());
        assertEquals(25.0, config.leaveRadius());
        assertReported(source, ConfigIssue.Kind.INVALID, "encounters.trigger_radius");
    }

    /** Neither radius is usable on its own once they disagree: only the built-in pair is known to. */
    @Test
    void leaveRadiusSmallerThanTriggerRadiusRevertsBoth() {
        ConfigSource source = source(complete("15.0", "8.0"));
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertEquals(12.0, config.triggerRadius());
        assertEquals(20.0, config.leaveRadius());
        assertReported(source, ConfigIssue.Kind.INVALID, "encounters.leave_radius");
    }

    /** A literal too large for a double overflows to infinity, which passes every range check. */
    @Test
    void overflowingTriggerRadiusFallsBackToItsDefault() {
        ConfigSource source = source(complete("1e400", "25.0"));
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertEquals(12.0, config.triggerRadius());
        assertEquals(25.0, config.leaveRadius());
        assertReported(source, ConfigIssue.Kind.INVALID, "encounters.trigger_radius");
    }

    @Test
    void overflowingLeaveRadiusFallsBackToItsDefault() {
        ConfigSource source = source(complete("12.0", "1e400"));
        EncounterConfig config = EncounterConfig.fromJson(source);
        assertEquals(12.0, config.triggerRadius());
        assertEquals(20.0, config.leaveRadius());
        assertReported(source, ConfigIssue.Kind.INVALID, "encounters.leave_radius");
    }
}
