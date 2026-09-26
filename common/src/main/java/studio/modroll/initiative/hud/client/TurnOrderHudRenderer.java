package studio.modroll.initiative.hud.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import studio.modroll.initiative.hud.TurnOrderLayout;
import studio.modroll.initiative.hud.TurnOrderSnapshot;

/**
 * Draws the initiative bar top-center: one cell per entry (icon + name), the current actor
 * highlighted by entity id and carrying the turn countdown, so every player — not only the one
 * acting — sees whose turn it is and how long is left. A big encounter wraps onto further rows
 * instead of running off the screen, and long names are cut to keep cells narrow. Players show their
 * skin face when their entity is loaded; mobs show their spawn-egg item; anything unresolved falls
 * back to name-only.
 */
public final class TurnOrderHudRenderer {

    private static final int TOP_MARGIN = 4;
    private static final int SIDE_MARGIN = 4;
    private static final int CELL_PADDING = 3;
    private static final int CELL_GAP = 2;
    private static final int ROW_GAP = 2;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEXT_GAP = 3;
    private static final int MAX_NAME_WIDTH = 54;
    private static final String ELLIPSIS = "…";
    private static final int BACKGROUND = 0x90101010;
    private static final int HIGHLIGHT_BACKGROUND = 0xC0355E2B;
    private static final int HIGHLIGHT_BORDER = 0xFF7FE05A;
    private static final int NAME_COLOR = 0xFFFFFFFF;

    private TurnOrderHudRenderer() {}

    public static void render(GuiGraphics graphics, TurnOrderSnapshot snapshot) {
        if (snapshot.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        List<String> names = new ArrayList<>();
        List<Integer> widths = new ArrayList<>();
        for (TurnOrderSnapshot.Entry entry : snapshot.entries()) {
            String name = label(font, entry, snapshot);
            names.add(name);
            widths.add(cellWidth(font, name));
        }
        int cellHeight = ICON_SIZE + CELL_PADDING * 2;
        int maxRowWidth = graphics.guiWidth() - SIDE_MARGIN * 2;
        int y = TOP_MARGIN;
        for (TurnOrderLayout.Row row :
                TurnOrderLayout.pack(widths, maxRowWidth, CELL_GAP).rows()) {
            int x = (graphics.guiWidth() - row.width()) / 2;
            for (int index = row.firstIndex(); index < row.firstIndex() + row.count(); index++) {
                TurnOrderSnapshot.Entry entry = snapshot.entries().get(index);
                boolean current = entry.entityId() == snapshot.currentActorId();
                drawCell(graphics, font, entry, names.get(index), x, y, widths.get(index), cellHeight, current);
                x += widths.get(index) + CELL_GAP;
            }
            y += cellHeight + ROW_GAP;
        }
    }

    /** The acting cell carries the countdown, which is how a waiting player reads the turn clock. */
    private static String label(Font font, TurnOrderSnapshot.Entry entry, TurnOrderSnapshot snapshot) {
        String name = shorten(font, entry.displayName());
        if (entry.entityId() != snapshot.currentActorId()) {
            return name;
        }
        return name + " " + snapshot.secondsRemaining() + "s";
    }

    private static String shorten(Font font, String displayName) {
        if (font.width(displayName) <= MAX_NAME_WIDTH) {
            return displayName;
        }
        return font.plainSubstrByWidth(displayName, MAX_NAME_WIDTH - font.width(ELLIPSIS)) + ELLIPSIS;
    }

    private static int cellWidth(Font font, String name) {
        return CELL_PADDING + ICON_SIZE + ICON_TEXT_GAP + font.width(name) + CELL_PADDING;
    }

    private static void drawCell(
            GuiGraphics graphics,
            Font font,
            TurnOrderSnapshot.Entry entry,
            String name,
            int x,
            int y,
            int width,
            int height,
            boolean current) {
        graphics.fill(x, y, x + width, y + height, current ? HIGHLIGHT_BACKGROUND : BACKGROUND);
        if (current) {
            graphics.renderOutline(x, y, width, height, HIGHLIGHT_BORDER);
        }
        drawIcon(graphics, entry, x + CELL_PADDING, y + CELL_PADDING);
        int textY = y + (height - font.lineHeight) / 2 + 1;
        graphics.drawString(font, name, x + CELL_PADDING + ICON_SIZE + ICON_TEXT_GAP, textY, NAME_COLOR);
    }

    private static void drawIcon(GuiGraphics graphics, TurnOrderSnapshot.Entry entry, int x, int y) {
        if (entry.isPlayer()) {
            if (Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getEntity(entry.entityId())
                            instanceof AbstractClientPlayer player) {
                PlayerFaceRenderer.draw(graphics, player.getSkin(), x, y, ICON_SIZE);
            }
            return;
        }
        ItemStack egg = spawnEgg(entry.entityTypeId());
        if (!egg.isEmpty()) {
            graphics.renderItem(egg, x, y);
        }
    }

    private static ItemStack spawnEgg(String entityTypeId) {
        ResourceLocation id = ResourceLocation.tryParse(entityTypeId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ENTITY_TYPE
                .getOptional(id)
                .map(SpawnEggItem::byId)
                .<ItemStack>map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }
}
