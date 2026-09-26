package studio.modroll.initiative.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ActionBarLayoutTest {

    @Test
    void oneRowIsCentered() {
        ActionBarLayout layout = ActionBarLayout.pack(List.of(40, 40), 200, 180);
        assertEquals(2, layout.slots().size());
        ActionBarLayout.Slot first = layout.slots().get(0);
        ActionBarLayout.Slot second = layout.slots().get(1);
        assertEquals(first.x(), 200 - (second.x() + second.width()), "the row is centered on the screen");
        assertEquals(first.x() + first.width() + 2, second.x());
        assertEquals(first.y(), second.y());
        assertEquals(180 - ActionBarLayout.CELL_HEIGHT, first.y());
    }

    @Test
    void tooManyButtonsWrapUpwards() {
        ActionBarLayout layout = ActionBarLayout.pack(List.of(60, 60, 60), 140, 180);
        assertEquals(3, layout.slots().size());
        assertTrue(layout.slots().get(2).y() > layout.slots().get(0).y(), "the third button starts a second row");
        assertEquals(layout.top(), layout.slots().get(0).y());
        assertTrue(layout.top() < 180 - ActionBarLayout.CELL_HEIGHT, "a wrapped bar reaches further up the screen");
    }

    @Test
    void hitTestFindsTheSlotUnderTheCursor() {
        ActionBarLayout layout = ActionBarLayout.pack(List.of(40, 40), 200, 180);
        ActionBarLayout.Slot second = layout.slots().get(1);
        assertEquals(1, layout.slotAt(second.x() + 1, second.y() + 1));
        assertEquals(-1, layout.slotAt(second.x() + 41, second.y() + 1));
        assertEquals(-1, layout.slotAt(second.x() + 1, second.y() - 1));
    }
}
