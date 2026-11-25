package studio.modroll.initiative.action;

/**
 * One participant's per-turn resources: a movement budget, one action, one bonus action and the
 * attacks its Attack action opened, reset at the start of each turn, with how far into the turn it
 * is driven. Only those acting on the current turn ever have one, which is why the reaction — spent
 * off-turn — lives per-participant on {@link EncounterFlags} instead.
 */
public final class TurnBudget {

    private double movementRemaining;
    private boolean actionAvailable;
    private boolean bonusActionAvailable;
    private int attacksLeft;
    private int ticks;
    private int nextAttackTick;
    private boolean done;

    public void reset(double movementBudget) {
        movementRemaining = movementBudget;
        actionAvailable = true;
        bonusActionAvailable = true;
        attacksLeft = 0;
        ticks = 0;
        nextAttackTick = 0;
        done = false;
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

    /** Whether the Attack action has an attack left: the action itself, or one it opened. */
    public boolean attackAvailable() {
        return actionAvailable || attacksLeft > 0;
    }

    public int attacksLeft() {
        return attacksLeft;
    }

    /** One of the opened attacks, or else the action, which opens the rest of its {@code attacksPerAction}. */
    public void useAttack(int attacksPerAction) {
        if (attacksLeft > 0) {
            attacksLeft--;
            return;
        }
        actionAvailable = false;
        attacksLeft = attacksPerAction - 1;
    }

    /** How far into the turn this tick is, 0 on its first; each call is the next tick. */
    int nextTick() {
        return ticks++;
    }

    boolean attackReadyAt(int tick) {
        return tick >= nextAttackTick;
    }

    void holdAttacksUntil(int tick) {
        nextAttackTick = tick;
    }

    /** A turn handler said its mob is done; the shared turn may still be waiting on the other half. */
    boolean done() {
        return done;
    }

    void finish() {
        done = true;
    }
}
