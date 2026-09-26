package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.initiative.config.ConfigTests.assertNothingReported;
import static studio.modroll.initiative.config.ConfigTests.assertReported;
import static studio.modroll.initiative.config.ConfigTests.source;

import org.junit.jupiter.api.Test;

class ActionConfigTest {

    private static String complete(String movementBudget) {
        return """
                {"actions": {
                    "enabled": true,
                    "movement_budget_blocks": %s,
                    "single_attack_per_turn": true,
                    "attack_reach_blocks": 3.0,
                    "end_turn_when_spent": false,
                    "mob_end_turn_when_spent": true,
                    "dash": true,
                    "disengage": true,
                    "dodge": true,
                    "help": false,
                    "hide": true,
                    "opportunity_attack": true,
                    "opportunity_attack_reach_blocks": 3.0,
                    "hide_observer_perception_bonus": 10,
                    "hide_stealth_bonus": 0,
                    "hide_suppresses_targeting": true,
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
                }}""".formatted(movementBudget);
    }

    @Test
    void parsesAllFields() {
        ConfigSource source = source(complete("6.0"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertTrue(config.enabled());
        assertEquals(6.0, config.movementBudgetBlocks());
        assertTrue(config.singleAttackPerTurn());
        assertEquals(3.0, config.attackReachBlocks());
        assertFalse(config.endTurnWhenSpent());
        assertTrue(config.mobEndTurnWhenSpent());
        assertTrue(config.dash());
        assertTrue(config.disengage());
        assertTrue(config.dodge());
        assertFalse(config.help());
        assertNothingReported(source);
    }

    @Test
    void missingFieldFallsBackToItsDefaultAlone() {
        ConfigSource source = source("""
                {"actions": {
                    "enabled": true,
                    "single_attack_per_turn": true,
                    "attack_reach_blocks": 3.0,
                    "end_turn_when_spent": true,
                    "mob_end_turn_when_spent": true,
                    "dash": true,
                    "disengage": true,
                    "dodge": true,
                    "help": true
                }}""");
        ActionConfig config = ActionConfig.fromJson(source);
        assertEquals(6.0, config.movementBudgetBlocks());
        assertEquals(3.0, config.attackReachBlocks());
        assertTrue(config.endTurnWhenSpent());
        assertReported(source, ConfigIssue.Kind.MISSING, "actions.movement_budget_blocks");
    }

    @Test
    void missingActionToggleFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("6.0").replace("\"dodge\": true,\n", ""));
        ActionConfig config = ActionConfig.fromJson(source);
        assertTrue(config.dodge());
        assertFalse(config.help());
        assertReported(source, ConfigIssue.Kind.MISSING, "actions.dodge");
    }

    @Test
    void missingActionsObjectFallsBackToEveryDefault() {
        ConfigSource source = source("{}");
        ActionConfig config = ActionConfig.fromJson(source);
        assertEquals(6.0, config.movementBudgetBlocks());
        assertEquals(4.0, config.attackReachBlocks());
        assertReported(source, ConfigIssue.Kind.MISSING, "actions");
    }

    @Test
    void negativeMovementBudgetFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("-1.0"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertEquals(6.0, config.movementBudgetBlocks());
        assertEquals(10, config.hideObserverPerceptionBonus());
        assertReported(source, ConfigIssue.Kind.INVALID, "actions.movement_budget_blocks");
    }

    @Test
    void parsesTheM4bFields() {
        ActionConfig config = ActionConfig.fromJson(source(complete("6.0")));
        assertTrue(config.hide());
        assertTrue(config.opportunityAttack());
        assertEquals(3.0, config.opportunityAttackReachBlocks());
        assertEquals(10, config.hideObserverPerceptionBonus());
        assertEquals(0, config.hideStealthBonus());
        assertTrue(config.hideSuppressesTargeting());
    }

    /** A config written before the targeting layer existed keeps Hide, and gets the new default. */
    @Test
    void missingTargetingSuppressionDefaultsToOn() {
        ConfigSource source = source(complete("6.0").replace("\"hide_suppresses_targeting\": true,\n", ""));
        ActionConfig config = ActionConfig.fromJson(source);
        assertTrue(config.hideSuppressesTargeting());
        assertTrue(config.hide());
        assertReported(source, ConfigIssue.Kind.MISSING, "actions.hide_suppresses_targeting");
    }

    @Test
    void targetingSuppressionCanBeTurnedOffAlone() {
        ConfigSource source = source(
                complete("6.0").replace("\"hide_suppresses_targeting\": true", "\"hide_suppresses_targeting\": false"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertFalse(config.hideSuppressesTargeting());
        assertTrue(config.hide());
        assertNothingReported(source);
    }

    @Test
    void negativeReachFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("6.0")
                .replace("\"opportunity_attack_reach_blocks\": 3.0", "\"opportunity_attack_reach_blocks\": -1.0"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertEquals(3.0, config.opportunityAttackReachBlocks());
        assertEquals(10, config.hideObserverPerceptionBonus());
        assertReported(source, ConfigIssue.Kind.INVALID, "actions.opportunity_attack_reach_blocks");
    }

    @Test
    void parsesTheM5aNativeActionFields() {
        ActionConfig config = ActionConfig.fromJson(source(complete("6.0")));
        assertTrue(config.shove());
        assertEquals(3.0, config.shoveReachBlocks());
        assertEquals(1.0, config.shoveKnockbackStrength());
        assertEquals(0, config.shoveAttackerBonus());
        assertEquals(0, config.shoveDefenderBonus());
        assertTrue(config.enderPearlBlink());
        assertEquals(8.0, config.blinkMaxBlocks());
        assertTrue(config.fishingRodReel());
        assertEquals(1.0, config.reelPullStrength());
        assertEquals(0, config.reelAttackerBonus());
        assertEquals(0, config.reelDefenderBonus());
    }

    @Test
    void negativeAttackReachFallsBackToItsDefaultAlone() {
        ConfigSource source =
                source(complete("6.0").replace("\"attack_reach_blocks\": 3.0", "\"attack_reach_blocks\": -1.0"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertEquals(4.0, config.attackReachBlocks());
        assertEquals(10, config.hideObserverPerceptionBonus());
        assertReported(source, ConfigIssue.Kind.INVALID, "actions.attack_reach_blocks");
    }

    @Test
    void negativeShoveReachFallsBackToItsDefaultAlone() {
        ConfigSource source =
                source(complete("6.0").replace("\"shove_reach_blocks\": 3.0", "\"shove_reach_blocks\": -1.0"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertEquals(3.0, config.shoveReachBlocks());
        assertReported(source, ConfigIssue.Kind.INVALID, "actions.shove_reach_blocks");
    }

    /** A literal too large for a double overflows to infinity, which passes every range check. */
    @Test
    void overflowingMovementBudgetFallsBackToItsDefault() {
        ConfigSource source = source(complete("1e400"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertEquals(6.0, config.movementBudgetBlocks());
        assertReported(source, ConfigIssue.Kind.INVALID, "actions.movement_budget_blocks");
    }

    @Test
    void overflowingBlinkDistanceFallsBackToItsDefault() {
        ConfigSource source =
                source(complete("6.0").replace("\"blink_max_blocks\": 8.0", "\"blink_max_blocks\": 1e400"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertEquals(8.0, config.blinkMaxBlocks());
        assertReported(source, ConfigIssue.Kind.INVALID, "actions.blink_max_blocks");
    }

    /** {@code null} is the easy typo, and it used to take the whole file down with it. */
    @Test
    void aMalformedToggleFallsBackToItsDefaultAlone() {
        ConfigSource source = source(complete("6.0").replace("\"dash\": true", "\"dash\": null"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertTrue(config.dash());
        assertEquals(6.0, config.movementBudgetBlocks());
        assertReported(source, ConfigIssue.Kind.INVALID, "actions.dash");
    }

    /** Renamed at M8; a file from before it keeps every other action setting it specifies. */
    @Test
    void theOldFishingRodKeyIsReportedAsUnknown() {
        ConfigSource source =
                source(complete("6.0").replace("\"fishing_rod_reel\": true", "\"fishing_rod_grapple\": false"));
        ActionConfig config = ActionConfig.fromJson(source);
        assertTrue(config.fishingRodReel());
        assertEquals(10, config.hideObserverPerceptionBonus());
        assertReported(source, ConfigIssue.Kind.UNKNOWN, "actions.fishing_rod_grapple");
        assertReported(source, ConfigIssue.Kind.MISSING, "actions.fishing_rod_reel");
    }
}
