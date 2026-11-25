package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AttackSourcesTest {

    @Test
    void noSourceMakesOneAttack() {
        assertEquals(1, AttackSources.highest(List.of(), null));
    }

    @Test
    void theHighestAnswerCounts() {
        assertEquals(3, AttackSources.highest(List.of(creature -> 2, creature -> 3, creature -> 1), null));
    }

    @Test
    void neverBelowOne() {
        assertEquals(1, AttackSources.highest(List.of(creature -> 0, creature -> -2), null));
    }
}
