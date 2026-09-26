package studio.modroll.initiative.api;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * One thing a participant can do on (or, for reactions, off) its turn. Register through
 * {@link ActionRegistry}; the invocation pipeline gates, targets, runs and charges every action the
 * same way, so a mod's action is indistinguishable from a built-in.
 */
public interface Action {

    ResourceLocation id();

    /** Queried per invocation, so a config toggle can change what an action costs. */
    ActionCost cost();

    ActionTargeting targeting();

    /** Queried per invocation: false makes the action unavailable without disturbing the rest. */
    boolean enabled();

    /**
     * Whether this actor may take the action at all right now — a condition, not a resource. False
     * hides it from that actor's UI and refuses the invocation, the way Escape exists only for a
     * grappled participant.
     */
    default boolean availableTo(LivingEntity actor) {
        return true;
    }

    ActionResult perform(ActionContext context);

    /** Blocks of movement charged when {@link #cost()} is {@link ActionCost#MOVEMENT}. */
    default double movementCost() {
        return 0.0;
    }

    /** Translation key for a performed action whose effect landed; null falls back to a generic line. */
    default String successKey() {
        return null;
    }

    /** Translation key for a performed action whose effect missed; null falls back to {@link #successKey()}. */
    default String failureKey() {
        return null;
    }

    /** Command literal override; null derives it from the id. */
    default String commandLiteral() {
        return null;
    }

    static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    final class Builder {

        private final ResourceLocation id;
        private Supplier<ActionCost> cost;
        private DoubleSupplier movementCost = () -> 0.0;
        private ActionTargeting targeting;
        private BooleanSupplier enabled = () -> true;
        private Predicate<LivingEntity> availableTo = actor -> true;
        private Function<ActionContext, ActionResult> effect;
        private String successKey;
        private String failureKey;
        private String commandLiteral;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder cost(ActionCost cost) {
            return cost(() -> cost);
        }

        /** For an action whose cost is a config decision, like Attack under {@code single_attack_per_turn}. */
        public Builder cost(Supplier<ActionCost> cost) {
            this.cost = cost;
            return this;
        }

        public Builder cost(ActionCost cost, DoubleSupplier movementCost) {
            this.movementCost = movementCost;
            return cost(() -> cost);
        }

        public Builder targeting(ActionTargeting targeting) {
            this.targeting = targeting;
            return this;
        }

        public Builder enabled(BooleanSupplier enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder messages(String successKey, String failureKey) {
            this.successKey = successKey;
            this.failureKey = failureKey;
            return this;
        }

        public Builder commandLiteral(String commandLiteral) {
            this.commandLiteral = commandLiteral;
            return this;
        }

        /** For an action only some actors ever have, like escaping a grapple. */
        public Builder availableTo(Predicate<LivingEntity> availableTo) {
            this.availableTo = availableTo;
            return this;
        }

        public Builder effect(Function<ActionContext, ActionResult> effect) {
            this.effect = effect;
            return this;
        }

        public Action build() {
            if (cost == null || targeting == null || effect == null) {
                throw new IllegalStateException("action " + id + " needs a cost, targeting and an effect");
            }
            return new BuiltAction(
                    id,
                    cost,
                    movementCost,
                    targeting,
                    enabled,
                    availableTo,
                    effect,
                    successKey,
                    failureKey,
                    commandLiteral);
        }
    }
}
