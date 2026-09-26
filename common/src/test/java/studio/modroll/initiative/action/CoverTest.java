package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import studio.modroll.initiative.action.Cover.Tier;
import studio.modroll.initiative.config.CoverConfig;

class CoverTest {

    private static final CoverConfig COVER = new CoverConfig(true, 2, 5, 0.5, 0.75, true, true, 1.0);

    @Test
    void noObstructionIsNoCover() {
        assertEquals(Tier.NONE, Cover.tierOf(0.0, COVER));
    }

    @Test
    void obstructionBelowHalfThresholdIsNoCover() {
        assertEquals(Tier.NONE, Cover.tierOf(0.375, COVER));
    }

    @Test
    void obstructionAtHalfThresholdIsHalfCover() {
        assertEquals(Tier.HALF, Cover.tierOf(0.5, COVER));
    }

    @Test
    void obstructionBetweenThresholdsIsHalfCover() {
        assertEquals(Tier.HALF, Cover.tierOf(0.625, COVER));
    }

    @Test
    void obstructionAtThreeQuarterThresholdIsThreeQuarterCover() {
        assertEquals(Tier.THREE_QUARTER, Cover.tierOf(0.75, COVER));
    }

    @Test
    void fullObstructionIsTotalCover() {
        assertEquals(Tier.TOTAL, Cover.tierOf(1.0, COVER));
    }

    @Test
    void thresholdsAreConfigDriven() {
        CoverConfig lenient = new CoverConfig(true, 2, 5, 0.25, 0.5, true, true, 1.0);
        assertEquals(Tier.HALF, Cover.tierOf(0.375, lenient));
        assertEquals(Tier.THREE_QUARTER, Cover.tierOf(0.5, lenient));
    }

    @Test
    void acBonusPerTier() {
        assertEquals(0, Cover.acBonus(Tier.NONE, COVER));
        assertEquals(2, Cover.acBonus(Tier.HALF, COVER));
        assertEquals(5, Cover.acBonus(Tier.THREE_QUARTER, COVER));
        assertEquals(5, Cover.acBonus(Tier.TOTAL, COVER));
    }
}
