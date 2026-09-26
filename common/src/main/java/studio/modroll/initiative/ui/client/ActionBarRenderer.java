package studio.modroll.initiative.ui.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.initiative.action.ActionFeedback;
import studio.modroll.initiative.action.BuiltinActions;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.ui.ActionBarLayout;
import studio.modroll.initiative.ui.ActionUiClientCache;
import studio.modroll.initiative.ui.ActionUiSnapshot;

/**
 * Draws the turn's action bar: one button per action the server says this player has, the budget
 * that gates them, and the turn clock. Every label comes from the action's own id, so a pack's
 * action draws itself without a line of code here.
 */
public final class ActionBarRenderer {

    private static final int BOTTOM_MARGIN = 48;
    private static final int CELL_PADDING = 5;
    private static final int MIN_CELL_WIDTH = 34;
    private static final int LINE_GAP = 3;
    private static final int BACKGROUND = 0xC0101010;
    private static final int END_TURN_BACKGROUND = 0xC0402A10;
    private static final int UNAVAILABLE_BACKGROUND = 0x90101010;
    private static final int HOVER_BORDER = 0xFFFFFFFF;
    private static final int LABEL = 0xFFFFFFFF;
    private static final int LABEL_UNAVAILABLE = 0xFF808080;
    private static final int SPENT = 0xFF808080;
    private static final int AVAILABLE = 0xFF7FE05A;
    private static final int CLOCK = 0xFFE0C05A;
    private static final int REASON = 0xFFE05A5A;

    private ActionBarRenderer() {}

    /** The always-on bar; it stands down while the panel screen is up, which draws the same bar. */
    public static void renderHud(GuiGraphics graphics) {
        if (Minecraft.getInstance().screen instanceof ActionPanelScreen) {
            return;
        }
        ActionUiClientCache.current().ifPresent(snapshot -> render(graphics, snapshot, -1));
    }

    public static void render(GuiGraphics graphics, ActionUiSnapshot snapshot, int hovered) {
        Font font = Minecraft.getInstance().font;
        ActionBarLayout layout = layout(graphics.guiWidth(), graphics.guiHeight(), snapshot);
        for (ActionBarLayout.Slot slot : layout.slots()) {
            drawCell(graphics, font, snapshot.entries().get(slot.index()), slot, slot.index() == hovered);
        }
        drawStatusLine(graphics, font, snapshot, layout.top() - font.lineHeight - LINE_GAP);
        drawHint(graphics, font, snapshot, hovered, layout.top() - (font.lineHeight + LINE_GAP) * 2);
    }

    public static ActionBarLayout layout(int screenWidth, int screenHeight, ActionUiSnapshot snapshot) {
        Font font = Minecraft.getInstance().font;
        List<Integer> widths = new ArrayList<>();
        for (ActionUiSnapshot.Entry entry : snapshot.entries()) {
            widths.add(Math.max(MIN_CELL_WIDTH, font.width(label(entry.id())) + CELL_PADDING * 2));
        }
        return ActionBarLayout.pack(widths, screenWidth, screenHeight - BOTTOM_MARGIN);
    }

    /** A pack's own lang file names its action; without one the id's path is shown as written. */
    public static Component label(ResourceLocation id) {
        return Component.translatableWithFallback(
                "action." + id.getNamespace() + "." + id.getPath(), id.getPath().replace('_', ' '));
    }

    private static void drawCell(
            GuiGraphics graphics, Font font, ActionUiSnapshot.Entry entry, ActionBarLayout.Slot slot, boolean hovered) {
        int bottom = slot.y() + ActionBarLayout.CELL_HEIGHT;
        graphics.fill(slot.x(), slot.y(), slot.x() + slot.width(), bottom, background(entry));
        if (hovered) {
            graphics.renderOutline(slot.x(), slot.y(), slot.width(), ActionBarLayout.CELL_HEIGHT, HOVER_BORDER);
        }
        Component label = label(entry.id());
        int x = slot.x() + (slot.width() - font.width(label)) / 2;
        int y = slot.y() + (ActionBarLayout.CELL_HEIGHT - font.lineHeight) / 2 + 1;
        graphics.drawString(font, label, x, y, entry.isAvailable() ? LABEL : LABEL_UNAVAILABLE);
    }

    private static int background(ActionUiSnapshot.Entry entry) {
        if (!entry.isAvailable()) {
            return UNAVAILABLE_BACKGROUND;
        }
        return entry.id().equals(BuiltinActions.END_TURN) ? END_TURN_BACKGROUND : BACKGROUND;
    }

    /** Action / bonus / reaction, what is left of the movement, and the turn clock, in one line. */
    private static void drawStatusLine(GuiGraphics graphics, Font font, ActionUiSnapshot snapshot, int y) {
        ActionUiSnapshot.Budget budget = snapshot.budget();
        Component line = Component.empty()
                .append(pip(budget.action(), "initiative.ui.action"))
                .append(CommonComponents.space())
                .append(pip(budget.bonusAction(), "initiative.ui.bonus_action"))
                .append(CommonComponents.space())
                .append(pip(budget.reaction(), "initiative.ui.reaction"))
                .append(CommonComponents.space())
                .append(Component.translatable(
                                "initiative.ui.movement",
                                format(budget.movementRemaining()),
                                format(budget.movementBudget()))
                        .withColor(budget.movementRemaining() > 0.0 ? AVAILABLE : SPENT))
                .append(CommonComponents.space())
                .append(Component.translatable("initiative.ui.timer", snapshot.turnSecondsRemaining())
                        .withColor(CLOCK));
        graphics.drawString(font, line, (graphics.guiWidth() - font.width(line)) / 2, y, LABEL);
    }

    /** Why the hovered button is greyed, or how to point the pending action. */
    private static void drawHint(GuiGraphics graphics, Font font, ActionUiSnapshot snapshot, int hovered, int y) {
        Component hint = hint(snapshot, hovered);
        if (hint == null) {
            return;
        }
        graphics.drawString(font, hint, (graphics.guiWidth() - font.width(hint)) / 2, y, REASON);
    }

    private static Component hint(ActionUiSnapshot snapshot, int hovered) {
        if (ActionUiInput.isTargeting()) {
            return ActionUiInput.targetingHint();
        }
        if (hovered < 0 || hovered >= snapshot.entries().size()) {
            return null;
        }
        ActionUiSnapshot.Entry entry = snapshot.entries().get(hovered);
        return entry.unavailable()
                .<Component>map(status -> Component.translatable(ActionFeedback.rejectionKey(status)))
                .orElseGet(() -> cost(entry.cost()));
    }

    private static Component cost(ActionCost cost) {
        return Component.translatable("initiative.ui.cost." + cost.name().toLowerCase(Locale.ROOT));
    }

    private static Component pip(boolean available, String key) {
        return Component.translatable(key).withColor(available ? AVAILABLE : SPENT);
    }

    private static String format(double blocks) {
        return String.format(Locale.ROOT, "%.1f", blocks);
    }
}
