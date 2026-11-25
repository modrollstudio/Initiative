package studio.modroll.initiative.action;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToIntFunction;
import net.minecraft.world.entity.LivingEntity;

/** The sources registered through {@code Attacks}: how many attacks one Attack action makes. */
public final class AttackSources {

    private static final List<ToIntFunction<LivingEntity>> SOURCES = new CopyOnWriteArrayList<>();

    private AttackSources() {}

    public static void register(ToIntFunction<LivingEntity> source) {
        SOURCES.add(source);
    }

    static int perAction(LivingEntity creature) {
        return highest(SOURCES, creature);
    }

    /** Extra Attack features don't stack, so the highest answer counts rather than the sum. */
    static int highest(List<ToIntFunction<LivingEntity>> sources, LivingEntity creature) {
        int attacks = 1;
        for (ToIntFunction<LivingEntity> source : sources) {
            attacks = Math.max(attacks, source.applyAsInt(creature));
        }
        return attacks;
    }
}
