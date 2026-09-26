package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import studio.modroll.critfall.api.dice.RollMode;

class ActionEconomyTest {

    @Test
    void neitherAdvantageNorDisadvantageRollsNormally() {
        assertEquals(RollMode.NORMAL, ActionEconomy.resolveMode(false, false));
    }

    @Test
    void helpAdvantageAloneRollsWithAdvantage() {
        assertEquals(RollMode.ADVANTAGE, ActionEconomy.resolveMode(true, false));
    }

    @Test
    void dodgeDisadvantageAloneRollsWithDisadvantage() {
        assertEquals(RollMode.DISADVANTAGE, ActionEconomy.resolveMode(false, true));
    }

    @Test
    void advantageAndDisadvantageCancelToNormal() {
        assertEquals(RollMode.NORMAL, ActionEconomy.resolveMode(true, true));
    }
}
