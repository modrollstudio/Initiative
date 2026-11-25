package studio.modroll.initiative.api;

import java.util.function.ToIntFunction;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.initiative.action.AttackSources;

/**
 * How many attacks one Attack action makes: a mob's Multiattack, a player's Extra Attack. Register a
 * source during mod construction.
 */
public final class Attacks {

    private Attacks() {}

    /** How many attacks one Attack action makes for a creature. The highest answer counts, never below 1. */
    public static void register(ToIntFunction<LivingEntity> source) {
        AttackSources.register(source);
    }
}
