package studio.modroll.initiative.action;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.AttackContext;
import studio.modroll.critfall.api.ContestContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.api.ActionContext;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.checks.ChecksBridge;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.CoverConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.encounter.Provocation;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;

/**
 * The turn actions and the per-tick turn reconcile. Every action runs through
 * {@link ActionRegistry}; the methods here are the callers' front door (loader hooks, mob turns) and
 * narrow the registry's {@link ActionStatus} down to the coarser outcomes those callers act on.
 * Initiative is the sole combat driver inside encounters: participant-on-participant vanilla damage
 * is canceled unless it is Critfall's own driven hurt, which the public
 * {@code RollService.isDrivenDamage} reports.
 */
public final class ActionEconomy {

    /** Outcome of an attack swing, as the loaders' attack hooks read it. */
    public enum AttackStatus {
        PERFORMED,
        INACTIVE,
        NOT_IN_ENCOUNTER,
        TARGET_OUTSIDE_ENCOUNTER,
        INVALID_TARGET,
        NOT_YOUR_TURN,
        NO_ACTION,
        TOTAL_COVER
    }

    /** The swing is vanilla's: Initiative is not driving this one and must not cancel it. */
    public static boolean leavesToVanilla(AttackStatus status) {
        return status == AttackStatus.INACTIVE
                || status == AttackStatus.NOT_IN_ENCOUNTER
                || status == AttackStatus.TARGET_OUTSIDE_ENCOUNTER;
    }

    public record AttackAttempt(AttackStatus status, Optional<AttackResult> result) {}

    /** Outcome of a standard action (Dash, Disengage, Dodge, Help); mirrors the attack rejections. */
    public enum ActionOutcome {
        PERFORMED,
        INACTIVE,
        DISABLED,
        NOT_IN_ENCOUNTER,
        NOT_YOUR_TURN,
        NO_ACTION,
        INVALID_TARGET,
        OUT_OF_REACH,
        MISSING_ITEM
    }

    public record HideAttempt(ActionOutcome status, boolean hidden) {}

    private ActionEconomy() {}

    /** The acting participant's budget; a grappled participant's turn starts with no movement. */
    public static TurnBudget budgetOf(Encounter encounter, UUID participant) {
        return encounter.budgetFor(
                encounter.turnOrder().round(), participant, Grapples.movementBudgetFor(encounter, participant));
    }

    public static boolean active() {
        return InitiativeConfig.turns().enabled() && InitiativeConfig.actions().enabled();
    }

    public static AttackAttempt attack(ServerLevel level, LivingEntity attacker, LivingEntity target) {
        ActionResult result = invoke(level, attacker, BuiltinActions.ATTACK, ActionRequest.of(target));
        AttackStatus status = attackStatus(result.status());
        if (status == AttackStatus.INVALID_TARGET && isRealTimeBystander(level, target)) {
            return new AttackAttempt(AttackStatus.TARGET_OUTSIDE_ENCOUNTER, Optional.empty());
        }
        return new AttackAttempt(status, result.attack());
    }

    /**
     * A neutral mob outside the fight is not a target the economy can drive — it is not in the turn
     * order — but refusing the swing would deadlock provocation, since that swing is what turns the
     * mob hostile and pulls it in. So it falls back to a plain vanilla hit, exactly like the one
     * that starts an encounter in the first place: no roll driven, no action spent. The rejection is
     * read first, so the turn and economy still gate the swing; only the target check is relaxed.
     */
    private static boolean isRealTimeBystander(ServerLevel level, LivingEntity target) {
        return InitiativeConfig.encounters().triggerOnProvokedNeutral()
                && Provocation.isUnclaimedNeutral(level, target);
    }

    public static ActionOutcome dash(ServerLevel level, LivingEntity actor) {
        return outcome(invoke(level, actor, BuiltinActions.DASH, ActionRequest.none()));
    }

    public static ActionOutcome disengage(ServerLevel level, LivingEntity actor) {
        return outcome(invoke(level, actor, BuiltinActions.DISENGAGE, ActionRequest.none()));
    }

    public static ActionOutcome dodge(ServerLevel level, LivingEntity actor) {
        return outcome(invoke(level, actor, BuiltinActions.DODGE, ActionRequest.none()));
    }

    public static ActionOutcome help(ServerLevel level, LivingEntity actor, LivingEntity ally) {
        return outcome(invoke(level, actor, BuiltinActions.HELP, ActionRequest.of(ally)));
    }

    public static HideAttempt hide(ServerLevel level, LivingEntity actor) {
        ActionResult result = invoke(level, actor, BuiltinActions.HIDE, ActionRequest.none());
        return new HideAttempt(outcome(result), result.succeeded());
    }

    public static boolean endTurn(ServerLevel level, LivingEntity actor) {
        return invoke(level, actor, BuiltinActions.END_TURN, ActionRequest.none())
                .isPerformed();
    }

    private static ActionResult invoke(
            ServerLevel level, LivingEntity actor, ResourceLocation id, ActionRequest request) {
        return ActionRegistry.invoke(level, actor, id, request);
    }

    static ActionOutcome outcome(ActionResult result) {
        return switch (result.status()) {
            case PERFORMED -> ActionOutcome.PERFORMED;
            case DISABLED -> ActionOutcome.DISABLED;
            case NOT_IN_ENCOUNTER -> ActionOutcome.NOT_IN_ENCOUNTER;
            case NOT_YOUR_TURN -> ActionOutcome.NOT_YOUR_TURN;
            case NO_ACTION, NO_BONUS_ACTION, NO_REACTION, NO_MOVEMENT -> ActionOutcome.NO_ACTION;
            case INVALID_TARGET, TOTAL_COVER -> ActionOutcome.INVALID_TARGET;
            case OUT_OF_REACH -> ActionOutcome.OUT_OF_REACH;
            case MISSING_ITEM, HANDS_FULL -> ActionOutcome.MISSING_ITEM;
            case INACTIVE -> ActionOutcome.INACTIVE;
        };
    }

    private static AttackStatus attackStatus(ActionStatus status) {
        return switch (status) {
            case PERFORMED -> AttackStatus.PERFORMED;
            case NOT_IN_ENCOUNTER -> AttackStatus.NOT_IN_ENCOUNTER;
            case NOT_YOUR_TURN -> AttackStatus.NOT_YOUR_TURN;
            case NO_ACTION, NO_BONUS_ACTION, NO_REACTION, NO_MOVEMENT -> AttackStatus.NO_ACTION;
            case INVALID_TARGET, OUT_OF_REACH, MISSING_ITEM, HANDS_FULL -> AttackStatus.INVALID_TARGET;
            case TOTAL_COVER -> AttackStatus.TOTAL_COVER;
            case INACTIVE, DISABLED -> AttackStatus.INACTIVE;
        };
    }

    /**
     * The Attack action. Cover is recomputed here rather than gated by the pipeline because a total
     * miss of line must leave the turn's action intact — the pipeline charges only performed actions.
     */
    static ActionResult performAttack(ActionContext context) {
        ServerLevel level = context.level();
        LivingEntity attacker = context.actor();
        LivingEntity target = context.target().orElseThrow();
        Encounter encounter = encounterOf(level, attacker);
        CoverConfig cover = InitiativeConfig.cover();
        Cover.Tier tier = Cover.against(level, attacker, target, cover);
        if (tier == Cover.Tier.TOTAL && cover.totalCoverBlocksAttack()) {
            return ActionResult.rejected(ActionStatus.TOTAL_COVER);
        }
        RollMode mode = attackMode(encounter, InitiativeConfig.actions(), attacker, target);
        AttackResult result = drive(level, attacker, target, mode, Cover.acBonus(tier, cover));
        if (result.isHit()) {
            encounter.flags().breakHidden(target.getUUID());
        }
        context.showRoll(result, target);
        return ActionResult.attacked(result);
    }

    /** Dash: extra movement equal to the base budget, spending the action. */
    static ActionResult performDash(ActionContext context) {
        context.grantMovement(InitiativeConfig.actions().movementBudgetBlocks());
        return ActionResult.performed();
    }

    /** Disengage: sets the flag opportunity attacks read. */
    static ActionResult performDisengage(ActionContext context) {
        flagsOf(context).setDisengaged(context.actor().getUUID());
        return ActionResult.performed();
    }

    /** Dodge: attacks against this participant roll with disadvantage until its next turn. */
    static ActionResult performDodge(ActionContext context) {
        flagsOf(context).setDodging(context.actor().getUUID());
        return ActionResult.performed();
    }

    /** Help: grants an ally advantage on its next attack. */
    static ActionResult performHelp(ActionContext context) {
        flagsOf(context)
                .grantHelpAdvantage(
                        context.target().orElseThrow().getUUID(),
                        context.actor().getUUID());
        return ActionResult.performed();
    }

    /**
     * Hide: a Stealth-vs-Perception contest against the nearest hostile observer. With Checks active
     * the hider instead rolls Stealth and hides; the total is kept for {@link PassivePerception} to
     * check each enemy against on its turn.
     */
    static ActionResult performHide(ActionContext context) {
        if (ChecksIntegration.active()) {
            return hideWithStealthCheck(context);
        }
        LivingEntity actor = context.actor();
        LivingEntity observer = nearestEnemy(context);
        boolean hidden = observer == null || hideContestWon(context, observer, InitiativeConfig.actions());
        if (hidden) {
            flagsOf(context).setHidden(actor.getUUID());
        }
        return ActionResult.performed(hidden);
    }

    /**
     * Unopposed, so the roll is shown straight through the sync: no opponent for {@code showRoll}. A
     * Stealth check a check event listener canceled rolled nothing: the Hide fails, and no dice are shown.
     */
    private static ActionResult hideWithStealthCheck(ActionContext context) {
        LivingEntity actor = context.actor();
        Optional<ChecksBridge.Rolled> stealth = ChecksBridge.stealthCheck(actor);
        if (stealth.isEmpty()) {
            return ActionResult.performed(false);
        }
        RollAnimationSync.play(
                actor,
                RollAnimation.check(stealth.get().roll(), actor.getDisplayName().getString()));
        flagsOf(context).setHidden(actor.getUUID(), stealth.get().total());
        return ActionResult.performed(true);
    }

    static ActionResult performEndTurn(ActionContext context) {
        encounterOf(context.level(), context.actor()).turnOrder().endTurn();
        return ActionResult.performed();
    }

    private static boolean hideContestWon(ActionContext context, LivingEntity observer, ActionConfig actions) {
        ContestContext contest = ContestContext.of(actions.hideStealthBonus(), actions.hideObserverPerceptionBonus());
        ContestResult result = RollService.contest(context.actor(), observer, contest);
        context.showRoll(result, observer);
        return result.initiatorWins();
    }

    /**
     * The driven roll mode for this attack. Advantage: a Help grant or the attacker's own hidden
     * state (both consumed here, even if the roll ends up canceled, since attacking spends them).
     * Disadvantage: the target is Dodging or is itself hidden — a hidden participant is hard to hit
     * (the target's hidden is only peeked here; it breaks separately when a hit lands). Multiple
     * disadvantage sources (Dodge and hidden) do not stack beyond a single disadvantage (5e), and
     * advantage and disadvantage cancel to a normal roll. Each source is honored only while its
     * action is enabled, so turning one off cleanly restores the plain roll.
     */
    private static RollMode attackMode(
            Encounter encounter, ActionConfig actions, LivingEntity attacker, LivingEntity target) {
        boolean helpAdvantage = actions.help() && encounter.flags().consumeHelpAdvantage(attacker.getUUID());
        boolean hiddenAdvantage = actions.hide() && encounter.flags().consumeHidden(attacker.getUUID());
        boolean advantage = helpAdvantage || hiddenAdvantage;
        boolean targetDodging = actions.dodge() && encounter.flags().isDodging(target.getUUID());
        boolean targetHidden = actions.hide() && encounter.flags().isHidden(target.getUUID());
        boolean disadvantage = targetDodging || targetHidden;
        return resolveMode(advantage, disadvantage);
    }

    static RollMode resolveMode(boolean advantage, boolean disadvantage) {
        if (advantage == disadvantage) {
            return RollMode.NORMAL;
        }
        return advantage ? RollMode.ADVANTAGE : RollMode.DISADVANTAGE;
    }

    static AttackResult drive(
            ServerLevel level, LivingEntity attacker, LivingEntity target, RollMode mode, int defenderAcBonus) {
        DamageSource source = attacker instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(attacker);
        AttackContext ctx =
                AttackContext.melee(source, attacker.getMainHandItem()).withMode(mode);
        if (defenderAcBonus != 0) {
            ctx = ctx.withDefenderAcBonus(defenderAcBonus);
        }
        return RollService.performAttack(attacker, target, ctx);
    }

    static Encounter encounterOf(ServerLevel level, LivingEntity actor) {
        return EncounterManager.encounterContaining(level, actor.getUUID()).orElseThrow();
    }

    static EncounterFlags flagsOf(ActionContext context) {
        return encounterOf(context.level(), context.actor()).flags();
    }

    /** The nearest hostile participant, found through the action context alone. */
    static LivingEntity nearestEnemy(ActionContext context) {
        LivingEntity actor = context.actor();
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : context.participants()) {
            if (!context.areEnemies(actor, candidate)) {
                continue;
            }
            double distance = candidate.distanceToSqr(actor);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /** Per-tick reconcile for the acting participant: budget, movement clamp, mob turn, auto-end. */
    public static void tick(ServerLevel level, Encounter encounter) {
        if (!active()) {
            return;
        }
        UUID current = encounter.turnOrder().currentTurn().orElse(null);
        if (current == null || !(level.getEntity(current) instanceof LivingEntity actor) || !actor.isAlive()) {
            return;
        }
        ActionConfig actions = InitiativeConfig.actions();
        Grapples.reconcile(level, encounter);
        PassivePerception.revealHidersSpottedBy(encounter, actor);
        TurnBudget budget = budgetOf(encounter, current);
        trackMovement(level, encounter, actor, budget);
        if (actor instanceof Mob mob) {
            driveMobTurn(level, encounter, mob, budget);
        }
        if (turnSpent(actor, budget, actions)) {
            encounter.turnOrder().endTurn();
        }
    }

    /**
     * Movement is measured horizontally against the last in-budget position; a tick that would
     * overshoot burns the remaining budget and reverts the actor — a hard stop at the edge.
     * Vertical movement is free. Re-anchor throughout the hurt window so knockback continuing
     * into the actor's turn neither consumes movement nor gets reverted by an exhausted budget.
     */
    private static void trackMovement(ServerLevel level, Encounter encounter, LivingEntity actor, TurnBudget budget) {
        Vec3 pos = actor.position();
        Vec3 anchor = encounter.movementAnchor();
        if (anchor == null || actor.hurtTime > 0) {
            encounter.setMovementAnchor(pos);
            return;
        }
        double delta = Math.hypot(pos.x - anchor.x, pos.z - anchor.z);
        if (delta == 0.0) {
            return;
        }
        if (delta <= budget.movementRemaining()) {
            budget.consumeMovement(delta);
            encounter.setMovementAnchor(pos);
            Reactions.onMovedWithinTurn(level, encounter, actor, anchor, pos);
        } else {
            budget.consumeMovement(budget.movementRemaining());
            actor.teleportTo(anchor.x, anchor.y, anchor.z);
        }
    }

    /** The mob turn: one driven attack on the nearest enemy in melee reach. */
    private static void driveMobTurn(ServerLevel level, Encounter encounter, Mob mob, TurnBudget budget) {
        if (!budget.actionAvailable()) {
            return;
        }
        LivingEntity target = nearestEnemy(level, encounter, mob);
        if (target == null || !mob.isWithinMeleeAttackRange(target)) {
            return;
        }
        if (attack(level, mob, target).status() == AttackStatus.PERFORMED) {
            budget.useAction();
        }
    }

    static LivingEntity nearestEnemy(ServerLevel level, Encounter encounter, LivingEntity actor) {
        Encounter.Side side = encounter.side(actor.getUUID());
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (UUID participant : encounter.participants()) {
            if (encounter.side(participant) == side) {
                continue;
            }
            if (!(level.getEntity(participant) instanceof LivingEntity candidate) || !candidate.isAlive()) {
                continue;
            }
            double distance = candidate.distanceToSqr(actor);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /**
     * Mobs are done once their action is spent; players keep the turn while movement remains. A
     * player ends the turn by clicking End Turn, so the player half defaults off — auto-end is
     * convenience there, while a mob has no UI to click and keeps its own toggle on.
     */
    static boolean turnSpent(LivingEntity actor, TurnBudget budget, ActionConfig actions) {
        if (budget.actionAvailable()) {
            return false;
        }
        if (actor instanceof Player) {
            return actions.endTurnWhenSpent() && budget.movementExhausted();
        }
        return actions.mobEndTurnWhenSpent();
    }

    /**
     * Participant-on-participant vanilla damage is Initiative's to drive, so it is canceled while
     * the economy is active — except Critfall's own driven hurt, which {@code isDrivenDamage}
     * reports while {@code performAttack} is applying rolled damage to the target. Membership is
     * read as of the previous tick ({@link Encounter#joinedBefore}), so the encounter-forming hit —
     * which joins both sides mid-hurt — still lands as plain vanilla damage on every loader.
     */
    public static boolean shouldCancelVanillaDamage(LivingEntity target, DamageSource source) {
        if (!active() || RollService.isDrivenDamage(target)) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == target) {
            return false;
        }
        long now = level.getGameTime();
        return EncounterManager.encounterContaining(level, attacker.getUUID())
                .map(encounter -> encounter.contains(target.getUUID())
                        && encounter.joinedBefore(attacker.getUUID(), now)
                        && encounter.joinedBefore(target.getUUID(), now))
                .orElse(false);
    }

    /** Lets the freeze hold the acting mob in place once its movement budget is gone. */
    public static boolean actingMovementExhausted(Encounter encounter) {
        if (!active()) {
            return false;
        }
        UUID current = encounter.turnOrder().currentTurn().orElse(null);
        if (current == null) {
            return false;
        }
        return budgetOf(encounter, current).movementExhausted();
    }

    public static void sendRejection(Player player, AttackStatus status) {
        String key =
                switch (status) {
                    case NOT_YOUR_TURN -> "initiative.action.not_your_turn";
                    case NO_ACTION -> "initiative.action.no_action";
                    case INVALID_TARGET -> "initiative.action.invalid_target";
                    case TOTAL_COVER -> "initiative.action.total_cover";
                    default -> null;
                };
        if (key != null) {
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        }
    }
}
