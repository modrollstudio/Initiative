package studio.modroll.initiative.action;

import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.turn.OffTurnRestriction;

/**
 * How far a participant may move in a turn: the configured budget, scaled by its movement speed
 * against its own unmodified walking speed, so Swiftness buys blocks and Slowness costs them. Two
 * modifiers say nothing about the creature and are left out: Initiative's off-turn root, which is
 * still on a player in the tick its turn starts, and vanilla's sprint boost, which only says whether
 * the player happened to be sprinting as the turn came round.
 */
public final class MovementSpeed {

    private static final ResourceLocation SPRINTING = ResourceLocation.withDefaultNamespace("sprinting");

    private MovementSpeed() {}

    /** The movement a full turn gives this participant; Dash and Escape grant the same again. */
    public static double fullBudget(LivingEntity entity) {
        ActionConfig actions = InitiativeConfig.actions();
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (!actions.movementScalesWithSpeed() || speed == null) {
            return actions.movementBudgetBlocks();
        }
        return actions.movementBudgetBlocks() * factor(speed.getBaseValue(), speed.getModifiers());
    }

    /**
     * The multiple of {@code walkingSpeed} the modifiers make, combined the way vanilla combines them:
     * flat additions, then multiples of the sum, then multiples of the total.
     */
    static double factor(double walkingSpeed, Collection<AttributeModifier> modifiers) {
        if (walkingSpeed <= 0.0) {
            return 1.0;
        }
        double added = walkingSpeed + sum(modifiers, AttributeModifier.Operation.ADD_VALUE);
        double speed = added * (1.0 + sum(modifiers, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        for (AttributeModifier modifier : modifiers) {
            if (counts(modifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
                speed *= 1.0 + modifier.amount();
            }
        }
        return Math.max(0.0, speed) / walkingSpeed;
    }

    private static double sum(Collection<AttributeModifier> modifiers, AttributeModifier.Operation operation) {
        return modifiers.stream()
                .filter(modifier -> counts(modifier, operation))
                .mapToDouble(AttributeModifier::amount)
                .sum();
    }

    private static boolean counts(AttributeModifier modifier, AttributeModifier.Operation operation) {
        return modifier.operation() == operation
                && !modifier.id().equals(OffTurnRestriction.ROOT_ID)
                && !modifier.id().equals(SPRINTING);
    }
}
