package studio.modroll.initiative.action;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.api.ActionContext;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.config.CoverConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;

/** The reaction seam: something happened on the actor's turn, resolve any eligible reactions. */
public final class Reactions {

    private Reactions() {}

    /** True when the mover was within horizontal {@code reach} of the reactor before the move and outside it after. */
    static boolean leftReach(Vec3 from, Vec3 to, Vec3 reactor, double reach) {
        double reachSqr = reach * reach;
        return horizontalDistanceSqr(from, reactor) <= reachSqr && horizontalDistanceSqr(to, reactor) > reachSqr;
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /**
     * Resolves an opportunity attack for every eligible hostile reactor whose reach {@code mover}
     * left this turn. Each reactor's attack is driven through {@code RollService.performAttack} in
     * isolation — this never touches {@link studio.modroll.initiative.turn.TurnOrder}, the mover's
     * own {@link TurnBudget}, or {@link AiFreeze}.
     */
    public static void onMovedWithinTurn(
            ServerLevel level, Encounter encounter, LivingEntity mover, Vec3 fromPos, Vec3 toPos) {
        if (!InitiativeConfig.actions().opportunityAttack()) {
            return;
        }
        if (encounter.flags().isDisengaged(mover.getUUID())) {
            return;
        }
        double reach = InitiativeConfig.actions().opportunityAttackReachBlocks();
        Encounter.Side moverSide = encounter.side(mover.getUUID());
        for (UUID candidate : encounter.participants()) {
            // An earlier reactor's opportunity attack may have killed the mover; later reactors must not attack a
            // corpse.
            if (!mover.isAlive()) {
                return;
            }
            if (candidate.equals(mover.getUUID()) || encounter.side(candidate) == moverSide) {
                continue;
            }
            if (!encounter.flags().hasReaction(candidate)) {
                continue;
            }
            if (!(level.getEntity(candidate) instanceof LivingEntity reactor) || !reactor.isAlive()) {
                continue;
            }
            if (leftReach(fromPos, toPos, reactor.position(), reach)) {
                ActionRegistry.invoke(level, reactor, BuiltinActions.OPPORTUNITY_ATTACK, ActionRequest.of(mover));
            }
        }
    }

    /**
     * The opportunity attack itself. A mover in total cover suppresses it from inside the effect, so
     * the rejection leaves the reactor's reaction unspent — the pipeline charges performed actions only.
     */
    static ActionResult performOpportunityAttack(ActionContext context) {
        ServerLevel level = context.level();
        LivingEntity reactor = context.actor();
        LivingEntity mover = context.target().orElseThrow();
        CoverConfig cover = InitiativeConfig.cover();
        Cover.Tier tier = Cover.against(level, reactor, mover, cover);
        if (tier == Cover.Tier.TOTAL && cover.totalCoverBlocksAttack()) {
            return ActionResult.rejected(ActionStatus.TOTAL_COVER);
        }
        AttackResult result = ActionEconomy.drive(level, reactor, mover, RollMode.NORMAL, Cover.acBonus(tier, cover));
        if (result.isHit()) {
            ActionEconomy.encounterOf(level, reactor).flags().breakHidden(mover.getUUID());
        }
        context.showRoll(result, mover);
        return ActionResult.attacked(result);
    }
}
