package studio.modroll.initiative.api;

/**
 * The turn resource an action spends. {@code REACTION} is the only cost that is not turn-gated —
 * a reaction is by definition taken outside its owner's turn. {@code ATTACK} is one attack of the
 * turn's Attack action: the first spends the action and opens the rest of its attacks
 * ({@link Attacks}), later ones spend those.
 */
public enum ActionCost {
    ACTION,
    BONUS_ACTION,
    MOVEMENT,
    REACTION,
    FREE,
    ATTACK
}
