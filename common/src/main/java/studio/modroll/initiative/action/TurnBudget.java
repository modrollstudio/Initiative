package studio.modroll.initiative.action;

/**
 * One participant's per-turn resources: a movement budget, one action, and one bonus action, reset
 * at the start of each turn. Only the acting participant ever has one, which is why the reaction —
 * spent off-turn — lives per-participant on {@link EncounterFlags} instead.
 */
public final class TurnBudget {

    private double movementRemaining;
    private boolean actionAvailable;
    private boolean bonusActionAvailable;

    public void reset(double movementBudget) {
        movementRemaining = movementBudget;
        actionAvailable = true;
        bonusActionAvailable = true;
    }

    public double movementRemaining() {
        return movementRemaining;
    }

    public void consumeMovement(double distance) {
        movementRemaining = Math.max(0.0, movementRemaining - distance);
    }

    /** Dash: grants extra movement this turn on top of whatever is left. */
    public void addMovement(double distance) {
        movementRemaining += distance;
    }

    public boolean movementExhausted() {
        return movementRemaining <= 0.0;
    }

    public boolean actionAvailable() {
        return actionAvailable;
    }

    public void useAction() {
        actionAvailable = false;
    }

    public boolean bonusActionAvailable() {
        return bonusActionAvailable;
    }

    public void useBonusAction() {
        bonusActionAvailable = false;
    }
}
