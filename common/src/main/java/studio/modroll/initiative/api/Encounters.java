package studio.modroll.initiative.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.critfall.api.AttackDelivery;
import studio.modroll.initiative.encounter.EncounterManager;

/** Who is in a fight, and fights another mod starts. Since 0.3.3. */
public final class Encounters {

    private Encounters() {}

    public static boolean inFight(LivingEntity entity) {
        return entity.level() instanceof ServerLevel level
                && EncounterManager.encounterContaining(level, entity.getUUID()).isPresent();
    }

    /**
     * {@code attacker} is about to hurt {@code target} in a way Initiative doesn't see, such as through Critfall's
     * {@code RollService}: a fight starts or takes them in by the rules a vanilla hit follows. Ask before the hurt,
     * while both are alive.
     */
    public static void engage(LivingEntity attacker, LivingEntity target) {
        EncounterManager.onCombat(attacker, target, AttackDelivery.SPELL);
    }
}
