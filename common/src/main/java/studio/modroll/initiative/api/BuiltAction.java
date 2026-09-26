package studio.modroll.initiative.api;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** What {@link Action.Builder} produces. Suppliers rather than values, so a config toggle is read per invocation. */
record BuiltAction(
        ResourceLocation id,
        Supplier<ActionCost> costSupplier,
        DoubleSupplier movementCostSupplier,
        ActionTargeting targeting,
        BooleanSupplier enabledSupplier,
        Predicate<LivingEntity> availableToPredicate,
        Function<ActionContext, ActionResult> effect,
        String successKey,
        String failureKey,
        String commandLiteral)
        implements Action {

    @Override
    public ActionCost cost() {
        return costSupplier.get();
    }

    @Override
    public boolean enabled() {
        return enabledSupplier.getAsBoolean();
    }

    @Override
    public boolean availableTo(LivingEntity actor) {
        return availableToPredicate.test(actor);
    }

    @Override
    public double movementCost() {
        return movementCostSupplier.getAsDouble();
    }

    @Override
    public ActionResult perform(ActionContext context) {
        return effect.apply(context);
    }
}
