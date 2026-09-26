package studio.modroll.initiative.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.turn.InitiativeEntry;
import studio.modroll.initiative.turn.TurnTimeout;

/** Builds the HUD's immutable view of an encounter; entries follow the turn order exactly. */
public final class TurnOrderSnapshots {

    private TurnOrderSnapshots() {}

    public static TurnOrderSnapshot build(ServerLevel level, Encounter encounter) {
        List<TurnOrderSnapshot.Entry> entries = new ArrayList<>();
        for (InitiativeEntry initiative : encounter.turnOrder().order()) {
            if (level.getEntity(initiative.participant()) instanceof LivingEntity entity) {
                entries.add(entry(entity));
            }
        }
        int currentActorId = encounter
                .turnOrder()
                .currentTurn()
                .map(level::getEntity)
                .filter(Objects::nonNull)
                .map(Entity::getId)
                .orElse(TurnOrderSnapshot.NO_ACTOR);
        return new TurnOrderSnapshot(
                encounter.id(),
                List.copyOf(entries),
                currentActorId,
                encounter
                        .turnOrder()
                        .secondsRemaining(
                                TurnTimeout.forCurrentActor(level, encounter.turnOrder(), InitiativeConfig.turns())));
    }

    private static TurnOrderSnapshot.Entry entry(LivingEntity entity) {
        return new TurnOrderSnapshot.Entry(
                entity.getId(),
                entity.getDisplayName().getString(),
                EntityType.getKey(entity.getType()).toString(),
                entity instanceof Player);
    }
}
