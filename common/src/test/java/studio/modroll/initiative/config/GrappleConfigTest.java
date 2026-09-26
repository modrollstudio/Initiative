package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertNothingReported;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import org.junit.jupiter.api.Test;

class GrappleConfigTest {

    private static String complete(String reach, String breakDistance) {
        return """
                {"grapple": {
                    "enabled": true,
                    "reach_blocks": %s,
                    "attacker_bonus": 1,
                    "defender_bonus": 2,
                    "break_distance_blocks": %s,
                    "escape": true,
                    "escape_attacker_bonus": 3,
                    "escape_defender_bonus": 4
                }}""".formatted(reach, breakDistance);
    }

    @Test
    void parsesAllFields() {
        ConfigSource source = source(complete("3.0", "5.0"));
        GrappleConfig config = GrappleConfig.fromJson(source);
        assertTrue(config.enabled());
        assertEquals(3.0, config.reachBlocks());
        assertEquals(1, config.attackerBonus());
        assertEquals(2, config.defenderBonus());
        assertEquals(5.0, config.breakDistanceBlocks());
        assertTrue(config.escape());
        assertEquals(3, config.escapeAttackerBonus());
        assertEquals(4, config.escapeDefenderBonus());
        assertNothingReported(source);
    }

    @Test
    void negativeReachFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("-1.0", "5.0"));
        GrappleConfig config = GrappleConfig.fromJson(source);
        assertEquals(3.0, config.reachBlocks());
        assertEquals(5.0, config.breakDistanceBlocks());
        assertEquals(1, config.attackerBonus());
        assertReported(source, ConfigIssue.Kind.INVALID, "grapple.reach_blocks");
    }

    /** Neither distance is usable once they disagree: only the built-in pair is known to agree. */
    @Test
    void breakDistanceBelowReachRevertsBoth() {
        ConfigSource source = source(complete("4.0", "2.0"));
        GrappleConfig config = GrappleConfig.fromJson(source);
        assertEquals(3.0, config.reachBlocks());
        assertEquals(5.0, config.breakDistanceBlocks());
        assertEquals(1, config.attackerBonus());
        assertReported(source, ConfigIssue.Kind.INVALID, "grapple.break_distance_blocks");
    }

    @Test
    void missingFieldFallsBackToItsDefaultAlone() {
        ConfigSource source = source("{\"grapple\": {\"enabled\": false, \"attacker_bonus\": 7}}");
        GrappleConfig config = GrappleConfig.fromJson(source);
        assertEquals(3.0, config.reachBlocks());
        assertEquals(5.0, config.breakDistanceBlocks());
        assertEquals(7, config.attackerBonus());
        assertReported(source, ConfigIssue.Kind.MISSING, "grapple.reach_blocks");
    }

    @Test
    void missingGrappleObjectFallsBackToEveryDefault() {
        ConfigSource source = source("{}");
        GrappleConfig config = GrappleConfig.fromJson(source);
        assertTrue(config.enabled());
        assertEquals(3.0, config.reachBlocks());
        assertEquals(5.0, config.breakDistanceBlocks());
        assertReported(source, ConfigIssue.Kind.MISSING, "grapple");
    }

    /** A literal too large for a double overflows to infinity, which passes every range check. */
    @Test
    void overflowingReachFallsBackToItsDefault() {
        ConfigSource source = source(complete("1e400", "1e400"));
        GrappleConfig config = GrappleConfig.fromJson(source);
        assertEquals(3.0, config.reachBlocks());
        assertEquals(5.0, config.breakDistanceBlocks());
        assertReported(source, ConfigIssue.Kind.INVALID, "grapple.reach_blocks");
        assertReported(source, ConfigIssue.Kind.INVALID, "grapple.break_distance_blocks");
    }

    @Test
    void overflowingBreakDistanceFallsBackToItsDefault() {
        ConfigSource source = source(complete("3.0", "1e400"));
        GrappleConfig config = GrappleConfig.fromJson(source);
        assertEquals(3.0, config.reachBlocks());
        assertEquals(5.0, config.breakDistanceBlocks());
        assertReported(source, ConfigIssue.Kind.INVALID, "grapple.break_distance_blocks");
    }
}
