package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;
import static studio.modroll.initiative.config.ConfigValue.INTEGER;
import static studio.modroll.initiative.config.ConfigValue.NON_NEGATIVE_DOUBLE;

public record ActionConfig(
        boolean enabled,
        double movementBudgetBlocks,
        boolean singleAttackPerTurn,
        double attackReachBlocks,
        boolean endTurnWhenSpent,
        boolean mobEndTurnWhenSpent,
        boolean dash,
        boolean disengage,
        boolean dodge,
        boolean help,
        boolean hide,
        boolean opportunityAttack,
        double opportunityAttackReachBlocks,
        int hideObserverPerceptionBonus,
        int hideStealthBonus,
        boolean hideSuppressesTargeting,
        boolean shove,
        double shoveReachBlocks,
        double shoveKnockbackStrength,
        int shoveAttackerBonus,
        int shoveDefenderBonus,
        boolean enderPearlBlink,
        double blinkMaxBlocks,
        boolean fishingRodReel,
        double reelPullStrength,
        int reelAttackerBonus,
        int reelDefenderBonus) {

    public static ActionConfig fromJson(ConfigSource source) {
        ConfigSection actions = source.section("actions");
        return new ActionConfig(
                actions.read("enabled", BOOLEAN),
                actions.read("movement_budget_blocks", NON_NEGATIVE_DOUBLE),
                actions.read("single_attack_per_turn", BOOLEAN),
                actions.read("attack_reach_blocks", NON_NEGATIVE_DOUBLE),
                actions.read("end_turn_when_spent", BOOLEAN),
                actions.read("mob_end_turn_when_spent", BOOLEAN),
                actions.read("dash", BOOLEAN),
                actions.read("disengage", BOOLEAN),
                actions.read("dodge", BOOLEAN),
                actions.read("help", BOOLEAN),
                actions.read("hide", BOOLEAN),
                actions.read("opportunity_attack", BOOLEAN),
                actions.read("opportunity_attack_reach_blocks", NON_NEGATIVE_DOUBLE),
                actions.read("hide_observer_perception_bonus", INTEGER),
                actions.read("hide_stealth_bonus", INTEGER),
                actions.read("hide_suppresses_targeting", BOOLEAN),
                actions.read("shove", BOOLEAN),
                actions.read("shove_reach_blocks", NON_NEGATIVE_DOUBLE),
                actions.read("shove_knockback_strength", NON_NEGATIVE_DOUBLE),
                actions.read("shove_attacker_bonus", INTEGER),
                actions.read("shove_defender_bonus", INTEGER),
                actions.read("ender_pearl_blink", BOOLEAN),
                actions.read("blink_max_blocks", NON_NEGATIVE_DOUBLE),
                actions.read("fishing_rod_reel", BOOLEAN),
                actions.read("reel_pull_strength", NON_NEGATIVE_DOUBLE),
                actions.read("reel_attacker_bonus", INTEGER),
                actions.read("reel_defender_bonus", INTEGER));
    }
}
