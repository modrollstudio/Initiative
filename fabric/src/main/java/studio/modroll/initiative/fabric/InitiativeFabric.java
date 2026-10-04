package studio.modroll.initiative.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionSettingsLoader;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.command.InitiativeCommands;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.hud.TurnOrderPayload;
import studio.modroll.initiative.hud.TurnOrderSnapshot;
import studio.modroll.initiative.hud.TurnOrderSync;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationPayload;
import studio.modroll.initiative.roll.RollAnimationSync;
import studio.modroll.initiative.turn.OffTurnRestriction;
import studio.modroll.initiative.ui.ActionInvoke;
import studio.modroll.initiative.ui.ActionInvokePayload;
import studio.modroll.initiative.ui.ActionUiPayload;
import studio.modroll.initiative.ui.ActionUiSnapshot;
import studio.modroll.initiative.ui.ActionUiSync;

/**
 * The Fabric server-side wiring: every callback here translates one Fabric event into a call on the
 * loader-agnostic code in {@code common} and does nothing else. Where Fabric has no event — block
 * placement, the mob AI step, target acquisition, the overlay message — a mixin in
 * {@code fabric.mixin} stands in and says why.
 */
public final class InitiativeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Initiative.init(FabricLoader.getInstance().getConfigDir());
        ChecksIntegration.setPresent(FabricLoader.getInstance().isModLoaded(ChecksIntegration.MOD_ID));
        PayloadTypeRegistry.playS2C().register(TurnOrderPayload.TYPE, TurnOrderPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RollAnimationPayload.TYPE, RollAnimationPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ActionUiPayload.TYPE, ActionUiPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ActionInvokePayload.TYPE, ActionInvokePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                ActionInvokePayload.TYPE,
                (payload, context) -> ActionInvoke.perform(context.player().serverLevel(), context.player(), payload));
        TurnOrderSync.setSender(InitiativeFabric::sendTurnOrder);
        RollAnimationSync.setSender(InitiativeFabric::sendRollAnimation);
        ActionUiSync.setSender(InitiativeFabric::sendActionUi);
        ServerTickEvents.END_WORLD_TICK.register(EncounterManager::tick);
        ServerWorldEvents.UNLOAD.register((server, level) -> EncounterManager.levelUnloaded(level));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> EncounterManager.serverStopping());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                EncounterManager.playerDisconnected(handler.player.serverLevel(), handler.player.getUUID()));
        AttackEntityCallback.EVENT.register(InitiativeFabric::onAttackEntity);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !offTurn(world, player));
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> refuseOffTurn(world, player));
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> refuseOffTurn(world, player));
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> refuseOffTurn(world, player));
        UseItemCallback.EVENT.register((player, world, hand) -> offTurn(world, player)
                ? InteractionResultHolder.fail(player.getItemInHand(hand))
                : InteractionResultHolder.pass(player.getItemInHand(hand)));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> !ActionEconomy.shouldCancelVanillaDamage(entity, source));
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> InitiativeCommands.register(dispatcher));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ActionSettingsResourceLoader());
    }

    /** Fabric wants its reload listeners identified; the loading itself is loader-agnostic. */
    private static final class ActionSettingsResourceLoader extends ActionSettingsLoader
            implements IdentifiableResourceReloadListener {

        @Override
        public ResourceLocation getFabricId() {
            return ActionSettingsLoader.ID;
        }
    }

    /** Whether the off-turn hold refuses this reach into the world, telling the player why if so. */
    private static boolean offTurn(Level world, Player player) {
        if (!(world instanceof ServerLevel level) || !OffTurnRestriction.preventsInteraction(level, player)) {
            return false;
        }
        OffTurnRestriction.sendRefusal(player);
        return true;
    }

    private static InteractionResult refuseOffTurn(Level world, Player player) {
        return offTurn(world, player) ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    private static InteractionResult onAttackEntity(
            Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity target)) {
            return InteractionResult.PASS;
        }
        ActionEconomy.AttackAttempt attempt = ActionEconomy.attack(level, player, target);
        if (ActionEconomy.leavesToVanilla(attempt.status())) {
            return InteractionResult.PASS;
        }
        ActionEconomy.sendRejection(player, attempt.status());
        return InteractionResult.FAIL;
    }

    private static void sendTurnOrder(Player player, TurnOrderSnapshot snapshot) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new TurnOrderPayload(snapshot));
        }
    }

    private static void sendRollAnimation(Player player, RollAnimation animation) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new RollAnimationPayload(animation));
        }
    }

    private static void sendActionUi(Player player, ActionUiSnapshot snapshot) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new ActionUiPayload(snapshot));
        }
    }
}
