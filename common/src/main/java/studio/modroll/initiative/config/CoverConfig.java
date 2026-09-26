package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;
import static studio.modroll.initiative.config.ConfigValue.NON_NEGATIVE_DOUBLE;
import static studio.modroll.initiative.config.ConfigValue.NON_NEGATIVE_INTEGER;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Line-of-sight cover and the build economy that feeds it. Cover is positional: every driven attack
 * raycasts the attacker's line to the target and grants the defender an AC bonus by tier. The
 * block-placement fields make building for cover cost turn movement. Each toggle is independent;
 * {@code enabled} off restores the plain uncovered attack, {@code block_placement_costs_movement}
 * off restores free placement.
 */
public record CoverConfig(
        boolean enabled,
        int halfCoverAcBonus,
        int threeQuarterCoverAcBonus,
        double halfCoverThreshold,
        double threeQuarterCoverThreshold,
        boolean totalCoverBlocksAttack,
        boolean blockPlacementCostsMovement,
        double blockPlacementMovementCost) {

    public static CoverConfig fromJson(ConfigSource source) {
        ConfigSection cover = source.section("cover");
        double halfThreshold = cover.read("half_cover_threshold", CoverConfig::fraction);
        double threeQuarterThreshold = cover.read("three_quarter_cover_threshold", CoverConfig::fraction);
        if (halfThreshold > threeQuarterThreshold) {
            String reason = "half_cover_threshold " + halfThreshold + " exceeds three_quarter_cover_threshold "
                    + threeQuarterThreshold;
            halfThreshold = cover.revertToDefault("half_cover_threshold", CoverConfig::fraction, reason);
            threeQuarterThreshold =
                    cover.revertToDefault("three_quarter_cover_threshold", CoverConfig::fraction, reason);
        }
        return new CoverConfig(
                cover.read("enabled", BOOLEAN),
                cover.read("half_cover_ac_bonus", NON_NEGATIVE_INTEGER),
                cover.read("three_quarter_cover_ac_bonus", NON_NEGATIVE_INTEGER),
                halfThreshold,
                threeQuarterThreshold,
                cover.read("total_cover_blocks_attack", BOOLEAN),
                cover.read("block_placement_costs_movement", BOOLEAN),
                cover.read("block_placement_movement_cost", NON_NEGATIVE_DOUBLE));
    }

    private static double fraction(JsonObject cover, String key) {
        double value = ConfigNumbers.finiteDouble(cover, key);
        if (value < 0 || value > 1) {
            throw new JsonSyntaxException(key + " must be within [0, 1] but was " + value);
        }
        return value;
    }
}
