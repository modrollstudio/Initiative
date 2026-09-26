package studio.modroll.initiative.api;

/** How an action invocation ended: performed, or the reason it was refused. */
public enum ActionStatus {
    PERFORMED,
    INACTIVE,
    DISABLED,
    NOT_IN_ENCOUNTER,
    NOT_YOUR_TURN,
    NO_ACTION,
    NO_BONUS_ACTION,
    NO_REACTION,
    NO_MOVEMENT,
    INVALID_TARGET,
    OUT_OF_REACH,
    MISSING_ITEM,
    HANDS_FULL,
    TOTAL_COVER
}
