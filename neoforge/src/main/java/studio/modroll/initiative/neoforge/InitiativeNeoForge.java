package studio.modroll.initiative.neoforge;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionSettingsLoader;
import studio.modroll.initiative.action.BlockPlacement;
import studio.modroll.initiative.action.Concealment;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.command.InitiativeCommands;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.encounter.Provocation;
import studio.modroll.initiative.hud.TurnOrderClientCache;
import studio.modroll.initiative.hud.TurnOrderPayload;
import studio.modroll.initiative.hud.TurnOrderSnapshot;
import studio.modroll.initiative.hud.TurnOrderSync;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationClientCache;
import studio.modroll.initiative.roll.RollAnimationPayload;
import studio.modroll.initiative.roll.RollAnimationSync;
import studio.modroll.initiative.turn.OffTurnRestriction;
import studio.modroll.initiative.ui.ActionInvoke;
import studio.modroll.initiative.ui.ActionInvokePayload;
import studio.modroll.initiative.ui.ActionUiClientCache;
import studio.modroll.initiative.ui.ActionUiPayload;
import studio.modroll.initiative.ui.ActionUiSnapshot;
import studio.modroll.initiative.ui.ActionUiSync;

/**
 * The NeoForge server-side wiring: every listener here translates one NeoForge event into a call on
 * the loader-agnostic code in {@code common} and does nothing else. Where NeoForge has no event —
 * the mob AI step, the overlay message — a mixin in {@code neoforge.mixin} stands in and says why.
 */
@Mod(Initiative.MOD_ID)
public final class InitiativeNeoForge {

    public InitiativeNeoForge(IEventBus modBus) {
        Initiative.init(FMLPaths.CONFIGDIR.get());
        ChecksIntegration.setPresent(ModList.get().isLoaded(ChecksIntegration.MOD_ID));
        TurnOrderSync.setSender(InitiativeNeoForge::sendTurnOrder);
        RollAnimationSync.setSender(InitiativeNeoForge::sendRollAnimation);
        ActionUiSync.setSender(InitiativeNeoForge::sendActionUi);
        modBus.addListener(InitiativeNeoForge::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onLevelTick);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onServerStopping);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.LeftClickBlock.class, InitiativeNeoForge::onInteract);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, InitiativeNeoForge::onInteract);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickItem.class, InitiativeNeoForge::onInteract);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.EntityInteract.class, InitiativeNeoForge::onInteract);
        NeoForge.EVENT_BUS.addListener(
                PlayerInteractEvent.EntityInteractSpecific.class, InitiativeNeoForge::onInteract);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(InitiativeNeoForge::onAddReloadListeners);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ActionSettingsLoader());
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        TurnOrderPayload.TYPE,
                        TurnOrderPayload.STREAM_CODEC,
                        (payload, context) -> TurnOrderClientCache.accept(payload.snapshot()))
                .playToClient(
                        RollAnimationPayload.TYPE,
                        RollAnimationPayload.STREAM_CODEC,
                        (payload, context) -> RollAnimationClientCache.accept(payload.animation(), Util.getMillis()))
                .playToClient(
                        ActionUiPayload.TYPE,
                        ActionUiPayload.STREAM_CODEC,
                        (payload, context) -> ActionUiClientCache.accept(payload.snapshot()))
                .playToServer(
                        ActionInvokePayload.TYPE, ActionInvokePayload.STREAM_CODEC, InitiativeNeoForge::onActionInvoke);
    }

    private static void onActionInvoke(ActionInvokePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> ActionInvoke.perform(player.serverLevel(), player, payload));
        }
    }

    private static void sendTurnOrder(Player player, TurnOrderSnapshot snapshot) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new TurnOrderPayload(snapshot));
        }
    }

    private static void sendRollAnimation(Player player, RollAnimation animation) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new RollAnimationPayload(animation));
        }
    }

    private static void sendActionUi(Player player, ActionUiSnapshot snapshot) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ActionUiPayload(snapshot));
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            EncounterManager.tick(level);
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            EncounterManager.levelUnloaded(level);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        EncounterManager.serverStopping();
    }

    private static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        ActionEconomy.AttackAttempt attempt = ActionEconomy.attack(level, event.getEntity(), target);
        if (ActionEconomy.leavesToVanilla(attempt.status())) {
            return;
        }
        event.setCanceled(true);
        ActionEconomy.sendRejection(event.getEntity(), attempt.status());
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (ActionEconomy.shouldCancelVanillaDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    /**
     * Two jobs on one event. Refusing a hidden target covers both target types: {@code MOB_TARGET}
     * is the {@code Mob.setTarget} goal path and {@code BEHAVIOR_TARGET} the brain path, which never
     * reaches {@code setTarget} and so has no Fabric counterpart. Provocation stays on
     * {@code MOB_TARGET} alone — that is the call Fabric mixes into, so both loaders report the same
     * transition — and is only asked about a target that was actually allowed.
     */
    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (Concealment.suppressesTarget(event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
            return;
        }
        if (event.getTargetType() == LivingChangeTargetEvent.LivingTargetType.MOB_TARGET) {
            Provocation.mobTargeted(mob, event.getNewAboutToBeSetTarget());
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EncounterManager.playerDisconnected(player.serverLevel(), player.getUUID());
        }
    }

    private static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (OffTurnRestriction.preventsInteraction(level, player)) {
            event.setCanceled(true);
            OffTurnRestriction.sendRefusal(player);
            return;
        }
        if (!BlockPlacement.onPlaceAttempt(level, player)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("initiative.action.no_movement_to_build"), true);
        }
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (OffTurnRestriction.preventsInteraction(level, event.getPlayer())) {
            event.setCanceled(true);
            OffTurnRestriction.sendRefusal(event.getPlayer());
        }
    }

    /** Every way a player reaches into the world off-turn: swinging at a block, using an item, an entity. */
    private static <T extends PlayerInteractEvent & ICancellableEvent> void onInteract(T event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (OffTurnRestriction.preventsInteraction(level, event.getEntity())) {
            event.setCanceled(true);
            OffTurnRestriction.sendRefusal(event.getEntity());
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        InitiativeCommands.register(event.getDispatcher());
    }
}
