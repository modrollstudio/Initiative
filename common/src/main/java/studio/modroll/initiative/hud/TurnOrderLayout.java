package studio.modroll.initiative.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Packs turn-order cells into rows that fit the screen. A big encounter would otherwise run the bar
 * off both edges, so entries wrap onto further rows in turn order; a cell too wide for a row on its
 * own still gets one, since dropping a participant would be worse than overflowing by a few pixels.
 */
public record TurnOrderLayout(List<Row> rows) {

    /** {@code count} cells starting at {@code firstIndex}, together {@code width} pixels wide. */
    public record Row(int firstIndex, int count, int width) {}

    public static TurnOrderLayout pack(List<Integer> cellWidths, int maxRowWidth, int gap) {
        List<Row> rows = new ArrayList<>();
        int firstIndex = 0;
        int width = 0;
        for (int index = 0; index < cellWidths.size(); index++) {
            int cell = cellWidths.get(index);
            boolean rowStarted = index > firstIndex;
            if (rowStarted && width + gap + cell > maxRowWidth) {
                rows.add(new Row(firstIndex, index - firstIndex, width));
                firstIndex = index;
                width = cell;
            } else {
                width += rowStarted ? gap + cell : cell;
            }
        }
        if (firstIndex < cellWidths.size()) {
            rows.add(new Row(firstIndex, cellWidths.size() - firstIndex, width));
        }
        return new TurnOrderLayout(List.copyOf(rows));
    }
}
