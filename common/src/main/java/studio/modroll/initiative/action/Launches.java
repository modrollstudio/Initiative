package studio.modroll.initiative.action;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Creatures an action threw, until they land. The flight is not their own movement: it spends none
 * and provokes nothing, and the off-turn hold lets a thrown player fly and holds it where it lands.
 */
public final class Launches {

    private static final Set<UUID> FLYING = ConcurrentHashMap.newKeySet();

    private Launches() {}

    /** Marked off the ground, so the flight ends on the first tick it is back on it. */
    static void launch(LivingEntity target, Vec3 motion) {
        target.setDeltaMovement(motion);
        target.setOnGround(false);
        target.hurtMarked = true;
        FLYING.add(target.getUUID());
    }

    /**
     * Whether a throw moved it this tick: still in the air, or landing. The landing tick counts, so
     * it is held where it came down; asking once it is down ends the flight.
     */
    public static boolean inFlight(LivingEntity entity) {
        if (!FLYING.contains(entity.getUUID())) {
            return false;
        }
        if (!entity.isAlive() || entity.onGround() || entity.isInLiquid() || entity.isPassenger()) {
            FLYING.remove(entity.getUUID());
        }
        return true;
    }

    public static void forget(UUID entity) {
        FLYING.remove(entity);
    }

    public static void clear() {
        FLYING.clear();
    }
}
