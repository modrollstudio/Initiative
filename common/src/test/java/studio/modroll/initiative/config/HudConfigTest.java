package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import org.junit.jupiter.api.Test;

class HudConfigTest {

    @Test
    void parsesEnabled() {
        assertTrue(HudConfig.fromJson(source("{\"hud\": {\"enabled\": true}}")).enabled());
        assertFalse(
                HudConfig.fromJson(source("{\"hud\": {\"enabled\": false}}")).enabled());
    }

    @Test
    void missingEnabledFallsBackToTheDefault() {
        ConfigSource source = source("{\"hud\": {}}");
        assertTrue(HudConfig.fromJson(source).enabled());
        assertReported(source, ConfigIssue.Kind.MISSING, "hud.enabled");
    }

    @Test
    void missingHudObjectFallsBackToTheDefault() {
        ConfigSource source = source("{}");
        assertTrue(HudConfig.fromJson(source).enabled());
        assertReported(source, ConfigIssue.Kind.MISSING, "hud");
    }

    @Test
    void aGroupThatIsNotAnObjectFallsBackToTheDefault() {
        ConfigSource source = source("{\"hud\": 7}");
        assertTrue(HudConfig.fromJson(source).enabled());
        assertReported(source, ConfigIssue.Kind.INVALID, "hud");
    }
}
