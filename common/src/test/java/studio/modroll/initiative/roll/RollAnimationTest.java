package studio.modroll.initiative.roll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.util.OptionalInt;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import studio.modroll.critfall.api.combat.AttackOutcome;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.roll.RollAnimation.Emphasis;
import studio.modroll.initiative.roll.RollAnimation.Kind;

class RollAnimationTest {

    private static final String ROLLER = "NightsHigh";
    private static final String OPPONENT = "Husk";

    private static AttackResult attackResult(AttackOutcome outcome, RollDetail roll) {
        return new AttackResult(outcome, roll.kept(), roll.kept() + 3, 14, 0, 5, roll);
    }

    @Test
    void advantageCarriesBothNaturalsAndTheKeptOne() {
        RollDetail roll = new RollDetail(RollMode.ADVANTAGE, 17, OptionalInt.of(4));
        RollAnimation animation = RollAnimation.attack(attackResult(AttackOutcome.HIT, roll), ROLLER);
        assertEquals(Kind.ATTACK, animation.kind());
        assertEquals(RollMode.ADVANTAGE, animation.actor().detail().mode());
        assertEquals(17, animation.actor().detail().kept());
        assertEquals(OptionalInt.of(4), animation.actor().detail().dropped());
        assertTrue(animation.actor().detail().hasTwoDice());
        assertTrue(animation.opponent().isEmpty());
    }

    @Test
    void disadvantageCarriesBothNaturalsAndTheKeptOne() {
        RollDetail roll = new RollDetail(RollMode.DISADVANTAGE, 4, OptionalInt.of(17));
        RollAnimation animation = RollAnimation.attack(attackResult(AttackOutcome.MISS, roll), ROLLER);
        assertEquals(RollMode.DISADVANTAGE, animation.actor().detail().mode());
        assertEquals(4, animation.actor().detail().kept());
        assertEquals(OptionalInt.of(17), animation.actor().detail().dropped());
    }

    @Test
    void normalCarriesASingleDie() {
        RollAnimation animation = RollAnimation.attack(attackResult(AttackOutcome.HIT, RollDetail.normal(12)), ROLLER);
        assertEquals(12, animation.actor().detail().kept());
        assertEquals(OptionalInt.empty(), animation.actor().detail().dropped());
        assertFalse(animation.actor().detail().hasTwoDice());
    }

    @Test
    void critAndFumbleOutcomesAreFlaggedForEmphasis() {
        assertEquals(
                Emphasis.CRIT,
                RollAnimation.attack(attackResult(AttackOutcome.CRIT, RollDetail.normal(20)), ROLLER)
                        .actor()
                        .emphasis());
        assertEquals(
                Emphasis.FUMBLE,
                RollAnimation.attack(attackResult(AttackOutcome.FUMBLE, RollDetail.normal(1)), ROLLER)
                        .actor()
                        .emphasis());
        assertEquals(
                Emphasis.NONE,
                RollAnimation.attack(attackResult(AttackOutcome.HIT, RollDetail.normal(15)), ROLLER)
                        .actor()
                        .emphasis());
    }

    @Test
    void contestCarriesBothSidesDetail() {
        ContestResult result = new ContestResult(
                20, 20, 3, 3, RollDetail.normal(20), new RollDetail(RollMode.DISADVANTAGE, 3, OptionalInt.of(9)));
        RollAnimation animation = RollAnimation.contest(result, ROLLER, OPPONENT);
        assertEquals(Kind.CONTEST, animation.kind());
        assertEquals(20, animation.actor().detail().kept());
        assertTrue(animation.opponent().isPresent());
        assertEquals(3, animation.opponent().orElseThrow().detail().kept());
        assertEquals(
                OptionalInt.of(9), animation.opponent().orElseThrow().detail().dropped());
    }

    @Test
    void contestEmphasisComesFromEachSidesKeptNatural() {
        ContestResult result = new ContestResult(20, 20, 1, 1, RollDetail.normal(20), RollDetail.normal(1));
        RollAnimation animation = RollAnimation.contest(result, ROLLER, OPPONENT);
        assertEquals(Emphasis.CRIT, animation.actor().emphasis());
        assertEquals(Emphasis.FUMBLE, animation.opponent().orElseThrow().emphasis());
    }

    @Test
    void initiativeRollIsASingleUnopposedDie() {
        RollAnimation animation = RollAnimation.initiative(RollDetail.normal(20), ROLLER);
        assertEquals(Kind.INITIATIVE, animation.kind());
        assertEquals(20, animation.actor().detail().kept());
        assertEquals(Emphasis.CRIT, animation.actor().emphasis());
        assertTrue(animation.opponent().isEmpty());
    }

    @Test
    void aSkillCheckIsASingleUnopposedDieThatSurvivesTheWire() {
        RollAnimation animation = RollAnimation.check(RollDetail.normal(12), ROLLER);
        assertEquals(Kind.CHECK, animation.kind());
        assertEquals(12, animation.actor().detail().kept());
        assertTrue(animation.opponent().isEmpty());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RollAnimation.STREAM_CODEC.encode(buf, animation);
        assertEquals(animation, RollAnimation.STREAM_CODEC.decode(buf));
    }

    /** Everyone in the encounter watches, so the wire must say whose die each one is. */
    @Test
    void everySidesRollerSurvivesTheWire() {
        ContestResult result = new ContestResult(20, 20, 3, 3, RollDetail.normal(20), RollDetail.normal(3));
        RollAnimation animation = RollAnimation.contest(result, ROLLER, OPPONENT);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RollAnimation.STREAM_CODEC.encode(buf, animation);
        RollAnimation decoded = RollAnimation.STREAM_CODEC.decode(buf);
        assertEquals(animation, decoded);
        assertEquals(ROLLER, decoded.actor().roller());
        assertEquals(OPPONENT, decoded.opponent().orElseThrow().roller());
    }
}
