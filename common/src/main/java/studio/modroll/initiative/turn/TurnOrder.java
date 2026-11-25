package studio.modroll.initiative.turn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The per-encounter turn state machine. Participants are ordered by initiative total (descending),
 * ties broken by higher bonus, then by join order (the earlier joiner acts first). Turns advance via
 * {@link #endTurn} or the per-turn timeout in {@link #tick}; a full pass over the order starts the
 * next round. A mount steered by its rider acts on the rider's turn ({@link #follow}). Ordering
 * only — holding a participant still on someone else's turn is {@link AiFreeze}'s job for mobs and
 * {@link OffTurnRestriction}'s for players.
 */
public final class TurnOrder {

    private static final int TICKS_PER_SECOND = 20;

    private final List<InitiativeEntry> entries = new ArrayList<>();
    private final Set<UUID> actedThisRound = new HashSet<>();
    private final List<UUID> skippedTurns = new ArrayList<>();
    private final Map<UUID, UUID> riders = new HashMap<>();
    private final Set<UUID> passedOverThisRound = new HashSet<>();
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
        insertAt(entry);
    }

    /**
     * Inserts a participant whose way in was an act of its own — an attack on someone already here —
     * which counts as its turn for the round it joined in. A slot still ahead this round is passed
     * over when the order reaches it ({@link #drainSkippedTurns}); one already behind the acting
     * participant needs nothing, since its first turn is next round anyway.
     */
    public void insertHavingActed(InitiativeEntry entry) {
        if (insertAt(entry)) {
            actedThisRound.add(entry.participant());
            skipTurnsAlreadyTaken();
        }
    }

    /** Whether the new entry's slot is still ahead this round (or is the forming order's first). */
    private boolean insertAt(InitiativeEntry entry) {
        int position = insertionPoint(entry);
        entries.add(position, entry);
        if (started && position <= currentIndex) {
            currentIndex++;
            return false;
        }
        return true;
    }

    /**
     * A mount steered by its rider acts on the rider's turn. Its entry moves to just after the
     * rider's, at the rider's initiative, and the order passes over it without a word; a mount whose
     * own turn was running hands it on.
     */
    public void follow(UUID mount, UUID rider) {
        int from = indexOf(mount);
        int riderIndex = indexOf(rider);
        if (rider.equals(riders.get(mount)) || from < 0 || riderIndex < 0) {
            return;
        }
        UUID current = currentTurn().orElseThrow();
        InitiativeEntry riderEntry = entries.get(riderIndex);
        entries.remove(from);
        entries.add(indexOf(rider) + 1, new InitiativeEntry(mount, riderEntry.total(), riderEntry.bonus()));
        riders.put(mount, rider);
        passedOverThisRound.remove(mount);
        if (!current.equals(mount)) {
            currentIndex = indexOf(current);
            return;
        }
        currentIndex = from;
        ticksInCurrentTurn = 0;
        if (currentIndex >= entries.size()) {
            currentIndex = 0;
            startNextRound();
        }
        skipTurnsAlreadyTaken();
    }

    /** The pair split: the mount takes its own turns again from the next round. */
    public void stopFollowing(UUID mount) {
        if (riders.remove(mount) != null) {
            passedOverThisRound.add(mount);
        }
    }

    /** Whether this participant acts now: it holds the turn, or is the mount acting with whoever does. */
    public boolean isActing(UUID participant) {
        UUID current = currentTurn().orElse(null);
        return current != null && (current.equals(participant) || current.equals(riders.get(participant)));
    }

    /** The mount acting on this rider's turn. */
    public Optional<UUID> mountOf(UUID rider) {
        return riders.entrySet().stream()
                .filter(pair -> pair.getValue().equals(rider))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * Drops a participant. Removing the acting participant ends its turn immediately: the next
     * participant in the order becomes current, wrapping into the next round when it was last. A
     * mount whose rider leaves takes its own turns again from the next round.
     */
    public void remove(UUID participant) {
        int index = indexOf(participant);
        if (index < 0) {
            return;
        }
        riders.remove(participant);
        passedOverThisRound.remove(participant);
        mountOf(participant).ifPresent(this::stopFollowing);
        entries.remove(index);
        actedThisRound.remove(participant);
        if (index < currentIndex) {
            currentIndex--;
            return;
        }
        if (index == currentIndex) {
            ticksInCurrentTurn = 0;
            if (currentIndex >= entries.size()) {
                currentIndex = 0;
                if (!entries.isEmpty()) {
                    startNextRound();
                }
            }
            skipTurnsAlreadyTaken();
        }
    }

    public void endTurn() {
        started = true;
        ticksInCurrentTurn = 0;
        if (entries.isEmpty()) {
            return;
        }
        advance();
        skipTurnsAlreadyTaken();
    }

    /**
     * The participants whose turn was passed over since the last call because they had already
     * acted this round, in the order they were passed; the encounter tells its players.
     */
    public List<UUID> drainSkippedTurns() {
        List<UUID> drained = List.copyOf(skippedTurns);
        skippedTurns.clear();
        return drained;
    }

    private void advance() {
        currentIndex++;
        if (currentIndex >= entries.size()) {
            currentIndex = 0;
            startNextRound();
        }
    }

    private void startNextRound() {
        round++;
        actedThisRound.clear();
        passedOverThisRound.clear();
    }

    /**
     * Passes over whoever already acted this round, with a word, and a following mount, or one whose
     * pair split this round, quietly. The first rider of a chain follows nobody, so a lap always
     * reaches someone whose turn it is.
     */
    private void skipTurnsAlreadyTaken() {
        while (!entries.isEmpty()) {
            UUID participant = entries.get(currentIndex).participant();
            if (actedThisRound.remove(participant)) {
                skippedTurns.add(participant);
            } else if (!riders.containsKey(participant) && !passedOverThisRound.contains(participant)) {
                return;
            }
            started = true;
            ticksInCurrentTurn = 0;
            advance();
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
