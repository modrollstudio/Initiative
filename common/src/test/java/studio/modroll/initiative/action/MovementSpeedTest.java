package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.turn.OffTurnRestriction;

/** Vanilla's own amounts: Swiftness and Slowness are +20% and -15% of the total per level. */
class MovementSpeedTest {

    private static final double WALKING = 0.1;
    private static final double EPSILON = 1e-9;

    @Test
    void walkingSpeedIsTheConfiguredBudget() {
        assertEquals(1.0, MovementSpeed.factor(WALKING, List.of()), EPSILON);
    }

    @Test
    void swiftnessBuysBlocks() {
        assertEquals(1.2, MovementSpeed.factor(WALKING, List.of(effect("speed", 0.2))), EPSILON);
        assertEquals(1.4, MovementSpeed.factor(WALKING, List.of(effect("speed", 0.4))), EPSILON);
    }

    @Test
    void slownessCostsBlocks() {
        assertEquals(0.85, MovementSpeed.factor(WALKING, List.of(effect("slowness", -0.15))), EPSILON);
    }

    /** The root is on a player in the tick its turn starts; reading it would start the turn with nothing. */
    @Test
    void theOffTurnRootIsNotTheCreaturesSpeed() {
        AttributeModifier root =
                new AttributeModifier(OffTurnRestriction.ROOT_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
        assertEquals(1.2, MovementSpeed.factor(WALKING, List.of(root, effect("speed", 0.2))), EPSILON);
    }

    @Test
    void sprintingAsTheTurnStartsBuysNothing() {
        AttributeModifier sprint = new AttributeModifier(
                ResourceLocation.withDefaultNamespace("sprinting"), 0.3, Operation.ADD_MULTIPLIED_TOTAL);
        assertEquals(1.0, MovementSpeed.factor(WALKING, List.of(sprint)), EPSILON);
    }

    @Test
    void modifiersCombineTheWayVanillaCombinesThem() {
        List<AttributeModifier> modifiers = List.of(
                new AttributeModifier(id("flat"), 0.1, Operation.ADD_VALUE),
                new AttributeModifier(id("base"), 0.5, Operation.ADD_MULTIPLIED_BASE),
                effect("speed", 0.2));
        assertEquals((0.2 * 1.5 * 1.2) / WALKING, MovementSpeed.factor(WALKING, modifiers), EPSILON);
    }

    @Test
    void aSpeedTakenBelowZeroLeavesNoMovement() {
        assertEquals(0.0, MovementSpeed.factor(WALKING, List.of(effect("slowness", -1.5))), EPSILON);
    }

    @Test
    void aCreatureWithNoWalkingSpeedKeepsTheConfiguredBudget() {
        assertEquals(1.0, MovementSpeed.factor(0.0, List.of(effect("speed", 0.2))), EPSILON);
    }

    private static AttributeModifier effect(String name, double amount) {
        return new AttributeModifier(
                ResourceLocation.withDefaultNamespace("effect." + name), amount, Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
