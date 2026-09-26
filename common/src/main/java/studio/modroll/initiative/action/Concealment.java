package studio.modroll.initiative.action;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;

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
 */
public final class Concealment {

    /**
     * Concurrent for the reason {@code AiFreeze}'s set is: the gate is read from any mob's targeting
     * decision server-wide while the encounter reconcile writes.
     */
    private static final Set<UUID> CONCEALED = ConcurrentHashMap.newKeySet();

    private Concealment() {}

    public static boolean isConcealed(UUID participant) {
        return CONCEALED.contains(participant);
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
        return !CONCEALED.isEmpty() && CONCEALED.contains(target) && active(InitiativeConfig.actions());
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
