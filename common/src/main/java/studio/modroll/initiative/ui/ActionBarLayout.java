package studio.modroll.initiative.ui;

import java.util.ArrayList;
import java.util.List;
import studio.modroll.initiative.hud.TurnOrderLayout;

/**
 * Places the action buttons above the hotbar, wrapping onto further rows when a pack registers more
 * actions than fit across the screen. Free of client-only imports so the placement — and the hit
 * test the panel screen clicks through — can be reasoned about without a client.
 */
public record ActionBarLayout(List<Slot> slots, int top) {

    public static final int CELL_HEIGHT = 20;
    private static final int CELL_GAP = 2;
    private static final int ROW_GAP = 2;
    private static final int SIDE_MARGIN = 4;

    public record Slot(int index, int x, int y, int width) {

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + CELL_HEIGHT;
        }
    }

    /** {@code bottom} is the y the last row ends at; rows stack upwards from it. */
    public static ActionBarLayout pack(List<Integer> cellWidths, int screenWidth, int bottom) {
        List<TurnOrderLayout.Row> rows = TurnOrderLayout.pack(cellWidths, screenWidth - SIDE_MARGIN * 2, CELL_GAP)
                .rows();
        int top = bottom - rows.size() * (CELL_HEIGHT + ROW_GAP) + ROW_GAP;
        List<Slot> slots = new ArrayList<>();
        int y = top;
        for (TurnOrderLayout.Row row : rows) {
            int x = (screenWidth - row.width()) / 2;
            for (int index = row.firstIndex(); index < row.firstIndex() + row.count(); index++) {
                slots.add(new Slot(index, x, y, cellWidths.get(index)));
                x += cellWidths.get(index) + CELL_GAP;
            }
            y += CELL_HEIGHT + ROW_GAP;
        }
        return new ActionBarLayout(List.copyOf(slots), top);
    }

    public int slotAt(double mouseX, double mouseY) {
        for (Slot slot : slots) {
            if (slot.contains(mouseX, mouseY)) {
                return slot.index();
            }
        }
        return -1;
    }
}
