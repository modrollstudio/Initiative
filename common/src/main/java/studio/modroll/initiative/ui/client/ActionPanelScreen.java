package studio.modroll.initiative.ui.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import studio.modroll.initiative.ui.ActionBarLayout;
import studio.modroll.initiative.ui.ActionUiClientCache;
import studio.modroll.initiative.ui.ActionUiSnapshot;

/**
 * Frees the cursor so the bar can be clicked. It draws the same bar the HUD does — the HUD stands
 * down while this is open — and closes the moment the turn does, so a timed-out turn cannot leave a
 * player staring at a panel of dead buttons.
 */
public final class ActionPanelScreen extends Screen {

    ActionPanelScreen() {
        super(Component.translatable("initiative.ui.panel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** No dimming: combat carries on behind the panel and the player must keep watching it. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ActionUiSnapshot snapshot = ActionUiClientCache.current().orElse(null);
        if (snapshot == null) {
            onClose();
            return;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        ActionBarRenderer.render(graphics, snapshot, hovered(snapshot, mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ActionUiSnapshot snapshot = ActionUiClientCache.current().orElse(null);
        if (snapshot == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int index = hovered(snapshot, mouseX, mouseY);
        if (index < 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        ActionUiInput.choose(snapshot.entries().get(index));
        return true;
    }

    private int hovered(ActionUiSnapshot snapshot, double mouseX, double mouseY) {
        ActionBarLayout layout = ActionBarRenderer.layout(width, height, snapshot);
        return layout.slotAt(mouseX, mouseY);
    }
}
