package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A config written against an older Initiative must keep loading. Every case here is a file a player
 * could plausibly have on disk after an update, a downgrade or a typo.
 */
class ConfigMigrationTest {

    /** Every key present, every value deliberately different from the built-in default. */
    private static String customized() {
        return """
                {
                  "encounters": {
                    "enabled": false,
                    "trigger_on_player_attacking": false,
                    "trigger_on_player_attacked": false,
                    "trigger_on_provoked_neutral": false,
                    "trigger_radius": 7.0,
                    "leave_radius": 15.0
                  },
                  "turns": {
                    "enabled": false,
                    "turn_timeout_ticks": 111,
                    "mob_turn_timeout_ticks": 22,
                    "initiative_bonus_per_speed": 5.0,
                    "initiative_max_bonus": 3,
                    "freeze_enabled": false,
                    "acting_marker_enabled": true,
                    "restrict_players_off_turn": false,
                    "no_freeze_types": ["minecraft:warden"]
                  },
                  "actions": {
                    "enabled": false,
                    "movement_budget_blocks": 9.0,
                    "single_attack_per_turn": false,
                    "attack_reach_blocks": 2.0,
                    "end_turn_when_spent": true,
                    "mob_end_turn_when_spent": false,
                    "dash": false,
                    "disengage": false,
                    "dodge": false,
                    "help": false,
                    "hide": false,
                    "opportunity_attack": false,
                    "opportunity_attack_reach_blocks": 1.0,
                    "hide_observer_perception_bonus": 4,
                    "hide_stealth_bonus": 4,
                    "shove": false,
                    "shove_reach_blocks": 1.0,
                    "shove_knockback_strength": 2.0,
                    "shove_attacker_bonus": 4,
                    "shove_defender_bonus": 4,
                    "ender_pearl_blink": false,
                    "blink_max_blocks": 2.0,
                    "fishing_rod_reel": false,
                    "reel_pull_strength": 2.0,
                    "reel_attacker_bonus": 4,
                    "reel_defender_bonus": 4
                  },
                  "cover": {
                    "enabled": false,
                    "half_cover_ac_bonus": 4,
                    "three_quarter_cover_ac_bonus": 9,
                    "half_cover_threshold": 0.25,
                    "three_quarter_cover_threshold": 0.9,
                    "total_cover_blocks_attack": false,
                    "block_placement_costs_movement": false,
                    "block_placement_movement_cost": 3.0
                  },
                  "grapple": {
                    "enabled": false,
                    "reach_blocks": 1.0,
                    "attacker_bonus": 4,
                    "defender_bonus": 4,
                    "break_distance_blocks": 9.0,
                    "escape": false,
                    "escape_attacker_bonus": 4,
                    "escape_defender_bonus": 4
                  },
                  "hud": {
                    "enabled": false
                  },
                  "action_ui": {
                    "enabled": false
                  },
                  "roll_animation": {
                    "enabled": false,
                    "tumble_ticks": 3,
                    "hold_ticks": 4,
                    "face_change_ticks": 5,
                    "hold_readout": false,
                    "shared_visibility": false
                  }
                }""";
    }

    private static void write(Path configDir, String json) throws IOException {
        Files.writeString(configDir.resolve("initiative.json"), json);
    }

    private static void assertCustomizedValuesSurvived() {
        assertFalse(InitiativeConfig.encounters().enabled());
        assertEquals(7.0, InitiativeConfig.encounters().triggerRadius());
        assertEquals(15.0, InitiativeConfig.encounters().leaveRadius());
        assertEquals(111, InitiativeConfig.turns().turnTimeoutTicks());
        assertEquals(5.0, InitiativeConfig.turns().initiativeBonusPerSpeed());
        assertEquals(Set.of("minecraft:warden"), InitiativeConfig.turns().noFreezeTypes());
        assertFalse(InitiativeConfig.turns().restrictPlayersOffTurn());
        assertEquals(9.0, InitiativeConfig.actions().movementBudgetBlocks());
        assertEquals(4, InitiativeConfig.cover().halfCoverAcBonus());
        assertEquals(9.0, InitiativeConfig.grapple().breakDistanceBlocks());
        assertFalse(InitiativeConfig.hud().enabled());
        assertFalse(InitiativeConfig.actionUi().enabled());
        assertEquals(3, InitiativeConfig.rollAnimation().tumbleTicks());
    }

    /** The M9 update: two keys appeared, and every existing file was missing both. */
    @Test
    void aFileFromBeforeTheM9KeysKeepsEverythingElse(@TempDir Path configDir) throws IOException {
        write(
                configDir,
                customized()
                        .replace("\"mob_turn_timeout_ticks\": 22,\n", "")
                        .replace(",\n    \"shared_visibility\": false", ""));
        InitiativeConfig.load(configDir);
        assertCustomizedValuesSurvived();
        assertEquals(120, InitiativeConfig.turns().mobTurnTimeoutTicks());
        assertTrue(InitiativeConfig.rollAnimation().sharedVisibility());
    }

    /** The neutral-provocation update: one key appeared, and every existing file is missing it. */
    @Test
    void aFileFromBeforeTheProvokedNeutralKeyKeepsEverythingElse(@TempDir Path configDir) throws IOException {
        write(configDir, customized().replace("\"trigger_on_provoked_neutral\": false,\n", ""));
        InitiativeConfig.load(configDir);
        assertCustomizedValuesSurvived();
        assertTrue(InitiativeConfig.encounters().triggerOnProvokedNeutral());
    }

    /** The off-turn update: one key appeared, and every existing file is missing exactly it. */
    @Test
    void aFileFromBeforeTheOffTurnKeyKeepsEverythingElse(@TempDir Path configDir) throws IOException {
        write(configDir, customized().replace("\"restrict_players_off_turn\": false,\n", ""));
        InitiativeConfig.load(configDir);
        assertEquals(111, InitiativeConfig.turns().turnTimeoutTicks());
        assertEquals(22, InitiativeConfig.turns().mobTurnTimeoutTicks());
        assertEquals(Set.of("minecraft:warden"), InitiativeConfig.turns().noFreezeTypes());
        assertTrue(InitiativeConfig.turns().restrictPlayersOffTurn());
    }

    @Test
    void anUnknownKeyIsIgnoredAndTheRestIsKept(@TempDir Path configDir) throws IOException {
        write(
                configDir,
                customized()
                        .replace("\"turn_timeout_ticks\": 111,", "\"turn_timeout_ticks\": 111,\n    \"wobble\": 3,")
                        .replace("  \"hud\": {", "  \"nonsense\": {\"enabled\": true},\n  \"hud\": {"));
        InitiativeConfig.load(configDir);
        assertCustomizedValuesSurvived();
    }

    @Test
    void oneInvalidValueDefaultsThatKeyAlone(@TempDir Path configDir) throws IOException {
        write(configDir, customized().replace("\"turn_timeout_ticks\": 111", "\"turn_timeout_ticks\": 0"));
        InitiativeConfig.load(configDir);
        assertEquals(600, InitiativeConfig.turns().turnTimeoutTicks());
        assertEquals(22, InitiativeConfig.turns().mobTurnTimeoutTicks());
        assertEquals(5.0, InitiativeConfig.turns().initiativeBonusPerSpeed());
        assertEquals(9.0, InitiativeConfig.actions().movementBudgetBlocks());
    }

    @Test
    void aMalformedBooleanDefaultsThatKeyAlone(@TempDir Path configDir) throws IOException {
        write(
                configDir,
                customized()
                        .replace(
                                "\"enabled\": false,\n    \"movement_budget_blocks\"",
                                "\"enabled\": null,\n    \"movement_budget_blocks\""));
        InitiativeConfig.load(configDir);
        assertTrue(InitiativeConfig.actions().enabled());
        assertEquals(9.0, InitiativeConfig.actions().movementBudgetBlocks());
        assertFalse(InitiativeConfig.encounters().enabled());
    }

    /** The audit's overflow guard, now granular: infinity is still refused, the file still loads. */
    @Test
    void anOverflowingNumberDefaultsThatKeyAlone(@TempDir Path configDir) throws IOException {
        write(configDir, customized().replace("\"movement_budget_blocks\": 9.0", "\"movement_budget_blocks\": 1e400"));
        InitiativeConfig.load(configDir);
        assertEquals(6.0, InitiativeConfig.actions().movementBudgetBlocks());
        assertEquals(2.0, InitiativeConfig.actions().attackReachBlocks());
    }

    @Test
    void aWholeMissingGroupTakesItsDefaultsWithoutTouchingTheRest(@TempDir Path configDir) throws IOException {
        write(configDir, customized().replace("""
                  "hud": {
                    "enabled": false
                  },
                """, ""));
        InitiativeConfig.load(configDir);
        assertTrue(InitiativeConfig.hud().enabled());
        assertEquals(111, InitiativeConfig.turns().turnTimeoutTicks());
    }

    @Test
    void aGroupThatIsNotAnObjectTakesItsDefaults(@TempDir Path configDir) throws IOException {
        write(configDir, customized().replace("""
                  "hud": {
                    "enabled": false
                  },
                """, "  \"hud\": 7,\n"));
        InitiativeConfig.load(configDir);
        assertTrue(InitiativeConfig.hud().enabled());
        assertEquals(111, InitiativeConfig.turns().turnTimeoutTicks());
    }

    @Test
    void anEmptyObjectLoadsEveryBuiltInDefault(@TempDir Path configDir) throws IOException {
        write(configDir, "{}");
        InitiativeConfig.load(configDir);
        assertTrue(InitiativeConfig.encounters().enabled());
        assertEquals(12.0, InitiativeConfig.encounters().triggerRadius());
        assertEquals(600, InitiativeConfig.turns().turnTimeoutTicks());
        assertEquals(6.0, InitiativeConfig.actions().movementBudgetBlocks());
        assertEquals(2, InitiativeConfig.cover().halfCoverAcBonus());
        assertEquals(3.0, InitiativeConfig.grapple().reachBlocks());
        assertTrue(InitiativeConfig.hud().enabled());
        assertTrue(InitiativeConfig.actionUi().enabled());
        assertEquals(10, InitiativeConfig.rollAnimation().tumbleTicks());
    }

    @Test
    void aPairOfKeysThatContradictEachOtherFallsBackTogether(@TempDir Path configDir) throws IOException {
        write(configDir, customized().replace("\"leave_radius\": 15.0", "\"leave_radius\": 3.0"));
        InitiativeConfig.load(configDir);
        assertEquals(12.0, InitiativeConfig.encounters().triggerRadius());
        assertEquals(20.0, InitiativeConfig.encounters().leaveRadius());
        assertEquals(111, InitiativeConfig.turns().turnTimeoutTicks());
    }

    @Test
    void aFileFromANewerBuildKeepsTheSettingsThisBuildKnows(@TempDir Path configDir) throws IOException {
        write(
                configDir,
                customized()
                        .replace("{\n  \"encounters\"", "{\n  \"format_version\": 99,\n  \"encounters\"")
                        .replace(
                                "\"turn_timeout_ticks\": 111,",
                                "\"turn_timeout_ticks\": 111,\n    \"from_the_future\": true,"));
        InitiativeConfig.load(configDir);
        assertCustomizedValuesSurvived();
    }

    @Test
    void aNewerFormatVersionIsReported() {
        ConfigSource source = ConfigTests.source("{\"format_version\": 99}");
        ConfigTests.assertReported(source, ConfigIssue.Kind.NEWER_FORMAT, "format_version");
    }

    @Test
    void aFormatVersionThatIsNotANumberIsReported() {
        ConfigSource source = ConfigTests.source("{\"format_version\": \"one\"}");
        ConfigTests.assertReported(source, ConfigIssue.Kind.INVALID, "format_version");
    }

    /** Every file written before the key existed, which is the very file this must not punish. */
    @Test
    void anAbsentFormatVersionIsNotReported() {
        ConfigTests.assertNothingReported(ConfigTests.source("{}"));
    }

    @Test
    void theBundledDefaultsAreCompleteAndConsistent() {
        ConfigSource source = ConfigTests.source(ConfigSource.defaultsJson());
        readEveryGroup(source);
        ConfigTests.assertNothingReported(source);
    }

    /** The audit found both documented examples stale; a copy-paste of one has to load clean. */
    @Test
    void theDocumentedExampleLoadsWithNothingReported() throws IOException {
        Path example = Path.of("..", "docs", "examples", "initiative.json");
        assertTrue(Files.exists(example), () -> "no documented example at " + example.toAbsolutePath());
        ConfigSource source = ConfigTests.source(Files.readString(example));
        readEveryGroup(source);
        ConfigTests.assertNothingReported(source);
    }

    private static void readEveryGroup(ConfigSource source) {
        EncounterConfig.fromJson(source);
        TurnConfig.fromJson(source);
        ActionConfig.fromJson(source);
        CoverConfig.fromJson(source);
        GrappleConfig.fromJson(source);
        HudConfig.fromJson(source);
        ActionUiConfig.fromJson(source);
        RollAnimationConfig.fromJson(source);
    }
}
