package studio.modroll.initiative.roll.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.RollAnimationConfig;
import studio.modroll.initiative.roll.RollAnimationClientCache;
import studio.modroll.initiative.roll.RollReadoutHold;

/** The client's whole roll presentation for one frame: draw the dice, then reveal a held readout. */
public final class RollHud {

    private RollHud() {}

    public static void render(GuiGraphics graphics) {
        RollAnimationConfig config = InitiativeConfig.rollAnimation();
        long now = Util.getMillis();
        RollAnimationClientCache.frame(now, config).ifPresent(frame -> RollAnimationRenderer.render(graphics, frame));
        RollReadoutHold.release(now, config)
                .ifPresent(line -> Minecraft.getInstance().gui.setOverlayMessage(line, false));
    }

    public static void clear() {
        RollAnimationClientCache.clear();
        RollReadoutHold.clear();
    }
}
