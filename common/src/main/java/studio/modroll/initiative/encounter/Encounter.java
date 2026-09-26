package studio.modroll.initiative.encounter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.action.EncounterFlags;
import studio.modroll.initiative.action.TurnBudget;
import studio.modroll.initiative.hud.TurnOrderSnapshot;
import studio.modroll.initiative.turn.TurnOrder;
import studio.modroll.initiative.ui.ActionUiSnapshot;

/**
 * Membership + geometry of one encounter bubble, plus its HUD bookkeeping (id and last-sent
 * snapshot) and the acting participant's per-turn budget; world interaction lives in the manager.
 */
public final class Encounter {

    public enum Side {
        PLAYER,
        HOSTILE
    }

    private final UUID id = UUID.randomUUID();
    private final Vec3 center;
    private final Map<UUID, Side> participants = new LinkedHashMap<>();
    private final Map<UUID, Long> joinedAtGameTime = new LinkedHashMap<>();
    private final Set<UUID> rangedAggressors = new LinkedHashSet<>();
    private final TurnOrder turnOrder = new TurnOrder();
    private final TurnBudget budget = new TurnBudget();
    private final EncounterFlags flags = new EncounterFlags();
    private final Map<UUID, ActionUiSnapshot> lastActionUiSnapshots = new LinkedHashMap<>();
    private TurnOrderSnapshot lastHudSnapshot;
    private int budgetRound = -1;
    private UUID budgetActor;
    private Vec3 movementAnchor;

    public Encounter(Vec3 center) {
        this.center = center;
    }

    public UUID id() {
        return id;
    }

    public Vec3 center() {
        return center;
    }

    public TurnOrderSnapshot lastHudSnapshot() {
        return lastHudSnapshot;
    }

    public void setLastHudSnapshot(TurnOrderSnapshot snapshot) {
        lastHudSnapshot = snapshot;
    }

    /** The action UI is per player, so its last-sent state is remembered per participant. */
    public ActionUiSnapshot lastActionUiSnapshot(UUID participant) {
        return lastActionUiSnapshots.get(participant);
    }

    public void setLastActionUiSnapshot(UUID participant, ActionUiSnapshot snapshot) {
        lastActionUiSnapshots.put(participant, snapshot);
    }

    public TurnOrder turnOrder() {
        return turnOrder;
    }

    public void add(UUID participant, Side side, long gameTime) {
        participants.put(participant, side);
        joinedAtGameTime.put(participant, gameTime);
    }

    public void remove(UUID participant) {
        participants.remove(participant);
        joinedAtGameTime.remove(participant);
        rangedAggressors.remove(participant);
        lastActionUiSnapshots.remove(participant);
        turnOrder.remove(participant);
        flags.remove(participant);
    }

    /** Marks a participant that shot its way in from outside the bubble rather than standing in it. */
    public void markRangedAggressor(UUID participant) {
        rangedAggressors.add(participant);
    }

    public boolean isRangedAggressor(UUID participant) {
        return rangedAggressors.contains(participant);
    }

    /**
     * Whether the participant joined on an earlier tick. The encounter-forming hit joins its
     * attacker and victim mid-hurt, in a listener whose order against other mods' damage listeners
     * is loader-dependent — so rules that react to participant damage must key on membership as of
     * the previous tick to behave the same on every loader.
     */
    public boolean joinedBefore(UUID participant, long gameTime) {
        Long joined = joinedAtGameTime.get(participant);
        return joined != null && joined < gameTime;
    }

    public boolean contains(UUID participant) {
        return participants.containsKey(participant);
    }

    public Side side(UUID participant) {
        return participants.get(participant);
    }

    /**
     * The acting participant's budget, reset lazily whenever the (round, actor) turn key changes.
     * The same transition starts the actor's turn for the per-turn flags: its own dodge/disengage
     * lapse and any Help it granted expires.
     */
    public TurnBudget budgetFor(int round, UUID actor, double movementBudget) {
        if (round != budgetRound || !actor.equals(budgetActor)) {
            budgetRound = round;
            budgetActor = actor;
            movementAnchor = null;
            budget.reset(movementBudget);
            flags.beginTurn(actor);
        }
        return budget;
    }

    public TurnBudget budget() {
        return budget;
    }

    public EncounterFlags flags() {
        return flags;
    }

    public Vec3 movementAnchor() {
        return movementAnchor;
    }

    public void setMovementAnchor(Vec3 anchor) {
        movementAnchor = anchor;
    }

    public Set<UUID> participants() {
        return Set.copyOf(participants.keySet());
    }

    public boolean isOver() {
        return !participants.containsValue(Side.PLAYER) || !participants.containsValue(Side.HOSTILE);
    }

    public boolean isWithin(Vec3 pos, double radius) {
        return center.distanceToSqr(pos) <= radius * radius;
    }
}
