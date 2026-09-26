package studio.modroll.initiative.api;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** What an invocation points the action at: an entity, a position, or neither. */
public record ActionRequest(Optional<LivingEntity> target, Optional<Vec3> position) {

    private static final ActionRequest NONE = new ActionRequest(Optional.empty(), Optional.empty());

    public static ActionRequest none() {
        return NONE;
    }

    public static ActionRequest of(LivingEntity target) {
        return new ActionRequest(Optional.ofNullable(target), Optional.empty());
    }

    public static ActionRequest at(Vec3 position) {
        return new ActionRequest(Optional.empty(), Optional.ofNullable(position));
    }
}
