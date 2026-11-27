package studio.modroll.initiative.turn;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.encounter.Encounter;

/** The listeners {@code Turns.onTurnStart} and {@code onTurnEnd} register, told as a fight's turn passes on. */
public final class TurnEvents {

    private static final List<Consumer<LivingEntity>> STARTS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<LivingEntity>> ENDS = new CopyOnWriteArrayList<>();

    private TurnEvents() {}

    public static void onStart(Consumer<LivingEntity> listener) {
        STARTS.add(listener);
    }

    public static void onEnd(Consumer<LivingEntity> listener) {
        ENDS.add(listener);
    }

    /** Once the turn has passed since the last tick: the old turn's end, then the new one's start. */
    public static void reconcile(ServerLevel level, Encounter encounter) {
        UUID current = encounter.turnOrder().currentTurn().orElse(null);
        int round = encounter.turnOrder().round();
        if (Objects.equals(current, encounter.startedTurn()) && round == encounter.startedRound()) {
            return;
        }
        over(level, encounter);
        encounter.startTurn(current, round);
        if (current != null && level.getEntity(current) instanceof LivingEntity entity) {
            tell(STARTS, entity);
        }
    }

    /** The turn that started last ends, as its fight does. */
    public static void over(ServerLevel level, Encounter encounter) {
        UUID started = encounter.startedTurn();
        encounter.startTurn(null, encounter.startedRound());
        if (started != null && level.getEntity(started) instanceof LivingEntity entity) {
            tell(ENDS, entity);
        }
    }

    /** A listener that throws is logged and skipped, so one broken mod can't stall the fight. */
    private static void tell(List<Consumer<LivingEntity>> listeners, LivingEntity participant) {
        for (Consumer<LivingEntity> listener : listeners) {
            try {
                listener.accept(participant);
            } catch (RuntimeException e) {
                Initiative.LOG.error("A turn listener failed on {}", participant, e);
            }
        }
    }
}
