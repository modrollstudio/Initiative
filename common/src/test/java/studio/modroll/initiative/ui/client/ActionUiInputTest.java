package studio.modroll.initiative.ui.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.action.BuiltinActions;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.ui.ActionInvokePayload;
import studio.modroll.initiative.ui.ActionUiClientCache;
import studio.modroll.initiative.ui.ActionUiSnapshot;

/** The End Turn key sends exactly what the End Turn button sends, and only when the button would. */
class ActionUiInputTest {

    private final List<ActionInvokePayload> sent = new ArrayList<>();

    @BeforeEach
    void captureSends() {
        ActionUiInput.setSender(sent::add);
    }

    @AfterEach
    void reset() {
        ActionUiClientCache.clear();
        ActionUiInput.setSender(payload -> {});
    }

    @Test
    void theKeyEndsYourTurn() {
        ActionUiClientCache.accept(yourTurn(Optional.empty()));
        ActionUiInput.endTurn();
        assertEquals(List.of(ActionInvokePayload.untargeted(BuiltinActions.END_TURN)), sent);
    }

    @Test
    void theKeyDoesNothingOffYourTurn() {
        ActionUiClientCache.accept(ActionUiSnapshot.INACTIVE);
        ActionUiInput.endTurn();
        assertEquals(List.of(), sent);
    }

    @Test
    void theKeyIsRefusedWhereTheButtonIsGreyed() {
        ActionUiClientCache.accept(yourTurn(Optional.of(ActionStatus.INACTIVE)));
        ActionUiInput.endTurn();
        assertEquals(List.of(), sent);
    }

    private static ActionUiSnapshot yourTurn(Optional<ActionStatus> endTurnUnavailable) {
        ActionUiSnapshot.Entry endTurn = new ActionUiSnapshot.Entry(
                BuiltinActions.END_TURN,
                ActionCost.FREE,
                ActionUiSnapshot.TargetKind.NONE,
                endTurnUnavailable,
                List.of());
        return new ActionUiSnapshot(
                true, List.of(endTurn), new ActionUiSnapshot.Budget(true, true, true, 10.0, 10.0), 30, 30);
    }
}
