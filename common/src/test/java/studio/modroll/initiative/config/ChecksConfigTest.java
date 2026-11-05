package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import org.junit.jupiter.api.Test;

class ChecksConfigTest {

    @Test
    void parsesEnabled() {
        assertTrue(ChecksConfig.fromJson(source("{\"checks\": {\"enabled\": true}}"))
                .enabled());
        assertFalse(ChecksConfig.fromJson(source("{\"checks\": {\"enabled\": false}}"))
                .enabled());
    }

    @Test
    void parsesBlockShortRests() {
        assertTrue(ChecksConfig.fromJson(source("{\"checks\": {\"block_short_rests\": true}}"))
                .blockShortRests());
        assertFalse(ChecksConfig.fromJson(source("{\"checks\": {\"block_short_rests\": false}}"))
                .blockShortRests());
    }

    /** A file written before the integration existed keeps it on, the same as a fresh install. */
    @Test
    void missingChecksObjectFallsBackToOn() {
        ConfigSource source = source("{}");
        assertTrue(ChecksConfig.fromJson(source).enabled());
        assertTrue(ChecksConfig.fromJson(source).blockShortRests());
        assertReported(source, ConfigIssue.Kind.MISSING, "checks");
    }

    /** A file from before Checks had rests keeps its other keys and blocks rests in a fight. */
    @Test
    void aFileWithoutBlockShortRestsBlocksThem() {
        ConfigSource source = source("{\"checks\": {\"enabled\": false}}");
        ChecksConfig checks = ChecksConfig.fromJson(source);
        assertFalse(checks.enabled());
        assertTrue(checks.blockShortRests());
        assertReported(source, ConfigIssue.Kind.MISSING, "checks.block_short_rests");
    }

    @Test
    void aGroupThatIsNotAnObjectFallsBackToTheDefault() {
        ConfigSource source = source("{\"checks\": 7}");
        assertTrue(ChecksConfig.fromJson(source).enabled());
        assertReported(source, ConfigIssue.Kind.INVALID, "checks");
    }
}
