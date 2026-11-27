package studio.modroll.initiative.api;

import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.initiative.encounter.EncounterManager;

/** The turns of a fight, for mods whose actions take time to land. */
public final class Turns {

    private Turns() {}

    /**
     * Holds the fight {@code participant} is in until {@code landed} answers true, so a hit on its way, such as a
     * spell's bolt, lands before the fight goes on: the turn neither ends (End Turn, the auto end, the timeout) nor
     * moves to the next participant, and every action but a reaction waits, refused with {@link
     * ActionStatus#WAITING}. Movement goes on. {@code landed} is asked every tick, so it must turn true soon, also
     * when the hit is dropped. Does nothing when {@code participant} is in no fight. Since 0.3.2.
     */
    public static void holdUntil(LivingEntity participant, BooleanSupplier landed) {
        if (participant.level() instanceof ServerLevel level) {
            EncounterManager.encounterContaining(level, participant.getUUID())
                    .ifPresent(encounter -> encounter.hold(landed));
        }
    }
}
