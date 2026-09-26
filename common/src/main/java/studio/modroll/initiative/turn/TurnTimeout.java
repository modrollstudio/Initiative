package studio.modroll.initiative.turn;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.config.TurnConfig;

/**
 * How long the participant currently holding the turn may hold it. Read at the tick that advances
 * the turn and at every countdown the clients are sent, so the timer that fires and the timer that
 * is displayed can never be two different numbers.
 */
public final class TurnTimeout {

    private TurnTimeout() {}

    public static int forCurrentActor(ServerLevel level, TurnOrder order, TurnConfig turns) {
        UUID current = order.currentTurn().orElse(null);
        boolean player = current != null && level.getEntity(current) instanceof Player;
        return player ? turns.turnTimeoutTicks() : turns.mobTurnTimeoutTicks();
    }
}
