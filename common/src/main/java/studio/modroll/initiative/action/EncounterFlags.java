package studio.modroll.initiative.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The per-participant, per-turn flags the standard actions raise: {@code dodging} and
 * {@code disengaged} last until their owner's next turn starts; a Help grant gives one ally
 * advantage on its next attack and lasts until it is consumed or the helper's next turn starts.
 * The reaction refreshes at the participant's own turn start (5e), unlike the dodge/disengage
 * flags which lapse. Hidden, by contrast, is a persistent condition — unlike dodge/disengage it
 * does NOT lapse at the hider's next turn start; it clears only when the hider attacks, is hit, or
 * is removed from the encounter. A grapple is a condition too, held on the grappled participant
 * against its grappler: it survives both their turns and clears only on an escape, a break, or
 * either side leaving.
 * Kept off {@link TurnBudget}, which only ever holds the acting participant — these flags outlive
 * the actor's own turn and must survive every other participant's turn in between.
 */
public final class EncounterFlags {

    private final Set<UUID> dodging = new HashSet<>();
    private final Set<UUID> disengaged = new HashSet<>();
    private final Map<UUID, UUID> helpAdvantageByAlly = new HashMap<>();
    private final Set<UUID> reactionUsed = new HashSet<>();
    private final Set<UUID> hidden = new HashSet<>();
    private final Map<UUID, UUID> grappledBy = new HashMap<>();

    public boolean isDodging(UUID participant) {
        return dodging.contains(participant);
    }

    public boolean isDisengaged(UUID participant) {
        return disengaged.contains(participant);
    }

    public boolean hasHelpAdvantage(UUID ally) {
        return helpAdvantageByAlly.containsKey(ally);
    }

    public void setDodging(UUID participant) {
        dodging.add(participant);
    }

    public void setDisengaged(UUID participant) {
        disengaged.add(participant);
    }

    public void grantHelpAdvantage(UUID ally, UUID helper) {
        helpAdvantageByAlly.put(ally, helper);
    }

    public boolean consumeHelpAdvantage(UUID ally) {
        return helpAdvantageByAlly.remove(ally) != null;
    }

    public boolean hasReaction(UUID participant) {
        return !reactionUsed.contains(participant);
    }

    public void useReaction(UUID participant) {
        reactionUsed.add(participant);
    }

    public boolean isHidden(UUID participant) {
        return hidden.contains(participant);
    }

    public void setHidden(UUID participant) {
        hidden.add(participant);
    }

    /** Whether anyone in this encounter is hiding, which is what {@code Concealment} reconciles on. */
    public boolean anyHidden() {
        return !hidden.isEmpty();
    }

    /** Attacking consumes the hidden state: returns whether it was hidden (the advantage grant), then breaks it. */
    public boolean consumeHidden(UUID participant) {
        return hidden.remove(participant);
    }

    /** Breaks the hidden state without granting anything (used when a hit reveals the hider). */
    public void breakHidden(UUID participant) {
        hidden.remove(participant);
    }

    public boolean isGrappled(UUID participant) {
        return grappledBy.containsKey(participant);
    }

    /** Who is holding this participant, or null. */
    public UUID grapplerOf(UUID participant) {
        return grappledBy.get(participant);
    }

    public void setGrappled(UUID target, UUID grappler) {
        grappledBy.put(target, grappler);
    }

    public void releaseGrapple(UUID target) {
        grappledBy.remove(target);
    }

    /** Every held participant against its grappler, for the per-tick break check. */
    public Map<UUID, UUID> grapples() {
        return Map.copyOf(grappledBy);
    }

    /**
     * Start of {@code actor}'s turn: its own dodge/disengage lapse, as does any Help it granted;
     * reaction refreshes. Hidden is untouched here — it persists across its owner's own turn
     * boundary and clears only via {@link #consumeHidden}, {@link #breakHidden} or {@link #remove}.
     */
    public void beginTurn(UUID actor) {
        dodging.remove(actor);
        disengaged.remove(actor);
        helpAdvantageByAlly.values().removeIf(helper -> helper.equals(actor));
        reactionUsed.remove(actor);
    }

    public void remove(UUID participant) {
        dodging.remove(participant);
        disengaged.remove(participant);
        helpAdvantageByAlly.remove(participant);
        helpAdvantageByAlly.values().removeIf(helper -> helper.equals(participant));
        reactionUsed.remove(participant);
        hidden.remove(participant);
        grappledBy.remove(participant);
        grappledBy.values().removeIf(grappler -> grappler.equals(participant));
    }
}
