package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertNothingReported;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import org.junit.jupiter.api.Test;

class RollAnimationConfigTest {

    private static String complete(String tumbleTicks, String faceChangeTicks) {
        return """
                {"roll_animation": {
                    "enabled": true,
                    "tumble_ticks": %s,
                    "hold_ticks": 7,
                    "face_change_ticks": %s,
                    "hold_readout": true,
                    "shared_visibility": true
                }}""".formatted(tumbleTicks, faceChangeTicks);
    }

    @Test
    void parsesAllFields() {
        ConfigSource source = source(complete("10", "2"));
        RollAnimationConfig config = RollAnimationConfig.fromJson(source);
        assertTrue(config.enabled());
        assertEquals(10, config.tumbleTicks());
        assertEquals(7, config.holdTicks());
        assertEquals(2, config.faceChangeTicks());
        assertTrue(config.holdReadout());
        assertTrue(config.sharedVisibility());
        assertNothingReported(source);
    }

    @Test
    void totalDurationIsTumblePlusHold() {
        assertEquals(
                17, RollAnimationConfig.fromJson(source(complete("10", "2"))).totalTicks());
    }

    @Test
    void missingObjectFallsBackToEveryDefault() {
        ConfigSource source = source("{}");
        RollAnimationConfig config = RollAnimationConfig.fromJson(source);
        assertEquals(10, config.tumbleTicks());
        assertEquals(10, config.holdTicks());
        assertEquals(2, config.faceChangeTicks());
        assertReported(source, ConfigIssue.Kind.MISSING, "roll_animation");
    }

    /** The M9 update added this key; every file written before it is missing exactly this one. */
    @Test
    void missingSharedVisibilityFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("10", "2").replace(",\n    \"shared_visibility\": true", ""));
        RollAnimationConfig config = RollAnimationConfig.fromJson(source);
        assertTrue(config.sharedVisibility());
        assertEquals(7, config.holdTicks());
        assertReported(source, ConfigIssue.Kind.MISSING, "roll_animation.shared_visibility");
    }

    @Test
    void negativeTumbleTicksFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("-1", "2"));
        RollAnimationConfig config = RollAnimationConfig.fromJson(source);
        assertEquals(10, config.tumbleTicks());
        assertEquals(7, config.holdTicks());
        assertReported(source, ConfigIssue.Kind.INVALID, "roll_animation.tumble_ticks");
    }

    @Test
    void faceChangeTicksBelowOneFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("10", "0"));
        RollAnimationConfig config = RollAnimationConfig.fromJson(source);
        assertEquals(2, config.faceChangeTicks());
        assertEquals(10, config.tumbleTicks());
        assertReported(source, ConfigIssue.Kind.INVALID, "roll_animation.face_change_ticks");
    }
}
