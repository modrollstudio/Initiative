package studio.modroll.initiative.encounter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.action.EncounterFlags;
import studio.modroll.initiative.action.RangedShot;
import studio.modroll.initiative.action.TurnBudget;
import studio.modroll.initiative.hud.TurnOrderSnapshot;
import studio.modroll.initiative.turn.TurnOrder;
import studio.modroll.initiative.ui.ActionUiSnapshot;

/**
 * Membership + geometry of one encounter bubble, plus its HUD bookkeeping (id and last-sent
 * snapshot) and the acting participants' per-turn budgets; world interaction lives in the manager.
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
    private final Set<UUID> aggressors = new LinkedHashSet<>();
    private final Set<UUID> openingAttacks = new LinkedHashSet<>();
    private final Map<UUID, RangedShot> shots = new HashMap<>();
    private final TurnOrder turnOrder = new TurnOrder();
    private final Map<UUID, TurnBudget> budgets = new HashMap<>();
    private final EncounterFlags flags = new EncounterFlags();
    private final List<BooleanSupplier> holds = new ArrayList<>();
    private final Map<UUID, ActionUiSnapshot> lastActionUiSnapshots = new LinkedHashMap<>();
    private TurnOrderSnapshot lastHudSnapshot;
    private int budgetRound = -1;
    private UUID budgetTurn;
    private Vec3 movementAnchor;
    private boolean teleported;

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
        aggressors.remove(participant);
        openingAttacks.remove(participant);
        shots.values().removeIf(shot -> shot.shooter().equals(participant));
        lastActionUiSnapshots.remove(participant);
        turnOrder.remove(participant);
        flags.remove(participant);
    }

    /**
     * Marks a participant that attacked its way in — from outside the fight, or by a shot from
     * outside the bubble — rather than being pulled in for standing in it.
     */
    public void markAggressor(UUID participant) {
        aggressors.add(participant);
    }

    public boolean isAggressor(UUID participant) {
        return aggressors.contains(participant);
    }

    /**
     * Notes that this participant has just joined by attacking someone already here, and that the
     * hit which joined it still has to be rolled as its turn's attack. Only good for the tick it
     * joined in: the encounter tick forgets whatever was not rolled.
     */
    public void markOpeningAttack(UUID participant) {
        openingAttacks.add(participant);
    }

    /** Whether this participant's joining hit was waiting to be rolled; asking takes it. */
    public boolean consumeOpeningAttack(UUID participant) {
        return openingAttacks.remove(participant);
    }

    public void forgetOpeningAttacks() {
        openingAttacks.clear();
    }

    /** Ties a projectile in flight to the ranged attack it belongs to. */
    public void recordShot(UUID projectile, RangedShot shot) {
        shots.put(projectile, shot);
    }

    /** The ranged attack this projectile was fired as, or null when it was fired as none. */
    public RangedShot shot(UUID projectile) {
        return shots.get(projectile);
    }

    /** The attack this shooter fired on this tick, which a second arrow of the same volley belongs to. */
    public Optional<RangedShot> volley(UUID shooter, long gameTime) {
        return shots.values().stream()
                .filter(shot -> shot.shooter().equals(shooter) && shot.firedAt() == gameTime)
                .findFirst();
    }

    public void forgetShots(Predicate<UUID> projectileGone) {
        shots.keySet().removeIf(projectileGone);
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
     * The budget of a participant acting on the current turn — its holder, or a mount acting with
     * it — made the first time it is asked for and dropped when the (round, turn) key changes. Making
     * one starts that participant's turn for the per-turn flags: its own dodge/disengage lapse and
     * any Help it granted expires.
     */
    public TurnBudget budgetFor(UUID actor, double movementBudget) {
        int round = turnOrder.round();
        UUID turn = turnOrder.currentTurn().orElse(null);
        if (round != budgetRound || !Objects.equals(turn, budgetTurn)) {
            budgetRound = round;
            budgetTurn = turn;
            budgets.clear();
            movementAnchor = null;
            teleported = false;
        }
        return budgets.computeIfAbsent(actor, participant -> {
            TurnBudget budget = new TurnBudget();
            budget.reset(movementBudget);
            flags.beginTurn(participant);
            return budget;
        });
    }

    /** The turn holder's budget, as of the last time one was asked for. */
    public TurnBudget budget() {
        return budgets.getOrDefault(budgetTurn, new TurnBudget());
    }

    public EncounterFlags flags() {
        return flags;
    }

    /** Holds the fight until {@code landed} answers true. */
    public void hold(BooleanSupplier landed) {
        holds.add(landed);
    }

    /** Whether a hit on its way still holds the fight: no turn ends and no one acts until it lands. */
    public boolean held() {
        holds.removeIf(BooleanSupplier::getAsBoolean);
        return !holds.isEmpty();
    }

    public Vec3 movementAnchor() {
        return movementAnchor;
    }

    public void setMovementAnchor(Vec3 anchor) {
        movementAnchor = anchor;
    }

    /** The acting participant teleported this tick, so the movement it is charged for was not a walk. */
    public void noteTeleport() {
        teleported = true;
    }

    /** Whether the acting participant teleported since the last ask; asking clears it. */
    public boolean consumeTeleport() {
        boolean noted = teleported;
        teleported = false;
        return noted;
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
