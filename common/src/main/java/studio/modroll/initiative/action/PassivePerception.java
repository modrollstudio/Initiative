package studio.modroll.initiative.action;

import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.initiative.checks.ChecksBridge;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.encounter.Encounter;

/**
 * The Hide re-check with Checks: on an enemy's turn, a hider whose Stealth total that enemy's passive
 * Perception meets or beats is revealed. Neither side rolls, so checking every tick of the turn reads
 * the same as checking once at its start. Only a Hide made through Checks keeps a total, so without
 * Checks there is never anyone to re-check.
 */
final class PassivePerception {

    private PassivePerception() {}

    static void revealHidersSpottedBy(Encounter encounter, LivingEntity watcher) {
        Map<UUID, Integer> stealthByHider = encounter.flags().hiddenStealth();
        if (stealthByHider.isEmpty() || !ChecksIntegration.active()) {
            return;
        }
        Encounter.Side watcherSide = encounter.side(watcher.getUUID());
        int perception = ChecksBridge.passivePerception(watcher);
        stealthByHider.forEach((hider, stealth) -> {
            if (encounter.side(hider) != watcherSide && perception >= stealth) {
                encounter.flags().breakHidden(hider);
            }
        });
    }
}
