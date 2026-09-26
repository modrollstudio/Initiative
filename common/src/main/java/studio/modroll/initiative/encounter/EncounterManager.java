package studio.modroll.initiative.encounter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import studio.modroll.critfall.api.AttackDelivery;
import studio.modroll.critfall.api.CombatSuppression;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.DiceExpression;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.api.dice.RollResult;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.Concealment;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.hud.TurnOrderSync;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;
import studio.modroll.initiative.turn.AiFreeze;
import studio.modroll.initiative.turn.InitiativeDerivation;
import studio.modroll.initiative.turn.InitiativeEntry;
import studio.modroll.initiative.turn.OffTurnRestriction;
import studio.modroll.initiative.turn.TurnTimeout;
import studio.modroll.initiative.ui.ActionUiSync;

/**
 * Server-side encounter tracking. Members are stored by UUID so every exit path (death, unload,
 * dimension change, flee, server stop) can release Critfall's suppression flag without needing the
 * entity to still exist.
 */
public final class EncounterManager {

    private static final Map<ServerLevel, List<Encounter>> ACTIVE = new HashMap<>();
    private static final DiceExpression D20 = DiceExpression.parse("1d20");
    private static final int MARKER_INTERVAL_TICKS = 5;

    private EncounterManager() {}

    public static void onCombat(LivingEntity attacker, LivingEntity victim, AttackDelivery delivery) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        EncounterConfig config = InitiativeConfig.encounters();
        if (!config.enabled()) {
            return;
        }
        if (attacker == victim) {
            return;
        }
        boolean playerAttacking = attacker instanceof Player;
        boolean playerAttacked = victim instanceof Player;
        if (playerAttacking == playerAttacked) {
            return;
        }
        if (playerAttacking && !config.triggerOnPlayerAttacking()) {
            return;
        }
        if (playerAttacked && !config.triggerOnPlayerAttacked()) {
            return;
        }
        LivingEntity hostile = playerAttacking ? victim : attacker;
        Player player = (Player) (playerAttacking ? attacker : victim);
        if (!triggers(hostile, player, config) || !hostile.isAlive() || !attacker.isAlive()) {
            return;
        }
        Encounter existing = encounterContaining(level, attacker.getUUID())
                .or(() -> encounterContaining(level, victim.getUUID()))
                .orElse(null);
        if (existing != null) {
            join(level, existing, attacker);
            join(level, existing, victim);
            noteRangedAttack(existing, attacker, victim, delivery);
            return;
        }
        Encounter encounter = new Encounter(victim.position());
        ACTIVE.computeIfAbsent(level, l -> new ArrayList<>()).add(encounter);
        join(level, encounter, attacker);
        join(level, encounter, victim);
        noteRangedAttack(encounter, attacker, victim, delivery);
        pullInRadius(level, encounter, config.triggerRadius());
    }

    /**
     * A shot that reached a participant is what put the shooter in the fight — proximity had nothing
     * to do with it — so the fight remembers that and stops measuring it against the leave radius.
     * Both ends must be in this fight: a shot into someone else's, or one the one-encounter guard
     * turned away, buys the shooter nothing. Only the hostile side is marked, since a player who
     * shoots into a bubble joins it as a player.
     */
    private static void noteRangedAttack(
            Encounter encounter, LivingEntity attacker, LivingEntity victim, AttackDelivery delivery) {
        if (isRanged(delivery)
                && encounter.contains(victim.getUUID())
                && encounter.side(attacker.getUUID()) == Encounter.Side.HOSTILE) {
            encounter.markRangedAggressor(attacker.getUUID());
        }
    }

    /** Critfall's own split: a bow or a thrown trident is ranged, an explosion or a touch is not. */
    private static boolean isRanged(AttackDelivery delivery) {
        return delivery == AttackDelivery.PROJECTILE || delivery == AttackDelivery.THROWN;
    }

    /**
     * An always-hostile mob triggers exactly as it always has. A neutral one triggers only once it
     * is already hostile toward this player — the mob that swung at you, or the one you provoked a
     * moment ago — which is what keeps a hit cow out of initiative.
     */
    private static boolean triggers(LivingEntity hostile, Player player, EncounterConfig config) {
        return hostile instanceof Enemy
                || config.triggerOnProvokedNeutral()
                        && hostile instanceof Mob mob
                        && Provocation.isProvokedBy(mob, player);
    }

    /**
     * A logged-out player leaves the encounter at once rather than waiting for the tick that would
     * notice their entity is gone: their turn passes on, their conditions are dropped and Critfall's
     * suppression is released, so nothing of theirs can outlive the session. Reconnecting puts them
     * back in the world outside the encounter; a fresh attack forms or joins one again.
     */
    public static void playerDisconnected(ServerLevel level, UUID player) {
        encounterContaining(level, player).ifPresent(encounter -> removeParticipant(level, encounter, player));
    }

    public static void tick(ServerLevel level) {
        EncounterConfig config = InitiativeConfig.encounters();
        if (!config.enabled()) {
            levelUnloaded(level);
            return;
        }
        if (config.triggerOnProvokedNeutral()) {
            formProvokedEncounters(level, config);
        } else {
            Provocation.forget(level);
        }
        List<Encounter> encounters = ACTIVE.get(level);
        if (encounters == null) {
            return;
        }
        TurnConfig turns = InitiativeConfig.turns();
        Iterator<Encounter> iterator = encounters.iterator();
        while (iterator.hasNext()) {
            Encounter encounter = iterator.next();
            releaseInvalidMembers(level, encounter, config.leaveRadius());
            if (encounter.isOver()) {
                TurnOrderSync.encounterEnding(level, encounter);
                ActionUiSync.encounterEnding(level, encounter);
                releaseAll(level, encounter);
                iterator.remove();
            } else {
                pullInRadius(level, encounter, config.triggerRadius());
                if (turns.enabled()) {
                    encounter.turnOrder().tick(TurnTimeout.forCurrentActor(level, encounter.turnOrder(), turns));
                }
                ActionEconomy.tick(level, encounter);
                applyFreeze(level, encounter, turns);
                Concealment.reconcile(level, encounter);
                OffTurnRestriction.reconcile(level, encounter, turns);
                TurnOrderSync.sync(level, encounter);
                ActionUiSync.sync(level, encounter);
            }
        }
        if (encounters.isEmpty()) {
            ACTIVE.remove(level);
        }
    }

    public static void levelUnloaded(ServerLevel level) {
        Provocation.forget(level);
        List<Encounter> encounters = ACTIVE.remove(level);
        if (encounters == null) {
            return;
        }
        encounters.forEach(encounter -> releaseAll(level, encounter));
    }

    public static void serverStopping() {
        ACTIVE.forEach((level, encounters) -> encounters.forEach(encounter -> releaseAll(level, encounter)));
        ACTIVE.clear();
        AiFreeze.clear();
        OffTurnRestriction.clear();
        Concealment.clear();
        Provocation.clear();
    }

    public static List<Encounter> encounters(ServerLevel level) {
        return List.copyOf(ACTIVE.getOrDefault(level, List.of()));
    }

    public static Optional<Encounter> encounterContaining(ServerLevel level, UUID participant) {
        return ACTIVE.getOrDefault(level, List.of()).stream()
                .filter(encounter -> encounter.contains(participant))
                .findFirst();
    }

    /** An entity belongs to exactly one encounter, so one already fighting elsewhere is left alone. */
    private static void join(ServerLevel level, Encounter encounter, LivingEntity entity) {
        if (encounter.contains(entity.getUUID())
                || encounterContaining(level, entity.getUUID()).isPresent()) {
            return;
        }
        Encounter.Side side = entity instanceof Player ? Encounter.Side.PLAYER : Encounter.Side.HOSTILE;
        encounter.add(entity.getUUID(), side, entity.level().getGameTime());
        RollService.suppress(entity);
        TurnConfig turns = InitiativeConfig.turns();
        if (turns.enabled()) {
            encounter.turnOrder().insert(rollInitiative(entity, turns));
        }
    }

    private static InitiativeEntry rollInitiative(LivingEntity entity, TurnConfig turns) {
        int bonus = InitiativeDerivation.bonus(entity.getAttributeValue(Attributes.MOVEMENT_SPEED), turns);
        RollResult roll = RollService.roll(D20);
        RollAnimationSync.play(
                entity,
                RollAnimation.initiative(
                        RollDetail.of(RollMode.NORMAL, roll),
                        entity.getDisplayName().getString()));
        return new InitiativeEntry(entity.getUUID(), roll.total() + bonus, bonus);
    }

    /**
     * A mob that turned on someone joins the fight on the tick after it did, rather than from
     * inside the goal that was setting its target. The provoking hit itself has already landed in
     * real time by then, exactly as it does for an always-hostile mob.
     */
    private static void formProvokedEncounters(ServerLevel level, EncounterConfig config) {
        for (UUID provoked : Provocation.drainPending(level)) {
            if (!(level.getEntity(provoked) instanceof Mob mob)
                    || !mob.isAlive()
                    || encounterContaining(level, provoked).isPresent()) {
                continue;
            }
            provokerNear(level, mob, config.triggerRadius())
                    .ifPresent(player -> beginOrJoin(level, config, mob, player));
        }
    }

    private static Optional<Player> provokerNear(ServerLevel level, Mob mob, double radius) {
        AABB box = mob.getBoundingBox().inflate(radius);
        return level
                .getEntitiesOfClass(
                        Player.class,
                        box,
                        player -> player.isAlive()
                                && mob.distanceToSqr(player) <= radius * radius
                                && Provocation.isProvokedBy(mob, player))
                .stream()
                .findFirst();
    }

    private static void beginOrJoin(ServerLevel level, EncounterConfig config, Mob mob, Player player) {
        Encounter existing = encounterContaining(level, player.getUUID()).orElse(null);
        if (existing != null) {
            if (existing.isWithin(mob.position(), config.triggerRadius())) {
                join(level, existing, mob);
            }
            return;
        }
        Encounter encounter = new Encounter(mob.position());
        ACTIVE.computeIfAbsent(level, l -> new ArrayList<>()).add(encounter);
        join(level, encounter, player);
        join(level, encounter, mob);
        pullInRadius(level, encounter, config.triggerRadius());
    }

    /**
     * The radius sweep, run when a fight forms and on every tick after: whoever belongs in the fight
     * and stands inside the bubble joins it, rolling initiative into the running order as they do.
     * Anyone already fighting elsewhere is left where they are.
     */
    private static void pullInRadius(ServerLevel level, Encounter encounter, double radius) {
        AABB box = AABB.ofSize(encounter.center(), radius * 2, radius * 2, radius * 2);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive()
                        && encounter.isWithin(entity.position(), radius)
                        && belongsInFight(level, encounter, entity)
                        && encounterContaining(level, entity.getUUID()).isEmpty())) {
            join(level, encounter, entity);
        }
    }

    /**
     * The whole party rolls initiative: a player inside the bubble is pulled in with the fight, and
     * one who walks in later joins mid-fight — unless they are only watching (spectator) or outside
     * the rules (creative), or {@code pull_nearby_players} is off and attacking is the only way in.
     * Always-hostile mobs are pulled in on sight; a neutral one only once it is hostile toward a
     * player already fighting here — so an angry wolf that walks into the bubble joins and the sheep
     * next to it does not.
     */
    private static boolean belongsInFight(ServerLevel level, Encounter encounter, LivingEntity entity) {
        if (entity instanceof Player player) {
            return InitiativeConfig.encounters().pullNearbyPlayers() && !player.isSpectator() && !player.isCreative();
        }
        return entity instanceof Enemy
                || InitiativeConfig.encounters().triggerOnProvokedNeutral()
                        && entity instanceof Mob mob
                        && isProvokedByAPlayerHere(level, encounter, mob);
    }

    private static boolean isProvokedByAPlayerHere(ServerLevel level, Encounter encounter, Mob mob) {
        for (UUID participant : encounter.participants()) {
            if (encounter.side(participant) == Encounter.Side.PLAYER
                    && level.getEntity(participant) instanceof Player player
                    && Provocation.isProvokedBy(mob, player)) {
                return true;
            }
        }
        return false;
    }

    private static void releaseInvalidMembers(ServerLevel level, Encounter encounter, double leaveRadius) {
        for (UUID participant : encounter.participants()) {
            Entity entity = level.getEntity(participant);
            boolean valid = entity != null
                    && entity.isAlive()
                    && (encounter.isWithin(entity.position(), leaveRadius)
                            || stillShootingIntoTheFight(level, encounter, entity))
                    && stillHostile(level, encounter, entity);
            if (!valid) {
                removeParticipant(level, encounter, participant);
            }
        }
    }

    /**
     * The distance exit means "you left the fight", which is not what a skeleton plinking away from
     * thirty blocks has done — so a participant that shot its way in keeps its place while it still
     * has someone here in its sights. Losing the target hands it back to the leave radius, and so
     * does turning the flag off, within a tick either way. The hold reads targeting the same way the
     * neutral leave path does, concealment stand-down included: a hider is why the sights dropped.
     */
    private static boolean stillShootingIntoTheFight(ServerLevel level, Encounter encounter, Entity entity) {
        return InitiativeConfig.encounters().holdRangedAttackers()
                && encounter.isRangedAggressor(entity.getUUID())
                && entity instanceof Mob mob
                && (Concealment.holdsNeutralsInTheFight(encounter) || isProvokedByAPlayerHere(level, encounter, mob));
    }

    /**
     * A former neutral is in the fight only while it is still hostile to a player in it: anger that
     * runs out is a leave path like fleeing the bubble, and it takes the same exit. Players and
     * always-hostile mobs are never asked — a husk that loses sight of you is still a husk. While
     * someone in the fight is hidden the question cannot be answered honestly — concealment is what
     * took the target — so the exit stands down until they are seen again.
     */
    private static boolean stillHostile(ServerLevel level, Encounter encounter, Entity entity) {
        if (!InitiativeConfig.encounters().triggerOnProvokedNeutral()
                || !(entity instanceof Mob mob)
                || !Provocation.isNeutral(mob)) {
            return true;
        }
        return Concealment.holdsNeutralsInTheFight(encounter) || isProvokedByAPlayerHere(level, encounter, mob);
    }

    /**
     * The one exit path for a single participant: suppression released, AI thawed, both client views
     * cleared and every per-encounter flag it held — including a grapple in either direction —
     * dropped by {@link Encounter#remove}.
     */
    private static void removeParticipant(ServerLevel level, Encounter encounter, UUID participant) {
        CombatSuppression.release(participant);
        AiFreeze.thaw(participant);
        OffTurnRestriction.release(level, participant);
        Concealment.reveal(participant);
        TurnOrderSync.participantRemoved(level, encounter, participant);
        ActionUiSync.participantRemoved(level, encounter, participant);
        encounter.remove(participant);
    }

    private static void releaseAll(ServerLevel level, Encounter encounter) {
        for (UUID participant : encounter.participants()) {
            CombatSuppression.release(participant);
            AiFreeze.thaw(participant);
            OffTurnRestriction.release(level, participant);
            Concealment.reveal(participant);
            encounter.remove(participant);
        }
    }

    /**
     * Reconciles freeze state against the turn every tick: all mob participants except the acting
     * one are frozen, the acting one and excluded types thaw. The acting mob's real-time window is
     * bounded by the action economy — once its movement budget is gone it freezes in place too.
     * Reconciliation instead of transition events means a flipped config flag or a swapped turn
     * heals within one tick, and no exit path can strand a frozen mob that is still ticked here.
     * Players are never frozen: a human sends input instead of running an AI, so
     * {@link OffTurnRestriction} holds them off-turn instead.
     */
    private static void applyFreeze(ServerLevel level, Encounter encounter, TurnConfig turns) {
        boolean freezeActive = turns.enabled() && turns.freezeEnabled();
        UUID current = encounter.turnOrder().currentTurn().orElse(null);
        for (UUID participant : encounter.participants()) {
            boolean acting = participant.equals(current);
            if (!freezeActive || (acting && !ActionEconomy.actingMovementExhausted(encounter))) {
                AiFreeze.thaw(participant);
                continue;
            }
            if (level.getEntity(participant) instanceof Mob mob && !AiFreeze.isExcluded(mob, turns)) {
                AiFreeze.freeze(participant);
            } else {
                AiFreeze.thaw(participant);
            }
        }
        if (turns.enabled() && turns.actingMarkerEnabled() && current != null) {
            markActingEntity(level, current);
        }
    }

    /** An in-world particle hint above whoever is acting, beside the turn HUD rather than instead of it. */
    private static void markActingEntity(ServerLevel level, UUID current) {
        if (level.getGameTime() % MARKER_INTERVAL_TICKS != 0) {
            return;
        }
        Entity entity = level.getEntity(current);
        if (entity == null) {
            return;
        }
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                entity.getX(),
                entity.getY() + entity.getBbHeight() + 0.4,
                entity.getZ(),
                2,
                0.2,
                0.1,
                0.2,
                0.0);
    }
}
