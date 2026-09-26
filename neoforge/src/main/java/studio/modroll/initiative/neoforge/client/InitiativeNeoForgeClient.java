package studio.modroll.initiative.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.hud.TurnOrderClientCache;
import studio.modroll.initiative.hud.client.TurnOrderHudRenderer;
import studio.modroll.initiative.roll.client.RollHud;
import studio.modroll.initiative.turn.OffTurnRestriction;
import studio.modroll.initiative.ui.ActionUiClientCache;
import studio.modroll.initiative.ui.client.ActionBarRenderer;
import studio.modroll.initiative.ui.client.ActionUiInput;

/**
 * The NeoForge client wiring: HUD layers, the action-panel key, and the tick that lets a targeting
 * click be consumed before vanilla sees it. The drawing itself lives in {@code common}'s
 * {@code client} packages.
 */
@Mod(value = Initiative.MOD_ID, dist = Dist.CLIENT)
public final class InitiativeNeoForgeClient {

    private static final KeyMapping ACTION_PANEL = new KeyMapping(
            "key.initiative.action_panel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.initiative");

    public InitiativeNeoForgeClient(IEventBus modBus) {
        ActionUiInput.setSender(PacketDistributor::sendToServer);
        modBus.addListener(InitiativeNeoForgeClient::onRegisterGuiLayers);
        modBus.addListener(InitiativeNeoForgeClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForgeClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForgeClient::onComputeFovModifier);
    }

    /**
     * Vanilla scales the field of view by the player's movement speed, so the off-turn root — which
     * takes that speed to zero — would zoom the camera all the way in. A held player is standing
     * still, and standing still is exactly the unmodified view.
     */
    private static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (OffTurnRestriction.isRooted(event.getPlayer())) {
            event.setNewFovModifier(1.0f);
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "turn_order"),
                (graphics, deltaTracker) -> TurnOrderClientCache.current()
                        .ifPresent(snapshot -> TurnOrderHudRenderer.render(graphics, snapshot)));
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "action_bar"),
                (graphics, deltaTracker) -> ActionBarRenderer.renderHud(graphics));
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "roll_animation"),
                (graphics, deltaTracker) -> RollHud.render(graphics));
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ACTION_PANEL);
    }

    /** Runs before vanilla reads the keybinds, which is what lets a targeting click be consumed. */
    private static void onClientTick(ClientTickEvent.Pre event) {
        ActionUiInput.tick(Minecraft.getInstance());
        while (ACTION_PANEL.consumeClick()) {
            ActionUiInput.openPanel();
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TurnOrderClientCache.clear();
        ActionUiClientCache.clear();
        ActionUiInput.cancel();
        RollHud.clear();
    }
}
