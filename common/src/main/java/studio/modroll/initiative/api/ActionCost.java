package studio.modroll.initiative.api;

/**
 * The turn resource an action spends. {@code REACTION} is the only cost that is not turn-gated —
 * a reaction is by definition taken outside its owner's turn.
 */
public enum ActionCost {
    ACTION,
    BONUS_ACTION,
    MOVEMENT,
    REACTION,
    FREE
}
