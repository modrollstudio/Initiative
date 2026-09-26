package studio.modroll.initiative.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import studio.modroll.initiative.hud.TurnOrderClientCache;
import studio.modroll.initiative.hud.TurnOrderPayload;
import studio.modroll.initiative.hud.client.TurnOrderHudRenderer;
import studio.modroll.initiative.roll.RollAnimationClientCache;
import studio.modroll.initiative.roll.RollAnimationPayload;
import studio.modroll.initiative.roll.client.RollHud;
import studio.modroll.initiative.ui.ActionUiClientCache;
import studio.modroll.initiative.ui.ActionUiPayload;
import studio.modroll.initiative.ui.client.ActionBarRenderer;
import studio.modroll.initiative.ui.client.ActionUiInput;

/**
 * The Fabric client wiring: payload receivers, the HUD render callback, the action-panel key, and
 * the tick that lets a targeting click be consumed before vanilla sees it. The drawing itself lives
 * in {@code common}'s {@code client} packages.
 */
public final class InitiativeFabricClient implements ClientModInitializer {

    private static final KeyMapping ACTION_PANEL = new KeyMapping(
            "key.initiative.action_panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.initiative");

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ACTION_PANEL);
        ActionUiInput.setSender(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(
                TurnOrderPayload.TYPE, (payload, context) -> TurnOrderClientCache.accept(payload.snapshot()));
        ClientPlayNetworking.registerGlobalReceiver(
                RollAnimationPayload.TYPE,
                (payload, context) -> RollAnimationClientCache.accept(payload.animation(), Util.getMillis()));
        ClientPlayNetworking.registerGlobalReceiver(
                ActionUiPayload.TYPE, (payload, context) -> ActionUiClientCache.accept(payload.snapshot()));
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            TurnOrderClientCache.current().ifPresent(snapshot -> TurnOrderHudRenderer.render(graphics, snapshot));
            ActionBarRenderer.renderHud(graphics);
            RollHud.render(graphics);
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ActionUiInput.tick(client);
            while (ACTION_PANEL.consumeClick()) {
                ActionUiInput.openPanel();
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TurnOrderClientCache.clear();
            ActionUiClientCache.clear();
            ActionUiInput.cancel();
            RollHud.clear();
        });
    }
}
