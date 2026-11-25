package studio.modroll.initiative.turn;

import studio.modroll.critfall.api.dice.DiceExpression;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.config.TurnConfig;

/**
 * Initiative bonus derived from the movement-speed attribute, mirroring Critfall's derivation
 * philosophy (plausible tabletop stats from vanilla attributes, no per-entity configuration).
 */
public final class InitiativeDerivation {

    private InitiativeDerivation() {}

    /** {@code floor(movementSpeed * initiative_bonus_per_speed)}, clamped to {@code [0, initiative_max_bonus]}. */
    public static int bonus(double movementSpeed, TurnConfig config) {
        int raw = (int) Math.floor(movementSpeed * config.initiativeBonusPerSpeed());
        return Math.clamp(raw, 0, config.initiativeMaxBonus());
    }

    /** Surprise as SRD 5.2 has it: disadvantage on initiative, no lost turn. */
    public static DiceExpression dice(boolean surprised) {
        return mode(surprised).d20Expression();
    }

    public static RollMode mode(boolean surprised) {
        return surprised ? RollMode.DISADVANTAGE : RollMode.NORMAL;
    }
}
