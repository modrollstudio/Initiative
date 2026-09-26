package studio.modroll.initiative.api;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.initiative.action.ActionGate;

/**
 * Every action Initiative knows about, built-in or third-party. Register during mod construction;
 * ids are unique and a clash throws rather than silently replacing another mod's action.
 */
public final class ActionRegistry {

    private static final Map<ResourceLocation, Action> ACTIONS = new LinkedHashMap<>();

    private ActionRegistry() {}

    public static void register(Action action) {
        synchronized (ACTIONS) {
            Action existing = ACTIONS.putIfAbsent(action.id(), action);
            if (existing != null) {
                throw new IllegalArgumentException("action " + action.id() + " is already registered");
            }
        }
    }

    public static Optional<Action> get(ResourceLocation id) {
        synchronized (ACTIONS) {
            return Optional.ofNullable(ACTIONS.get(id));
        }
    }

    /** Every registered action, in registration order. */
    public static Collection<Action> all() {
        synchronized (ACTIONS) {
            return List.copyOf(ACTIONS.values());
        }
    }

    /**
     * Runs an action for {@code actor}: gates it on the economy, the action's own toggle, the turn,
     * its cost and its targeting, performs it, then charges the cost only if it was performed. The
     * gate itself is deliberately internal — this delegation is what keeps it off the public surface,
     * so third parties invoke actions without being able to reach around the rules.
     */
    public static ActionResult invoke(
            ServerLevel level, LivingEntity actor, ResourceLocation id, ActionRequest request) {
        return ActionGate.invoke(level, actor, id, request);
    }

    /** Test-scope only, mirroring Critfall's {@code RollService.setRoller} convention. */
    public static void clearForTesting() {
        synchronized (ACTIONS) {
            ACTIONS.clear();
        }
    }
}
