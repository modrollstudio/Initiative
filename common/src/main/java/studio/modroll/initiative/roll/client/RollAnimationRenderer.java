package studio.modroll.initiative.roll.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import studio.modroll.initiative.roll.RollAnimation.Emphasis;
import studio.modroll.initiative.roll.RollFrame;

/**
 * Draws the tumbling d20 above the hotbar: one die per roll, left to right, the actor's dice first.
 * Each die is a hexagon — the silhouette of an icosahedron — carrying nothing but its number, so
 * the face stays legible at 24 pixels. A dropped die (the one advantage or disadvantage discarded)
 * is greyed; a settled crit or fumble gets its own colour and a small shake. The roller's name sits
 * under its side's dice, since every player is shown the whole encounter's rolls. Everything else
 * about the roll — the modifier, the target AC, hit or miss — stays in Critfall's own text readout.
 */
public final class RollAnimationRenderer {

    private static final int DIE_SIZE = 24;
    private static final int DIE_GAP = 4;
    private static final int SIDE_GAP = 14;
    /**
     * Clears the action bar: vanilla draws the overlay message at {@code guiHeight - 68} with a
     * backdrop above it, so the dice sit high enough that the readout never lands on them.
     */
    private static final int BOTTOM_MARGIN = 96;

    private static final int SHAKE_PIXELS = 1;

    /** Half the width of the flat top and bottom edges, which is what makes the outline a hexagon. */
    private static final int EDGE_HALF_WIDTH = DIE_SIZE / 4;

    private static final int KEPT_BODY = 0xE8232733;
    private static final int DROPPED_BODY = 0x78232733;
    private static final int KEPT_EDGE = 0xFFE8E8F0;
    private static final int DROPPED_EDGE = 0xFF5A5A66;
    private static final int CRIT_EDGE = 0xFFFFD24A;
    private static final int FUMBLE_EDGE = 0xFFE05252;
    private static final int KEPT_NUMBER = 0xFFFFFFFF;
    private static final int DROPPED_NUMBER = 0xFF8A8A93;
    private static final int ROLLER_NAME = 0xFFC8C8D2;
    private static final int NAME_GAP = 2;
    /** The least space between the two sides' names, so they never read as one word. */
    private static final int NAME_SPACING = 8;

    private static final int MAX_NAME_WIDTH = 64;
    private static final String ELLIPSIS = "…";

    private RollAnimationRenderer() {}

    public static void render(GuiGraphics graphics, RollFrame frame) {
        Font font = Minecraft.getInstance().font;
        int x = (graphics.guiWidth() - totalWidth(frame)) / 2;
        int y = graphics.guiHeight() - BOTTOM_MARGIN - DIE_SIZE;
        RollFrame.Die previous = null;
        int sideLeft = x;
        int nameFloor = Integer.MIN_VALUE;
        for (RollFrame.Die die : frame.dice()) {
            if (previous != null) {
                x += previous.side() == die.side() ? DIE_GAP : SIDE_GAP;
                if (previous.side() != die.side()) {
                    int divider = x - SIDE_GAP / 2;
                    drawRollerName(
                            graphics, font, previous, sideLeft, x - SIDE_GAP, y, nameFloor, divider - NAME_SPACING / 2);
                    sideLeft = x;
                    nameFloor = divider + NAME_SPACING / 2;
                }
            }
            drawDie(graphics, font, die, x, y, frame.elapsedTicks());
            x += DIE_SIZE;
            previous = die;
        }
        if (previous != null) {
            drawRollerName(graphics, font, previous, sideLeft, x, y, nameFloor, Integer.MAX_VALUE);
        }
    }

    /**
     * One name per side, centered under that side's dice and cut short so it never widens the row.
     * A long name is pushed outward rather than past the middle of the gap between the sides, so the
     * two names in a contest never run into each other.
     */
    private static void drawRollerName(
            GuiGraphics graphics, Font font, RollFrame.Die die, int left, int right, int top, int floor, int ceiling) {
        String name = shorten(font, die.roller());
        int width = font.width(name);
        graphics.drawString(
                font,
                name,
                Math.max(floor, Math.min((left + right - width) / 2, ceiling - width)),
                top + DIE_SIZE + NAME_GAP,
                die.side() == RollFrame.Side.ACTOR ? ROLLER_NAME : DROPPED_NUMBER);
    }

    private static String shorten(Font font, String name) {
        if (font.width(name) <= MAX_NAME_WIDTH) {
            return name;
        }
        return font.plainSubstrByWidth(name, MAX_NAME_WIDTH - font.width(ELLIPSIS)) + ELLIPSIS;
    }

    private static int totalWidth(RollFrame frame) {
        int width = 0;
        RollFrame.Die previous = null;
        for (RollFrame.Die die : frame.dice()) {
            if (previous != null) {
                width += previous.side() == die.side() ? DIE_GAP : SIDE_GAP;
            }
            width += DIE_SIZE;
            previous = die;
        }
        return width;
    }

    private static void drawDie(GuiGraphics graphics, Font font, RollFrame.Die die, int x, int y, int elapsedTicks) {
        int shake = shake(die, elapsedTicks);
        int left = x + shake;
        int top = y + shake;
        drawHexagon(graphics, die, left, top);
        String face = String.valueOf(die.face());
        graphics.drawString(
                font,
                face,
                left + (DIE_SIZE - font.width(face)) / 2,
                top + (DIE_SIZE - font.lineHeight) / 2 + 2,
                die.kept() ? KEPT_NUMBER : DROPPED_NUMBER);
    }

    /** Row by row: a filled hexagon body with a one-pixel edge down both slanted sides. */
    private static void drawHexagon(GuiGraphics graphics, RollFrame.Die die, int left, int top) {
        int body = die.kept() ? KEPT_BODY : DROPPED_BODY;
        int edge = edge(die);
        for (int row = 0; row < DIE_SIZE; row++) {
            int half = halfWidth(row);
            int rowLeft = left + DIE_SIZE / 2 - half;
            int rowRight = left + DIE_SIZE / 2 + half;
            graphics.fill(rowLeft, top + row, rowRight, top + row + 1, body);
            if (row == 0 || row == DIE_SIZE - 1) {
                graphics.fill(rowLeft, top + row, rowRight, top + row + 1, edge);
            } else {
                graphics.fill(rowLeft, top + row, rowLeft + 1, top + row + 1, edge);
                graphics.fill(rowRight - 1, top + row, rowRight, top + row + 1, edge);
            }
        }
    }

    private static int halfWidth(int row) {
        int distanceFromEnd = Math.min(row, DIE_SIZE - 1 - row);
        return Math.min(DIE_SIZE / 2, EDGE_HALF_WIDTH + distanceFromEnd);
    }

    /** A settled crit or fumble jitters by a pixel every tick; everything else sits still. */
    private static int shake(RollFrame.Die die, int elapsedTicks) {
        if (die.emphasis() == Emphasis.NONE) {
            return 0;
        }
        return elapsedTicks % 2 == 0 ? SHAKE_PIXELS : -SHAKE_PIXELS;
    }

    private static int edge(RollFrame.Die die) {
        if (!die.kept()) {
            return DROPPED_EDGE;
        }
        return switch (die.emphasis()) {
            case CRIT -> CRIT_EDGE;
            case FUMBLE -> FUMBLE_EDGE;
            case NONE -> KEPT_EDGE;
        };
    }
}
