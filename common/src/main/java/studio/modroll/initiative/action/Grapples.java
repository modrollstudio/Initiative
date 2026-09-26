package studio.modroll.initiative.action;

import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * The lifetime of a grapple. The condition itself lives on {@link EncounterFlags} beside dodge and
 * hidden; everything that starts, ends or reads one goes through here, so no path can leave a hold
 * on a dead grappler or a participant that has walked out of the encounter.
 */
public final class Grapples {

    private Grapples() {}

    public static boolean isGrappled(LivingEntity entity) {
        Encounter encounter = encounterOf(entity);
        return encounter != null && encounter.flags().isGrappled(entity.getUUID());
    }

    /** Whoever is holding {@code grappled}, or null when it is free or the holder is gone. */
    public static LivingEntity grapplerOf(ServerLevel level, LivingEntity grappled) {
        Encounter encounter =
                EncounterManager.encounterContaining(level, grappled.getUUID()).orElse(null);
        if (encounter == null) {
            return null;
        }
        UUID grappler = encounter.flags().grapplerOf(grappled.getUUID());
        return grappler != null && level.getEntity(grappler) instanceof LivingEntity holder ? holder : null;
    }

    public static void hold(ServerLevel level, LivingEntity target, LivingEntity grappler) {
        EncounterManager.encounterContaining(level, target.getUUID())
                .ifPresent(encounter -> encounter.flags().setGrappled(target.getUUID(), grappler.getUUID()));
    }

    public static void release(ServerLevel level, LivingEntity target) {
        EncounterManager.encounterContaining(level, target.getUUID())
                .ifPresent(encounter -> encounter.flags().releaseGrapple(target.getUUID()));
    }

    /** A held participant has no speed of its own, so its turn starts with no movement budget. */
    static double movementBudgetFor(Encounter encounter, UUID participant) {
        double budget = InitiativeConfig.actions().movementBudgetBlocks();
        return encounter.flags().isGrappled(participant) ? 0.0 : budget;
    }

    /**
     * Per-tick break check: a hold ends when either side is gone or dead, when the grappler has left
     * the encounter, or when the two have ended up further apart than a hold can reach — knockback,
     * a blink or a shove all separate them without anyone spending an action on it.
     */
    public static void reconcile(ServerLevel level, Encounter encounter) {
        double breakDistance = InitiativeConfig.grapple().breakDistanceBlocks();
        for (Map.Entry<UUID, UUID> hold : encounter.flags().grapples().entrySet()) {
            if (broken(level, encounter, hold.getKey(), hold.getValue(), breakDistance)) {
                encounter.flags().releaseGrapple(hold.getKey());
            }
        }
    }

    private static boolean broken(
            ServerLevel level, Encounter encounter, UUID target, UUID grappler, double breakDistance) {
        if (!encounter.contains(grappler) || !encounter.contains(target)) {
            return true;
        }
        if (!(level.getEntity(grappler) instanceof LivingEntity holder) || !holder.isAlive()) {
            return true;
        }
        if (!(level.getEntity(target) instanceof LivingEntity held) || !held.isAlive()) {
            return true;
        }
        double dx = holder.getX() - held.getX();
        double dz = holder.getZ() - held.getZ();
        return dx * dx + dz * dz > breakDistance * breakDistance;
    }

    private static Encounter encounterOf(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return null;
        }
        return EncounterManager.encounterContaining(level, entity.getUUID()).orElse(null);
    }
}
