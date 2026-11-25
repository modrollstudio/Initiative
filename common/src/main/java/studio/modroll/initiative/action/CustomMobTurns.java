package studio.modroll.initiative.action;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.world.entity.Mob;
import studio.modroll.initiative.api.MobTurn;
import studio.modroll.initiative.config.InitiativeConfig;

/** The turn handlers and hiding rules mods register through {@code MobTurns}. */
public final class CustomMobTurns {

    private record Handler(Predicate<Mob> handles, MobTurn turn) {}

    private static final List<Handler> HANDLERS = new CopyOnWriteArrayList<>();
    private static final List<Predicate<Mob>> HIDERS = new CopyOnWriteArrayList<>();

    private CustomMobTurns() {}

    public static void register(Predicate<Mob> handles, MobTurn turn) {
        HANDLERS.add(new Handler(handles, turn));
    }

    public static void joinsHidden(Predicate<Mob> hides) {
        HIDERS.add(hides);
    }

    /** The first handler that takes this mob, or null for Initiative's own turn. */
    static MobTurn handlerFor(Mob mob) {
        if (!InitiativeConfig.turns().customMobTurns()) {
            return null;
        }
        for (Handler handler : HANDLERS) {
            if (handler.handles().test(mob)) {
                return handler.turn();
            }
        }
        return null;
    }

    public static boolean joinsHidden(Mob mob) {
        for (Predicate<Mob> hides : HIDERS) {
            if (hides.test(mob)) {
                return true;
            }
        }
        return false;
    }
}
