package studio.modroll.initiative.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.action.ActionFeedback;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;

class ActionCommandTreeTest {

    private static Action action(String namespace, String path, String literal) {
        return Action.builder(ResourceLocation.fromNamespaceAndPath(namespace, path))
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.self())
                .commandLiteral(literal)
                .effect(context -> ActionResult.performed())
                .build();
    }

    @AfterEach
    void clear() {
        ActionRegistry.clearForTesting();
    }

    @Test
    void ownNamespaceUsesTheBarePath() {
        assertEquals("shove", InitiativeCommands.literalFor(action("initiative", "shove", null)));
    }

    @Test
    void foreignNamespaceKeepsItsPrefix() {
        assertEquals("examplemod:taunt", InitiativeCommands.literalFor(action("examplemod", "taunt", null)));
    }

    @Test
    void aDeclaredLiteralWins() {
        assertEquals("endturn", InitiativeCommands.literalFor(action("initiative", "end_turn", "endturn")));
    }

    @Test
    void everyStatusHasARejectionLine() {
        for (ActionStatus status : ActionStatus.values()) {
            assertNotNull(ActionFeedback.rejectionKey(status), status + " needs a rejection line");
        }
    }
}
