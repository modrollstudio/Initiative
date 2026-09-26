package studio.modroll.initiative.turn;

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
}
