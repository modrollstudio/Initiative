package studio.modroll.initiative.api;

import net.minecraft.world.entity.Mob;

/** A mod's own turn for its mobs, registered through {@link MobTurns#register}. */
@FunctionalInterface
public interface MobTurn {

    /**
     * One server tick of the mob's own turn. Take actions with {@link ActionRegistry#invoke}, which
     * gates them as it gates a player's; true ends the turn.
     */
    boolean tick(Mob mob, Turn turn);

    /** {@code ticks}: how far into this turn, 0 on its first tick. {@code movementLeft}: blocks it can still move. */
    record Turn(int ticks, double movementLeft) {}
}
