package studio.modroll.initiative.turn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The per-encounter turn state machine. Participants are ordered by initiative total (descending),
 * ties broken by higher bonus, then by join order (the earlier joiner acts first). Turns advance via
 * {@link #endTurn} or the per-turn timeout in {@link #tick}; a full pass over the order starts the
 * next round. Ordering only — holding a participant still on someone else's turn is
 * {@link AiFreeze}'s job for mobs and {@link OffTurnRestriction}'s for players.
 */
public final class TurnOrder {

    private static final int TICKS_PER_SECOND = 20;

    private final List<InitiativeEntry> entries = new ArrayList<>();
    private int currentIndex = 0;
    private int round = 1;
    private int ticksInCurrentTurn = 0;
    private boolean started = false;

    /**
     * Inserts by initiative. While the machine is still forming (nothing has ticked or ended a
     * turn yet) the first turn simply belongs to the highest roll so far. Once running, a late
     * joiner keeps the acting participant's turn unchanged; one whose slot in the current round
     * has already passed first acts next round.
     */
    public void insert(InitiativeEntry entry) {
        int position = insertionPoint(entry);
        entries.add(position, entry);
        if (started && position <= currentIndex) {
            currentIndex++;
        }
    }

    /**
     * Drops a participant. Removing the acting participant ends its turn immediately: the next
     * participant in the order becomes current, wrapping into the next round when it was last.
     */
    public void remove(UUID participant) {
        int index = indexOf(participant);
        if (index < 0) {
            return;
        }
        entries.remove(index);
        if (index < currentIndex) {
            currentIndex--;
            return;
        }
        if (index == currentIndex) {
            ticksInCurrentTurn = 0;
            if (currentIndex >= entries.size()) {
                currentIndex = 0;
                if (!entries.isEmpty()) {
                    round++;
                }
            }
        }
    }

    public void endTurn() {
        started = true;
        ticksInCurrentTurn = 0;
        if (entries.isEmpty()) {
            return;
        }
        currentIndex++;
        if (currentIndex >= entries.size()) {
            currentIndex = 0;
            round++;
        }
    }

    /** Advances the per-turn clock; the turn ends when it reaches {@code timeoutTicks}. */
    public void tick(int timeoutTicks) {
        if (entries.isEmpty()) {
            return;
        }
        started = true;
        ticksInCurrentTurn++;
        if (ticksInCurrentTurn >= timeoutTicks) {
            endTurn();
        }
    }

    public Optional<UUID> currentTurn() {
        return entries.isEmpty()
                ? Optional.empty()
                : Optional.of(entries.get(currentIndex).participant());
    }

    public int round() {
        return round;
    }

    /** How long the acting participant has held the turn; the UI counts the timeout down from it. */
    public int ticksInCurrentTurn() {
        return ticksInCurrentTurn;
    }

    /** The countdown every client shows — the acting player on its bar, the rest on the turn strip. */
    public int secondsRemaining(int timeoutTicks) {
        return seconds(timeoutTicks - ticksInCurrentTurn);
    }

    public static int seconds(int ticks) {
        return Math.max(0, (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
    }

    public List<InitiativeEntry> order() {
        return List.copyOf(entries);
    }

    private int insertionPoint(InitiativeEntry entry) {
        int position = 0;
        while (position < entries.size() && !sortsBefore(entry, entries.get(position))) {
            position++;
        }
        return position;
    }

    private static boolean sortsBefore(InitiativeEntry a, InitiativeEntry b) {
        if (a.total() != b.total()) {
            return a.total() > b.total();
        }
        return a.bonus() > b.bonus();
    }

    private int indexOf(UUID participant) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).participant().equals(participant)) {
                return i;
            }
        }
        return -1;
    }
}
