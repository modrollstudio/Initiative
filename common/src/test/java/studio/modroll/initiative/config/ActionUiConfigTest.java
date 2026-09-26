package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import org.junit.jupiter.api.Test;

class ActionUiConfigTest {

    @Test
    void parsesEnabled() {
        assertTrue(ActionUiConfig.fromJson(source("{\"action_ui\": {\"enabled\": true}}"))
                .enabled());
        assertFalse(ActionUiConfig.fromJson(source("{\"action_ui\": {\"enabled\": false}}"))
                .enabled());
    }

    @Test
    void missingEnabledFallsBackToTheDefault() {
        ConfigSource source = source("{\"action_ui\": {}}");
        assertTrue(ActionUiConfig.fromJson(source).enabled());
        assertReported(source, ConfigIssue.Kind.MISSING, "action_ui.enabled");
    }

    @Test
    void missingActionUiObjectFallsBackToTheDefault() {
        ConfigSource source = source("{}");
        assertTrue(ActionUiConfig.fromJson(source).enabled());
        assertReported(source, ConfigIssue.Kind.MISSING, "action_ui");
    }
}
