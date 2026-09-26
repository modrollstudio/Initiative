package studio.modroll.initiative.roll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.config.RollAnimationConfig;
import studio.modroll.initiative.roll.RollAnimation.Emphasis;
import studio.modroll.initiative.roll.RollFrame.Die;
import studio.modroll.initiative.roll.RollFrame.Side;

class RollFrameTest {

    private static final String ROLLER = "NightsHigh";
    private static final String OPPONENT = "Husk";
    private static final RollAnimationConfig CONFIG = new RollAnimationConfig(true, 10, 10, 2, true, true);

    private static RollAnimation advantage() {
        return RollAnimation.initiative(new RollDetail(RollMode.ADVANTAGE, 17, OptionalInt.of(4)), ROLLER);
    }

    @Test
    void tumblingDiceShowCyclingFacesAndNoResultYet() {
        RollFrame frame = RollFrame.at(advantage(), 0, CONFIG);
        assertFalse(frame.finished());
        for (Die die : frame.dice()) {
            assertFalse(die.settled());
            assertEquals(Emphasis.NONE, die.emphasis());
            assertTrue(die.face() >= 1 && die.face() <= 20, "a tumbling face must be a d20 face");
        }
        assertNotEquals(
                frame.dice().getFirst().face(),
                RollFrame.at(advantage(), CONFIG.faceChangeTicks(), CONFIG)
                        .dice()
                        .getFirst()
                        .face());
    }

    @Test
    void facesHoldStillBetweenFaceChanges() {
        assertEquals(
                RollFrame.at(advantage(), 0, CONFIG).dice().getFirst().face(),
                RollFrame.at(advantage(), 1, CONFIG).dice().getFirst().face());
    }

    @Test
    void settledAdvantageKeepsTheHigherAndDimsTheDropped() {
        List<Die> dice = RollFrame.at(advantage(), CONFIG.tumbleTicks(), CONFIG).dice();
        assertEquals(2, dice.size());
        assertEquals(new Die(Side.ACTOR, 17, true, true, Emphasis.NONE, ROLLER), dice.get(0));
        assertEquals(new Die(Side.ACTOR, 4, false, true, Emphasis.NONE, ROLLER), dice.get(1));
    }

    @Test
    void settledDisadvantageKeepsTheLower() {
        RollAnimation animation =
                RollAnimation.initiative(new RollDetail(RollMode.DISADVANTAGE, 4, OptionalInt.of(17)), ROLLER);
        List<Die> dice = RollFrame.at(animation, CONFIG.tumbleTicks(), CONFIG).dice();
        assertEquals(4, dice.get(0).face());
        assertTrue(dice.get(0).kept());
        assertEquals(17, dice.get(1).face());
        assertFalse(dice.get(1).kept());
    }

    @Test
    void normalRollSettlesToOneDie() {
        List<Die> dice = RollFrame.at(
                        RollAnimation.initiative(RollDetail.normal(12), ROLLER), CONFIG.tumbleTicks(), CONFIG)
                .dice();
        assertEquals(List.of(new Die(Side.ACTOR, 12, true, true, Emphasis.NONE, ROLLER)), dice);
    }

    @Test
    void contestSettlesBothSides() {
        ContestResult result = new ContestResult(20, 20, 3, 3, RollDetail.normal(20), RollDetail.normal(3));
        List<Die> dice = RollFrame.at(RollAnimation.contest(result, ROLLER, OPPONENT), CONFIG.tumbleTicks(), CONFIG)
                .dice();
        assertEquals(
                List.of(
                        new Die(Side.ACTOR, 20, true, true, Emphasis.CRIT, ROLLER),
                        new Die(Side.OPPONENT, 3, true, true, Emphasis.NONE, OPPONENT)),
                dice);
    }

    @Test
    void emphasisOnlyLandsOnceTheDieHasSettled() {
        RollAnimation animation = RollAnimation.initiative(RollDetail.normal(20), ROLLER);
        assertEquals(
                Emphasis.NONE,
                RollFrame.at(animation, CONFIG.tumbleTicks() - 1, CONFIG)
                        .dice()
                        .getFirst()
                        .emphasis());
        assertEquals(
                Emphasis.CRIT,
                RollFrame.at(animation, CONFIG.tumbleTicks(), CONFIG)
                        .dice()
                        .getFirst()
                        .emphasis());
    }

    @Test
    void theFrameIsFinishedOnceTheHoldIsOver() {
        RollAnimation animation = RollAnimation.initiative(RollDetail.normal(12), ROLLER);
        assertFalse(RollFrame.at(animation, CONFIG.totalTicks() - 1, CONFIG).finished());
        assertTrue(RollFrame.at(animation, CONFIG.totalTicks(), CONFIG).finished());
    }
}
