package studio.modroll.initiative.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.hud.TurnOrderLayout.Row;

class TurnOrderLayoutTest {

    private static final int GAP = 2;

    @Test
    void noEntriesLayOutNoRows() {
        assertTrue(TurnOrderLayout.pack(List.of(), 100, GAP).rows().isEmpty());
    }

    @Test
    void entriesThatFitStayOnOneRow() {
        TurnOrderLayout layout = TurnOrderLayout.pack(List.of(30, 30, 30), 100, GAP);
        assertEquals(List.of(new Row(0, 3, 94)), layout.rows());
    }

    @Test
    void entriesWrapOntoTheNextRowRatherThanRunningOffScreen() {
        TurnOrderLayout layout = TurnOrderLayout.pack(List.of(40, 40, 40, 40), 100, GAP);
        assertEquals(List.of(new Row(0, 2, 82), new Row(2, 2, 82)), layout.rows());
    }

    @Test
    void anEntryWiderThanTheRowGetsARowToItself() {
        TurnOrderLayout layout = TurnOrderLayout.pack(List.of(30, 150, 30), 100, GAP);
        assertEquals(List.of(new Row(0, 1, 30), new Row(1, 1, 150), new Row(2, 1, 30)), layout.rows());
    }

    @Test
    void everyEntryIsLaidOutExactlyOnce() {
        List<Integer> widths = List.of(25, 60, 45, 30, 70, 20, 55, 35);
        int laidOut = TurnOrderLayout.pack(widths, 160, GAP).rows().stream()
                .mapToInt(Row::count)
                .sum();
        assertEquals(widths.size(), laidOut);
    }
}
