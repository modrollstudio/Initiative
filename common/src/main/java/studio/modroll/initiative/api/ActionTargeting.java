package studio.modroll.initiative.api;

import java.util.function.DoubleSupplier;

/**
 * What an action needs pointed at it. Declared rather than checked inside the effect so the
 * invocation pipeline can reject a bad target before anything happens, and so a UI can know what to
 * prompt for without executing the action.
 */
public sealed interface ActionTargeting {

    enum Side {
        ENEMY,
        ALLY,
        ANY
    }

    record None() implements ActionTargeting {}

    record Self() implements ActionTargeting {}

    record Position() implements ActionTargeting {}

    /** Reach resolves per invocation; NaN or negative means the action has no reach gate. */
    record Entity(Side side, DoubleSupplier reachBlocks) implements ActionTargeting {}

    static ActionTargeting none() {
        return new None();
    }

    static ActionTargeting self() {
        return new Self();
    }

    static ActionTargeting position() {
        return new Position();
    }

    static ActionTargeting entity(Side side) {
        return new Entity(side, () -> Double.NaN);
    }

    static ActionTargeting entity(Side side, DoubleSupplier reachBlocks) {
        return new Entity(side, reachBlocks);
    }
}
