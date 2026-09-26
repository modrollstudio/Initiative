package studio.modroll.initiative.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.config.TurnConfig;

class InitiativeDerivationTest {

    private static final TurnConfig CONFIG = new TurnConfig(true, 100, 100, 20.0, 12, true, true, false, Set.of());

    @Test
    void scalesMovementSpeedAndFloors() {
        assertEquals(2, InitiativeDerivation.bonus(0.1, CONFIG));
        assertEquals(4, InitiativeDerivation.bonus(0.23, CONFIG));
        assertEquals(6, InitiativeDerivation.bonus(0.3, CONFIG));
    }

    @Test
    void clampsToConfiguredMaximum() {
        assertEquals(12, InitiativeDerivation.bonus(5.0, CONFIG));
    }

    @Test
    void neverGoesBelowZero() {
        assertEquals(0, InitiativeDerivation.bonus(-1.0, CONFIG));
        assertEquals(0, InitiativeDerivation.bonus(0.0, CONFIG));
    }
}
