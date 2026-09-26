package studio.modroll.initiative.encounter;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;

/**
 * "This mob will now attack that player" — the signal that pulls a provoked neutral into an
 * encounter. Vanilla says it two ways: mobs that only acquire an attack target (panda, llama, an
 * iron golem defending its village) and {@link NeutralMob}s whose anger outlives the target field
 * (wolf, bee, polar bear). Both go through {@code Mob.setTarget}, which is what the loaders report
 * to {@link #mobTargeted}; anger is read too, so a goal re-picking its target for one tick cannot
 * drop an angry mob out of the fight it started.
 *
 * <p>Reporting only queues the mob: the encounter forms on the next tick rather than inside the
 * goal that was assigning the target. {@link Enemy} mobs are ignored here — they already trigger on
 * the combat interaction, and that path is untouched.
 */
public final class Provocation {

    /**
     * Concurrent because {@link #mobTargeted} is written from any mob's AI step server-wide while
     * the encounter tick drains, the same reason {@code AiFreeze} guards its set.
     */
    private static final Map<ServerLevel, Set<UUID>> PENDING = new ConcurrentHashMap<>();

    private Provocation() {}

    /** The loaders' mob-target hook. A null target, a non-player target and PvP all report nothing. */
    public static void mobTargeted(Mob mob, LivingEntity target) {
        if (!(target instanceof Player) || !isNeutral(mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        EncounterConfig config = InitiativeConfig.encounters();
        if (!config.enabled() || !config.triggerOnProvokedNeutral()) {
            return;
        }
        PENDING.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet()).add(mob.getUUID());
    }

    static Set<UUID> drainPending(ServerLevel level) {
        Set<UUID> pending = PENDING.remove(level);
        return pending == null ? Set.of() : pending;
    }

    static void forget(ServerLevel level) {
        PENDING.remove(level);
    }

    static void clear() {
        PENDING.clear();
    }

    /** Not an always-hostile mob, so its hostility has to be read rather than assumed. */
    static boolean isNeutral(Mob mob) {
        return !(mob instanceof Enemy);
    }

    /**
     * A neutral mob no encounter owns — the one thing an in-encounter swing has to be allowed to
     * reach, because the swing is what makes it hostile. An always-hostile mob never needs this:
     * it is pulled into the bubble on sight, so it is already a target by the time you can hit it.
     * Livestock answers yes here too, stays hittable, and simply never turns hostile.
     */
    public static boolean isUnclaimedNeutral(ServerLevel level, LivingEntity entity) {
        return entity instanceof Mob mob
                && isNeutral(mob)
                && EncounterManager.encounterContaining(level, mob.getUUID()).isEmpty();
    }

    static boolean isProvokedBy(Mob mob, Player player) {
        return player.equals(mob.getTarget()) || mob instanceof NeutralMob neutral && neutral.isAngryAt(player);
    }
}
