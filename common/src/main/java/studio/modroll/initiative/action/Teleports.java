package studio.modroll.initiative.action;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.turn.OffTurnRestriction;

/**
 * Teleports in an encounter. On its mover's own turn a teleport is movement: it may go no farther
 * than the movement left, measured horizontally like a walk, and the distance it covers is charged.
 * Off turn it is refused outright, exactly as the off-turn hold refuses a step. Every teleport a
 * player takes — an ender pearl, chorus fruit, a command, another mod's — ends in the one server
 * call that moves the player and tells its client, which both loaders' mixins hand to
 * {@link #refuses} first. Initiative's own moves go through {@link #moveAsInitiative} and are never
 * refused: the hold's pull-back, the budget's hard stop and Blink are how these rules are enforced.
 */
public final class Teleports {

    private static boolean movingAsInitiative;

    private Teleports() {}

    /** The mixins' question: true cancels the teleport to {@code (x, z)}, after telling the player why. */
    public static boolean refuses(ServerPlayer player, double x, double z) {
        if (movingAsInitiative) {
            return false;
        }
        Encounter encounter = EncounterManager.encounterContaining(player.serverLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            return false;
        }
        if (OffTurnRestriction.preventsInteraction(player.serverLevel(), player)) {
            if (horizontalDistance(player.position(), x, z) == 0.0) {
                return false;
            }
            OffTurnRestriction.sendRefusal(player);
            return true;
        }
        if (!isActing(encounter, player)
                || !ActionEconomy.active()
                || !InitiativeConfig.actions().teleportsCostMovement()) {
            return false;
        }
        return refusesOnTurn(encounter, player, x, z);
    }

    /** Moves an entity the way Initiative's own rules do, past the gate those rules are enforcing. */
    public static void moveAsInitiative(Entity entity, Vec3 to) {
        movingAsInitiative = true;
        try {
            entity.teleportTo(to.x, to.y, to.z);
        } finally {
            movingAsInitiative = false;
        }
    }

    /**
     * Measured from the last in-budget position, so a teleport after a walk in the same tick is
     * judged on both. The charge itself is left to the turn's movement tracking, which sees where
     * the player really ended up: a chorus fruit tries a spot and steps back when it is blocked.
     */
    private static boolean refusesOnTurn(Encounter encounter, ServerPlayer player, double x, double z) {
        TurnBudget budget = ActionEconomy.budgetOf(encounter, player);
        if (encounter.movementAnchor() == null) {
            encounter.setMovementAnchor(player.position());
        }
        if (horizontalDistance(encounter.movementAnchor(), x, z) > budget.movementRemaining()) {
            player.displayClientMessage(
                    Component.translatable("initiative.action.no_movement").withStyle(ChatFormatting.RED), true);
            return true;
        }
        encounter.noteTeleport();
        return false;
    }

    private static boolean isActing(Encounter encounter, ServerPlayer player) {
        return player.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null));
    }

    private static double horizontalDistance(Vec3 from, double x, double z) {
        return Math.hypot(x - from.x, z - from.z);
    }
}
