package studio.modroll.initiative.turn;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.config.TurnConfig;

/**
 * Tracks which mobs are frozen because it is not their turn. State is a transient server-side UUID
 * set: nothing is written to the entity, so a restart can never leave a statue. The encounter
 * manager freezes and thaws along the turn state; every encounter exit path must thaw, mirroring
 * the discipline that releases Critfall's combat suppression.
 */
public final class AiFreeze {

    public static final TagKey<EntityType<?>> NO_FREEZE = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "no_freeze"));

    /**
     * Concurrent because {@link #holdIfFrozen} is read from every mob's AI step server-wide while the
     * encounter reconcile writes: vanilla ticks both on the server thread, but a mod that
     * parallelises entity ticking would otherwise read a set mid-resize.
     */
    private static final Set<UUID> FROZEN = ConcurrentHashMap.newKeySet();

    private AiFreeze() {}

    public static void freeze(UUID entity) {
        FROZEN.add(entity);
    }

    public static void thaw(UUID entity) {
        FROZEN.remove(entity);
    }

    public static boolean isFrozen(UUID entity) {
        return FROZEN.contains(entity);
    }

    public static Set<UUID> frozenUuids() {
        return Set.copyOf(FROZEN);
    }

    public static void clear() {
        FROZEN.clear();
    }

    /**
     * The per-tick gate the loader mixins call at the head of every mob's AI step; must stay cheap.
     * A frozen mob makes no decisions and does not move under its own power, so alongside skipping
     * the AI step this clears the movement inputs the last unfrozen tick left behind — the same
     * fields vanilla zeroes for immobile entities in {@code LivingEntity.aiStep}.
     */
    public static boolean holdIfFrozen(Mob mob) {
        if (FROZEN.isEmpty() || !FROZEN.contains(mob.getUUID())) {
            return false;
        }
        mob.getNavigation().stop();
        mob.setJumping(false);
        mob.xxa = 0;
        mob.yya = 0;
        mob.zza = 0;
        return true;
    }

    /** The escape hatch for entity types that misbehave when AI-suppressed: tag or config list. */
    public static boolean isExcluded(Mob mob, TurnConfig turns) {
        return mob.getType().is(NO_FREEZE)
                || turns.noFreezeTypes()
                        .contains(BuiltInRegistries.ENTITY_TYPE
                                .getKey(mob.getType())
                                .toString());
    }
}
