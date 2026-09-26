package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertNothingReported;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import org.junit.jupiter.api.Test;

class CoverConfigTest {

    private static String complete() {
        return """
                {"cover": {
                    "enabled": true,
                    "half_cover_ac_bonus": 3,
                    "three_quarter_cover_ac_bonus": 6,
                    "half_cover_threshold": 0.4,
                    "three_quarter_cover_threshold": 0.8,
                    "total_cover_blocks_attack": true,
                    "block_placement_costs_movement": true,
                    "block_placement_movement_cost": 2.0
                }}""";
    }

    @Test
    void parsesAllFields() {
        ConfigSource source = source(complete());
        CoverConfig config = CoverConfig.fromJson(source);
        assertTrue(config.enabled());
        assertEquals(3, config.halfCoverAcBonus());
        assertEquals(6, config.threeQuarterCoverAcBonus());
        assertEquals(0.4, config.halfCoverThreshold());
        assertEquals(0.8, config.threeQuarterCoverThreshold());
        assertTrue(config.totalCoverBlocksAttack());
        assertTrue(config.blockPlacementCostsMovement());
        assertEquals(2.0, config.blockPlacementMovementCost());
        assertNothingReported(source);
    }

    @Test
    void missingCoverObjectFallsBackToEveryDefault() {
        ConfigSource source = source("{}");
        CoverConfig config = CoverConfig.fromJson(source);
        assertEquals(2, config.halfCoverAcBonus());
        assertEquals(0.5, config.halfCoverThreshold());
        assertEquals(1.0, config.blockPlacementMovementCost());
        assertReported(source, ConfigIssue.Kind.MISSING, "cover");
    }

    @Test
    void missingFieldFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete().replace("\"enabled\": true,\n", ""));
        CoverConfig config = CoverConfig.fromJson(source);
        assertTrue(config.enabled());
        assertEquals(3, config.halfCoverAcBonus());
        assertReported(source, ConfigIssue.Kind.MISSING, "cover.enabled");
    }

    @Test
    void negativeAcBonusFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete().replace("\"half_cover_ac_bonus\": 3", "\"half_cover_ac_bonus\": -1"));
        CoverConfig config = CoverConfig.fromJson(source);
        assertEquals(2, config.halfCoverAcBonus());
        assertEquals(6, config.threeQuarterCoverAcBonus());
        assertReported(source, ConfigIssue.Kind.INVALID, "cover.half_cover_ac_bonus");
    }

    @Test
    void thresholdOutOfRangeFallsBackToItsDefaultAlone() {
        ConfigSource source = source(
                complete().replace("\"three_quarter_cover_threshold\": 0.8", "\"three_quarter_cover_threshold\": 1.5"));
        CoverConfig config = CoverConfig.fromJson(source);
        assertEquals(0.75, config.threeQuarterCoverThreshold());
        assertEquals(0.4, config.halfCoverThreshold());
        assertReported(source, ConfigIssue.Kind.INVALID, "cover.three_quarter_cover_threshold");
    }

    /** Neither threshold is usable once they cross: only the built-in pair is known to agree. */
    @Test
    void halfThresholdAboveThreeQuarterRevertsBoth() {
        ConfigSource source =
                source(complete().replace("\"half_cover_threshold\": 0.4", "\"half_cover_threshold\": 0.9"));
        CoverConfig config = CoverConfig.fromJson(source);
        assertEquals(0.5, config.halfCoverThreshold());
        assertEquals(0.75, config.threeQuarterCoverThreshold());
        assertReported(source, ConfigIssue.Kind.INVALID, "cover.half_cover_threshold");
    }

    @Test
    void negativeBlockPlacementCostFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete()
                .replace("\"block_placement_movement_cost\": 2.0", "\"block_placement_movement_cost\": -0.5"));
        CoverConfig config = CoverConfig.fromJson(source);
        assertEquals(1.0, config.blockPlacementMovementCost());
        assertTrue(config.blockPlacementCostsMovement());
        assertReported(source, ConfigIssue.Kind.INVALID, "cover.block_placement_movement_cost");
    }

    /** A literal too large for a double overflows to infinity, which passes the negative check. */
    @Test
    void overflowingBlockPlacementCostFallsBackToItsDefault() {
        ConfigSource source = source(complete()
                .replace("\"block_placement_movement_cost\": 2.0", "\"block_placement_movement_cost\": 1e400"));
        CoverConfig config = CoverConfig.fromJson(source);
        assertEquals(1.0, config.blockPlacementMovementCost());
        assertReported(source, ConfigIssue.Kind.INVALID, "cover.block_placement_movement_cost");
    }

    @Test
    void togglesParseIndependently() {
        CoverConfig config = CoverConfig.fromJson(source(complete()
                .replace("\"enabled\": true", "\"enabled\": false")
                .replace("\"block_placement_costs_movement\": true", "\"block_placement_costs_movement\": false")));
        assertFalse(config.enabled());
        assertFalse(config.blockPlacementCostsMovement());
    }
}
