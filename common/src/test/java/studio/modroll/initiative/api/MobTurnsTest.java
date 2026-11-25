package studio.modroll.initiative.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ConfigSource;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;

class MobTurnsTest {

    @AfterEach
    void reset() {
        InitiativeConfig.overrideTurnsForTesting(null);
        InitiativeConfig.overrideActionsForTesting(null);
    }

    @Test
    void enabledWithTurnsCustomMobTurnsAndActionsOn() {
        configure(true, true, true);
        assertTrue(MobTurns.enabled());
    }

    @Test
    void disabledWithTurnsOff() {
        configure(false, true, true);
        assertFalse(MobTurns.enabled());
    }

    @Test
    void disabledWithCustomMobTurnsOff() {
        configure(true, false, true);
        assertFalse(MobTurns.enabled());
    }

    @Test
    void disabledWithActionsOff() {
        configure(true, true, false);
        assertFalse(MobTurns.enabled());
    }

    private static void configure(boolean turns, boolean customMobTurns, boolean actions) {
        String json = """
                {"turns": {"enabled": %s, "custom_mob_turns": %s}, "actions": {"enabled": %s}}""".formatted(turns, customMobTurns, actions);
        ConfigSource source = ConfigSource.of(JsonParser.parseString(json).getAsJsonObject());
        InitiativeConfig.overrideTurnsForTesting(TurnConfig.fromJson(source));
        InitiativeConfig.overrideActionsForTesting(ActionConfig.fromJson(source));
    }
}
