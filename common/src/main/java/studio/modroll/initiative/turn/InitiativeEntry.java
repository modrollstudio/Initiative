package studio.modroll.initiative.turn;

import java.util.UUID;

/** One participant's initiative result: {@code total = d20 natural + bonus}. */
public record InitiativeEntry(UUID participant, int total, int bonus) {}
