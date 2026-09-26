package studio.modroll.initiative.turn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * Holds a participant player still while somebody else is acting. A player sends input packets
 * rather than running an AI, so unlike {@link AiFreeze} the hold is two halves: a movement-speed
 * root the client itself obeys, so nothing has to fight the input it sends, and a server-side
 * pull-back that undoes the step of anyone who moves anyway. The root is a transient modifier and
 * the anchor a server-side map, so nothing reaches the entity's saved data and no crash can leave a
 * player rooted. The anchor is the one record of a held player: while it is absent the player is
 * free, so {@link #release} can never leave the root behind on its own.
 */
public final class OffTurnRestriction {

    private static final ResourceLocation ROOT_ID =
            ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "off_turn_root");
    private static final AttributeModifier ROOT =
            new AttributeModifier(ROOT_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private static final Map<UUID, Vec3> ANCHORS = new ConcurrentHashMap<>();

    private OffTurnRestriction() {}

    /** The line that tells a refused player why, where every other action rejection lands. */
    public static void sendRefusal(Player player) {
        player.displayClientMessage(
                Component.translatable("initiative.action.not_your_turn").withStyle(ChatFormatting.RED), true);
    }

    /** The question every loader's break, place, use and interact hook asks before it cancels. */
    public static boolean preventsInteraction(ServerLevel level, Player player) {
        Encounter encounter =
                EncounterManager.encounterContaining(level, player.getUUID()).orElse(null);
        if (encounter == null) {
            return false;
        }
        return holds(InitiativeConfig.turns(), isActing(encounter, player.getUUID()));
    }

    /**
     * Reconciles the hold against the turn every tick, the way the freeze reconciles mobs: a flipped
     * toggle, a swapped turn or a participant that is no longer a player heals within one tick, and
     * a released participant is released whether or not its entity is still there to unroot.
     */
    public static void reconcile(ServerLevel level, Encounter encounter, TurnConfig turns) {
        for (UUID participant : encounter.participants()) {
            if (level.getEntity(participant) instanceof Player player
                    && holds(turns, isActing(encounter, participant))) {
                hold(player);
            } else {
                release(level, participant);
            }
        }
    }

    public static void release(ServerLevel level, UUID participant) {
        if (ANCHORS.remove(participant) == null) {
            return;
        }
        LivingEntity held = heldEntity(level, participant);
        if (held != null) {
            unroot(held);
        }
    }

    public static void clear() {
        ANCHORS.clear();
    }

    public static boolean isHeld(UUID participant) {
        return ANCHORS.containsKey(participant);
    }

    /**
     * Whether the root is on this entity. Unlike {@link #isHeld} this reads the entity itself, and
     * attribute modifiers are synced, so the client can ask it of its own player — which is how the
     * camera keeps its normal field of view while its owner is held.
     */
    public static boolean isRooted(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        return speed != null && speed.hasModifier(ROOT_ID);
    }

    static boolean holds(TurnConfig turns, boolean acting) {
        return turns.enabled() && turns.restrictPlayersOffTurn() && !acting;
    }

    /**
     * Where a held player belongs right now: the anchor's horizontal plane always, and its height
     * too while the player stands in a fluid, where leaving the vertical free means a slow sink. Out
     * of a fluid the height is the player's own — falling and knockback stay free, as they do for
     * the acting participant's movement budget — and the anchor follows it down, so a player who
     * drops into water is held where it entered rather than pulled back up to where the hold began.
     */
    static Vec3 heldPosition(Vec3 anchor, Vec3 current, boolean inFluid) {
        return new Vec3(anchor.x, inFluid ? anchor.y : current.y, anchor.z);
    }

    private static void hold(Player player) {
        UUID id = player.getUUID();
        Vec3 current = player.position();
        Vec3 anchor = ANCHORS.computeIfAbsent(id, key -> current);
        root(player);
        Vec3 held = heldPosition(anchor, current, standsInFluid(player));
        ANCHORS.replace(id, held);
        if (!held.equals(current)) {
            player.teleportTo(held.x, held.y, held.z);
        }
    }

    /**
     * Read from the block the feet are in rather than from the player's own water flags, which are
     * a movement tick behind the teleport that put it there. Any fluid counts: water, lava and a
     * modded one all carry whoever stands in them.
     */
    private static boolean standsInFluid(Player player) {
        return !player.level().getFluidState(player.blockPosition()).isEmpty();
    }

    /**
     * A player who changed dimension keeps the entity that carries the root but has left the level
     * the encounter ticked in, so the player list is asked before giving up on unrooting them.
     */
    private static LivingEntity heldEntity(ServerLevel level, UUID participant) {
        if (level.getEntity(participant) instanceof LivingEntity entity) {
            return entity;
        }
        return level.getServer().getPlayerList().getPlayer(participant);
    }

    private static void root(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !isRooted(entity)) {
            speed.addTransientModifier(ROOT);
        }
    }

    private static void unroot(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ROOT_ID);
        }
    }

    private static boolean isActing(Encounter encounter, UUID participant) {
        return participant.equals(encounter.turnOrder().currentTurn().orElse(null));
    }
}
