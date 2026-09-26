package studio.modroll.initiative.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ActionRegistryTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private static Action stub(String path) {
        return Action.builder(id(path))
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.self())
                .effect(context -> ActionResult.performed())
                .build();
    }

    @AfterEach
    void clear() {
        ActionRegistry.clearForTesting();
    }

    @Test
    void registeredActionIsFoundById() {
        Action action = stub("mark");
        ActionRegistry.register(action);
        assertSame(action, ActionRegistry.get(id("mark")).orElseThrow());
    }

    @Test
    void unknownIdIsEmpty() {
        assertTrue(ActionRegistry.get(id("absent")).isEmpty());
    }

    @Test
    void duplicateIdIsRejected() {
        ActionRegistry.register(stub("mark"));
        assertThrows(IllegalArgumentException.class, () -> ActionRegistry.register(stub("mark")));
    }

    @Test
    void allKeepsRegistrationOrder() {
        Action first = stub("first");
        Action second = stub("second");
        Action third = stub("third");
        ActionRegistry.register(first);
        ActionRegistry.register(second);
        ActionRegistry.register(third);
        assertEquals(List.of(first, second, third), List.copyOf(ActionRegistry.all()));
    }

    @Test
    void builderRequiresCostTargetingAndEffect() {
        assertThrows(
                IllegalStateException.class, () -> Action.builder(id("bare")).build());
    }

    @Test
    void builderDefaultsToEnabled() {
        assertTrue(stub("mark").enabled());
    }
}
