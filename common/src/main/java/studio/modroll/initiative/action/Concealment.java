package studio.modroll.initiative.action;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * Which participants mob AI may not target, because they are hidden. State is a transient
 * server-side UUID set — nothing is written to the entity, so a restart can never leave a player
 * permanently unseeable — reconciled from {@link EncounterFlags} every tick, the same discipline
 * {@code AiFreeze} follows.
 *
 * <p>The loaders gate their target hook on {@link #suppressesTarget}, which refuses the assignment.
 * Refusing is not enough on its own: a refused {@code Mob.setTarget} leaves the previous value in
 * place, and {@code TargetGoal.canContinueToUse} re-asserts its remembered target every tick, so a
 * mob that acquired the hider before it hid would keep it forever. {@link #reconcile} therefore also
 * clears targets already held — the goal-driven {@code target} field and the brain's
 * {@code ATTACK_TARGET} memory alike, since brain mobs never go through {@code Mob.setTarget}.
 *
 * <p>A fight in which every player is hiding is over: the mobs have lost track of them. Those
 * players stay out of mob targeting after it — see {@link #loseTrack} — until they give themselves
 * away or walk off.
 */
public final class Concealment {

    /**
     * Concurrent for the reason {@code AiFreeze}'s set is: the gate is read from any mob's targeting
     * decision server-wide while the encounter reconcile writes.
     */
    private static final Set<UUID> CONCEALED = ConcurrentHashMap.newKeySet();

    /** Players a fight lost track of, read by the same gate from the same threads as {@link #CONCEALED}. */
    private static final Map<UUID, LostTrack> LOST = new ConcurrentHashMap<>();

    /**
     * Where the fight that lost track of a player stood, and the player's two combat timestamps as it
     * ended: either one moving on means it has attacked something or been hurt by a mob since.
     */
    private record LostTrack(ResourceKey<Level> dimension, Vec3 center, int lastHurtMob, int lastHurtByMob) {}

    private Concealment() {}

    public static boolean isConcealed(UUID participant) {
        return CONCEALED.contains(participant) || LOST.containsKey(participant);
    }

    public static Set<UUID> concealedUuids() {
        return Set.copyOf(CONCEALED);
    }

    public static void conceal(UUID participant) {
        CONCEALED.add(participant);
    }

    public static void reveal(UUID participant) {
        CONCEALED.remove(participant);
    }

    public static void clear() {
        CONCEALED.clear();
        LOST.clear();
    }

    public static void forget(ServerLevel level) {
        LOST.values().removeIf(lost -> lost.dimension().equals(level.dimension()));
    }

    /**
     * The loaders' target gate: true means the mob must not take this target. The config is read on
     * every call rather than trusted from the last reconcile, so turning the toggle off restores
     * ordinary targeting within the same tick.
     */
    public static boolean suppressesTarget(LivingEntity target) {
        return target != null && suppressesTarget(target.getUUID());
    }

    public static boolean suppressesTarget(UUID target) {
        return (!CONCEALED.isEmpty() || !LOST.isEmpty()) && isConcealed(target) && active(InitiativeConfig.actions());
    }

    /**
     * Whether {@code participant} is hidden from the other side right now. A hider is no target at
     * all: no mob turn picks it, and no reactor can see it leave its reach.
     */
    public static boolean hides(Encounter encounter, UUID participant) {
        return active(InitiativeConfig.actions()) && encounter.flags().isHidden(participant);
    }

    /** Every player in the fight is hiding, which leaves the mobs nobody to fight. */
    public static boolean everyPlayerHidden(Encounter encounter) {
        List<UUID> players = encounter.participants().stream()
                .filter(participant -> encounter.side(participant) == Encounter.Side.PLAYER)
                .toList();
        return !players.isEmpty() && players.stream().allMatch(player -> hides(encounter, player));
    }

    /**
     * The fight is ending because every player in it is hiding: its mobs drop them as targets and
     * let go of any anger at them, and the players stay out of mob targeting from here. That lasts
     * until a player attacks something, a mob hurts it, it joins another fight, or it walks out past
     * {@code leave_radius} from where the fight stood — out there, nobody was looking for it.
     */
    public static void loseTrack(ServerLevel level, Encounter encounter) {
        for (UUID participant : encounter.participants()) {
            if (encounter.side(participant) == Encounter.Side.PLAYER
                    && level.getEntity(participant) instanceof Player player) {
                LOST.put(
                        participant,
                        new LostTrack(
                                level.dimension(),
                                encounter.center(),
                                player.getLastHurtMobTimestamp(),
                                player.getLastHurtByMobTimestamp()));
            }
        }
        for (UUID participant : encounter.participants()) {
            if (level.getEntity(participant) instanceof Mob mob) {
                forgetLostPlayers(mob);
            }
        }
    }

    /**
     * Ends the lost track of every player that gave itself away or left, and takes the lost players
     * away from any mob around the old fight that picked one up anyway — a brain mob on Fabric, which
     * the target gate cannot reach.
     */
    public static void reconcileLostTrack(ServerLevel level) {
        if (LOST.isEmpty()) {
            return;
        }
        double radius = InitiativeConfig.encounters().leaveRadius();
        boolean active = active(InitiativeConfig.actions());
        LOST.entrySet()
                .removeIf(entry -> entry.getValue().dimension().equals(level.dimension())
                        && (!active || !stillLost(level, entry.getKey(), entry.getValue(), radius)));
        for (LostTrack lost : LOST.values()) {
            if (lost.dimension().equals(level.dimension())) {
                AABB around = AABB.ofSize(lost.center(), radius * 2, radius * 2, radius * 2);
                level.getEntitiesOfClass(Mob.class, around).forEach(Concealment::forgetLostPlayers);
            }
        }
    }

    private static boolean stillLost(ServerLevel level, UUID id, LostTrack lost, double radius) {
        return level.getEntity(id) instanceof Player player
                && player.isAlive()
                && player.position().distanceToSqr(lost.center()) <= radius * radius
                && player.getLastHurtMobTimestamp() == lost.lastHurtMob()
                && player.getLastHurtByMobTimestamp() == lost.lastHurtByMob()
                && EncounterManager.encounterContaining(level, id).isEmpty();
    }

    private static void forgetLostPlayers(Mob mob) {
        dropConcealedTarget(mob);
        if (mob instanceof NeutralMob neutral
                && neutral.getPersistentAngerTarget() != null
                && LOST.containsKey(neutral.getPersistentAngerTarget())) {
            neutral.stopBeingAngry();
        }
    }

    /**
     * Mirrors the encounter's hidden flags into the gate's set and takes away the targets mobs
     * already hold on a hidden participant. Reconciliation rather than transition events means a
     * broken hidden state, a removed participant or a flipped config flag all heal within one tick.
     */
    public static void reconcile(ServerLevel level, Encounter encounter) {
        boolean active = active(InitiativeConfig.actions());
        for (UUID participant : encounter.participants()) {
            if (active && encounter.flags().isHidden(participant)) {
                conceal(participant);
            } else {
                reveal(participant);
            }
        }
        if (!active || !encounter.flags().anyHidden()) {
            return;
        }
        for (UUID participant : encounter.participants()) {
            if (level.getEntity(participant) instanceof Mob mob) {
                dropConcealedTarget(mob);
            }
        }
    }

    /**
     * Whether the encounter's "a neutral that lost its target has calmed down" leave path must stand
     * down: while someone is hiding it was concealment that took the target, not the mob losing
     * interest, and dropping it would eject it from the fight the hider is still in.
     */
    public static boolean holdsNeutralsInTheFight(Encounter encounter) {
        return active(InitiativeConfig.actions()) && encounter.flags().anyHidden();
    }

    private static boolean active(ActionConfig actions) {
        return actions.hide() && actions.hideSuppressesTargeting();
    }

    /** The null write is never suppressed, so clearing cannot be refused by the gate it feeds. */
    private static void dropConcealedTarget(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target != null && isConcealed(target.getUUID())) {
            mob.setTarget(null);
        }
        Brain<?> brain = mob.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && brain.getMemory(MemoryModuleType.ATTACK_TARGET)
                        .filter(attacked -> isConcealed(attacked.getUUID()))
                        .isPresent()) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }
}
