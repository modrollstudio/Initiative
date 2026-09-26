package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InitiativeConfigTest {

    @Test
    void loadWritesDefaultFileWhenMissing(@TempDir Path configDir) {
        InitiativeConfig.load(configDir);
        assertTrue(Files.exists(configDir.resolve("initiative.json")));
        assertTrue(InitiativeConfig.encounters().enabled());
        assertTrue(InitiativeConfig.encounters().triggerRadius() > 0);
        assertTrue(InitiativeConfig.turns().enabled());
        assertTrue(InitiativeConfig.turns().turnTimeoutTicks() > 0);
        assertTrue(InitiativeConfig.turns().mobTurnTimeoutTicks() > 0);
        assertTrue(InitiativeConfig.turns().mobTurnTimeoutTicks()
                < InitiativeConfig.turns().turnTimeoutTicks());
        assertTrue(InitiativeConfig.actions().enabled());
        assertTrue(InitiativeConfig.actions().movementBudgetBlocks() > 0);
        assertTrue(InitiativeConfig.cover().enabled());
        assertTrue(InitiativeConfig.cover().halfCoverAcBonus() > 0);
        assertTrue(InitiativeConfig.grapple().enabled());
        assertTrue(InitiativeConfig.grapple().reachBlocks() > 0);
        assertTrue(InitiativeConfig.hud().enabled());
        assertTrue(InitiativeConfig.actionUi().enabled());
        assertTrue(InitiativeConfig.actions().mobEndTurnWhenSpent());
        assertTrue(InitiativeConfig.rollAnimation().enabled());
        assertTrue(InitiativeConfig.rollAnimation().totalTicks() > 0);
        assertTrue(InitiativeConfig.rollAnimation().sharedVisibility());
    }

    @Test
    void loadParsesExistingFile(@TempDir Path configDir) throws IOException {
        Files.writeString(configDir.resolve("initiative.json"), """
                {"encounters": {
                    "enabled": false,
                    "trigger_on_player_attacking": true,
                    "trigger_on_player_attacked": true,
                    "trigger_radius": 5.0,
                    "leave_radius": 9.0
                },
                "turns": {
                    "enabled": false,
                    "turn_timeout_ticks": 40,
                    "mob_turn_timeout_ticks": 8,
                    "initiative_bonus_per_speed": 10.0,
                    "initiative_max_bonus": 6,
                    "freeze_enabled": false,
                    "acting_marker_enabled": false,
                    "no_freeze_types": ["minecraft:warden"]
                },
                "actions": {
                    "enabled": false,
                    "movement_budget_blocks": 4.0,
                    "single_attack_per_turn": false,
                    "attack_reach_blocks": 3.0,
                    "end_turn_when_spent": false,
                    "mob_end_turn_when_spent": true,
                    "dash": true,
                    "disengage": true,
                    "dodge": true,
                    "help": true,
                    "hide": true,
                    "opportunity_attack": true,
                    "opportunity_attack_reach_blocks": 3.0,
                    "hide_observer_perception_bonus": 10,
                    "hide_stealth_bonus": 0,
                    "shove": true,
                    "shove_reach_blocks": 3.0,
                    "shove_knockback_strength": 1.0,
                    "shove_attacker_bonus": 0,
                    "shove_defender_bonus": 0,
                    "ender_pearl_blink": true,
                    "blink_max_blocks": 8.0,
                    "fishing_rod_reel": true,
                    "reel_pull_strength": 1.0,
                    "reel_attacker_bonus": 0,
                    "reel_defender_bonus": 0
                },
                "cover": {
                    "enabled": false,
                    "half_cover_ac_bonus": 3,
                    "three_quarter_cover_ac_bonus": 7,
                    "half_cover_threshold": 0.4,
                    "three_quarter_cover_threshold": 0.8,
                    "total_cover_blocks_attack": false,
                    "block_placement_costs_movement": false,
                    "block_placement_movement_cost": 2.0
                },
                "grapple": {
                    "enabled": false,
                    "reach_blocks": 2.0,
                    "attacker_bonus": 0,
                    "defender_bonus": 0,
                    "break_distance_blocks": 4.0,
                    "escape": false,
                    "escape_attacker_bonus": 0,
                    "escape_defender_bonus": 0
                },
                "hud": {
                    "enabled": false
                },
                "action_ui": {
                    "enabled": false
                },
                "roll_animation": {
                    "enabled": false,
                    "tumble_ticks": 4,
                    "hold_ticks": 6,
                    "face_change_ticks": 1,
                    "hold_readout": false,
                    "shared_visibility": false
                }}""");
        InitiativeConfig.load(configDir);
        assertEquals(false, InitiativeConfig.encounters().enabled());
        assertEquals(5.0, InitiativeConfig.encounters().triggerRadius());
        assertEquals(false, InitiativeConfig.turns().enabled());
        assertEquals(40, InitiativeConfig.turns().turnTimeoutTicks());
        assertEquals(8, InitiativeConfig.turns().mobTurnTimeoutTicks());
        assertEquals(false, InitiativeConfig.turns().freezeEnabled());
        assertEquals(Set.of("minecraft:warden"), InitiativeConfig.turns().noFreezeTypes());
        assertEquals(false, InitiativeConfig.actions().enabled());
        assertEquals(4.0, InitiativeConfig.actions().movementBudgetBlocks());
        assertEquals(false, InitiativeConfig.cover().enabled());
        assertEquals(3, InitiativeConfig.cover().halfCoverAcBonus());
        assertEquals(2.0, InitiativeConfig.cover().blockPlacementMovementCost());
        assertEquals(false, InitiativeConfig.grapple().enabled());
        assertEquals(4.0, InitiativeConfig.grapple().breakDistanceBlocks());
        assertEquals(false, InitiativeConfig.hud().enabled());
        assertEquals(false, InitiativeConfig.actionUi().enabled());
        assertEquals(false, InitiativeConfig.rollAnimation().enabled());
        assertEquals(10, InitiativeConfig.rollAnimation().totalTicks());
        assertEquals(false, InitiativeConfig.rollAnimation().sharedVisibility());
    }

    @Test
    void loadFallsBackToDefaultsOnBrokenFile(@TempDir Path configDir) throws IOException {
        Files.writeString(configDir.resolve("initiative.json"), "not json at all");
        InitiativeConfig.load(configDir);
        assertTrue(InitiativeConfig.encounters().enabled());
        assertTrue(InitiativeConfig.encounters().triggerRadius() > 0);
    }

    @Test
    void overrideReplacesLoadedConfig(@TempDir Path configDir) {
        InitiativeConfig.load(configDir);
        EncounterConfig override = new EncounterConfig(true, true, true, false, true, true, 3.0, 5.0);
        InitiativeConfig.overrideEncountersForTesting(override);
        assertEquals(override, InitiativeConfig.encounters());
    }
}
