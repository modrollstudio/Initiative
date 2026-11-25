package studio.modroll.initiative.api;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.action.Concealment;
import studio.modroll.initiative.action.CustomMobTurns;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * Mobs' turns for mods that bring their own moves and the AI that picks them. Initiative keeps the
 * turn, its budget, the movement clamp and the freeze; every action a handler takes goes through
 * {@link ActionRegistry#invoke}. Register during mod construction.
 */
public final class MobTurns {

    private MobTurns() {}

    /** Mobs {@code handles} matches take their turns through {@code turn} instead of Initiative's one attack. First match wins. */
    public static void register(Predicate<Mob> handles, MobTurn turn) {
        CustomMobTurns.register(handles, turn);
    }

    /** A mob {@code hides} matches joins a fight hidden: with Checks on a Stealth roll, without as it stands. */
    public static void joinsHidden(Predicate<Mob> hides) {
        CustomMobTurns.joinsHidden(hides);
    }

    /**
     * Starts a fight with the mob hidden; {@code target} and the others it catches roll initiative with
     * disadvantage. False when no fight was started: carry on in real time.
     */
    public static boolean ambush(Mob mob, Player target) {
        return EncounterManager.ambush(mob, target);
    }

    /** Whether it is hidden in its fight right now (its hit, its attack or a Perception check can end that). */
    public static boolean hidden(LivingEntity creature) {
        return Concealment.hidden(creature);
    }
}
