package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertNothingReported;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TurnConfigTest {

    private static String complete(String timeoutTicks, String bonusPerSpeed, String maxBonus) {
        return completeWithFreeze(timeoutTicks, "45", bonusPerSpeed, maxBonus, "[]");
    }

    private static String completeWithFreeze(
            String timeoutTicks, String mobTimeoutTicks, String bonusPerSpeed, String maxBonus, String noFreezeTypes) {
        return """
                {"turns": {
                    "enabled": true,
                    "turn_timeout_ticks": %s,
                    "mob_turn_timeout_ticks": %s,
                    "initiative_bonus_per_speed": %s,
                    "initiative_max_bonus": %s,
                    "freeze_enabled": true,
                    "acting_marker_enabled": false,
                    "restrict_players_off_turn": false,
                    "no_freeze_types": %s
                }}""".formatted(timeoutTicks, mobTimeoutTicks, bonusPerSpeed, maxBonus, noFreezeTypes);
    }

    @Test
    void parsesAllFields() {
        ConfigSource source = source(complete("100", "20.0", "12"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertTrue(config.enabled());
        assertEquals(100, config.turnTimeoutTicks());
        assertEquals(45, config.mobTurnTimeoutTicks());
        assertEquals(20.0, config.initiativeBonusPerSpeed());
        assertEquals(12, config.initiativeMaxBonus());
        assertTrue(config.freezeEnabled());
        assertFalse(config.actingMarkerEnabled());
        assertFalse(config.restrictPlayersOffTurn());
        assertEquals(Set.of(), config.noFreezeTypes());
        assertNothingReported(source);
    }

    @Test
    void parsesNoFreezeTypes() {
        TurnConfig config = TurnConfig.fromJson(
                source(completeWithFreeze("100", "45", "20.0", "12", "[\"minecraft:husk\", \"somemod:boss\"]")));
        assertEquals(Set.of("minecraft:husk", "somemod:boss"), config.noFreezeTypes());
    }

    @Test
    void missingFieldFallsBackToItsDefaultAlone() {
        ConfigSource source = source("""
                {"turns": {
                    "enabled": true,
                    "mob_turn_timeout_ticks": 45,
                    "initiative_bonus_per_speed": 20.0,
                    "initiative_max_bonus": 12,
                    "freeze_enabled": true,
                    "acting_marker_enabled": true,
                    "restrict_players_off_turn": true,
                    "no_freeze_types": []
                }}""");
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(600, config.turnTimeoutTicks());
        assertEquals(45, config.mobTurnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.MISSING, "turns.turn_timeout_ticks");
    }

    @Test
    void missingFreezeFieldFallsBackToItsDefaultAlone() {
        ConfigSource source = source("""
                {"turns": {
                    "enabled": true,
                    "turn_timeout_ticks": 100,
                    "mob_turn_timeout_ticks": 45,
                    "initiative_bonus_per_speed": 20.0,
                    "initiative_max_bonus": 12,
                    "acting_marker_enabled": true,
                    "restrict_players_off_turn": true,
                    "no_freeze_types": []
                }}""");
        TurnConfig config = TurnConfig.fromJson(source);
        assertTrue(config.freezeEnabled());
        assertEquals(100, config.turnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.MISSING, "turns.freeze_enabled");
    }

    @Test
    void missingTurnsObjectFallsBackToEveryDefault() {
        ConfigSource source = source("{}");
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(600, config.turnTimeoutTicks());
        assertEquals(120, config.mobTurnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.MISSING, "turns");
    }

    /** The M9 update added this key; every file written before it is missing exactly this one. */
    @Test
    void missingMobTimeoutFallsBackToItsDefaultAlone() {
        ConfigSource source = source("""
                {"turns": {
                    "enabled": true,
                    "turn_timeout_ticks": 100,
                    "initiative_bonus_per_speed": 20.0,
                    "initiative_max_bonus": 12,
                    "freeze_enabled": true,
                    "acting_marker_enabled": true,
                    "restrict_players_off_turn": true,
                    "no_freeze_types": ["minecraft:warden"]
                }}""");
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(120, config.mobTurnTimeoutTicks());
        assertEquals(100, config.turnTimeoutTicks());
        assertEquals(Set.of("minecraft:warden"), config.noFreezeTypes());
        assertReported(source, ConfigIssue.Kind.MISSING, "turns.mob_turn_timeout_ticks");
    }

    /** The off-turn update added this key; every file written before it is missing exactly this one. */
    @Test
    void missingOffTurnRestrictionFallsBackToItsDefaultAlone() {
        ConfigSource source = source("""
                {"turns": {
                    "enabled": true,
                    "turn_timeout_ticks": 100,
                    "mob_turn_timeout_ticks": 45,
                    "initiative_bonus_per_speed": 20.0,
                    "initiative_max_bonus": 12,
                    "freeze_enabled": true,
                    "acting_marker_enabled": true,
                    "no_freeze_types": ["minecraft:warden"]
                }}""");
        TurnConfig config = TurnConfig.fromJson(source);
        assertTrue(config.restrictPlayersOffTurn());
        assertEquals(100, config.turnTimeoutTicks());
        assertEquals(Set.of("minecraft:warden"), config.noFreezeTypes());
        assertReported(source, ConfigIssue.Kind.MISSING, "turns.restrict_players_off_turn");
    }

    @Test
    void nonPositiveTimeoutFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("0", "20.0", "12"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(600, config.turnTimeoutTicks());
        assertEquals(45, config.mobTurnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.INVALID, "turns.turn_timeout_ticks");
    }

    @Test
    void nonPositiveMobTimeoutFallsBackToItsDefaultAlone() {
        ConfigSource source = source(completeWithFreeze("100", "0", "20.0", "12", "[]"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(120, config.mobTurnTimeoutTicks());
        assertEquals(100, config.turnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.INVALID, "turns.mob_turn_timeout_ticks");
    }

    @Test
    void negativeBonusPerSpeedFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("100", "-1.0", "12"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(20.0, config.initiativeBonusPerSpeed());
        assertEquals(100, config.turnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.INVALID, "turns.initiative_bonus_per_speed");
    }

    @Test
    void negativeMaxBonusFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("100", "20.0", "-1"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(12, config.initiativeMaxBonus());
        assertEquals(100, config.turnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.INVALID, "turns.initiative_max_bonus");
    }

    /** A literal too large for a double overflows to infinity, which passes the negative check. */
    @Test
    void overflowingBonusPerSpeedFallsBackToItsDefault() {
        ConfigSource source = source(complete("100", "1e400", "12"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(20.0, config.initiativeBonusPerSpeed());
        assertReported(source, ConfigIssue.Kind.INVALID, "turns.initiative_bonus_per_speed");
    }

    @Test
    void invalidNoFreezeTypeIdFallsBackToTheDefaultList() {
        ConfigSource source = source(completeWithFreeze("100", "45", "20.0", "12", "[\"Not A Valid Id\"]"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(Set.of(), config.noFreezeTypes());
        assertEquals(100, config.turnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.INVALID, "turns.no_freeze_types");
    }

    @Test
    void nonStringNoFreezeTypeFallsBackToTheDefaultList() {
        ConfigSource source = source(completeWithFreeze("100", "45", "20.0", "12", "[7]"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(Set.of(), config.noFreezeTypes());
        assertReported(source, ConfigIssue.Kind.INVALID, "turns.no_freeze_types");
    }

    @Test
    void anUnknownKeyIsReportedAndIgnored() {
        ConfigSource source = source(complete("100", "20.0", "12")
                .replace("\"enabled\": true,", "\"enabled\": true,\n    \"turn_timeout_seconds\": 5,"));
        TurnConfig config = TurnConfig.fromJson(source);
        assertEquals(100, config.turnTimeoutTicks());
        assertReported(source, ConfigIssue.Kind.UNKNOWN, "turns.turn_timeout_seconds");
    }
}
