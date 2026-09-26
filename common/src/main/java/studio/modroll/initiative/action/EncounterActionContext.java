package studio.modroll.initiative.action;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.initiative.api.ActionContext;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;

/** The live view an action effect gets: its actor, its target, and the encounter around them. */
record EncounterActionContext(
        ServerLevel level,
        Encounter encounter,
        LivingEntity actor,
        ActionRequest request,
        ResourceLocation id,
        TurnBudget budget)
        implements ActionContext {

    @Override
    public Optional<LivingEntity> target() {
        return request.target();
    }

    @Override
    public Optional<Vec3> position() {
        return request.position();
    }

    @Override
    public boolean isParticipant(LivingEntity entity) {
        return entity != null && encounter.contains(entity.getUUID());
    }

    @Override
    public boolean areEnemies(LivingEntity a, LivingEntity b) {
        if (!isParticipant(a) || !isParticipant(b)) {
            return false;
        }
        return encounter.side(a.getUUID()) != encounter.side(b.getUUID());
    }

    @Override
    public List<LivingEntity> participants() {
        List<LivingEntity> living = new ArrayList<>();
        for (UUID participant : encounter.participants()) {
            if (level.getEntity(participant) instanceof LivingEntity entity && entity.isAlive()) {
                living.add(entity);
            }
        }
        return List.copyOf(living);
    }

    /** The budget belongs to whoever is acting, so a reaction — taken off-turn — cannot touch it. */
    @Override
    public boolean spendMovement(double blocks) {
        if (budget == null || budget.movementRemaining() < blocks) {
            return false;
        }
        budget.consumeMovement(blocks);
        return true;
    }

    @Override
    public void grantMovement(double blocks) {
        if (budget != null) {
            budget.addMovement(blocks);
        }
    }

    @Override
    public void showRoll(AttackResult result, LivingEntity opponent) {
        RollAnimationSync.play(actor, opponent, RollAnimation.attack(result, nameOf(actor)));
    }

    @Override
    public void showRoll(ContestResult result, LivingEntity opponent) {
        RollAnimationSync.play(actor, opponent, RollAnimation.contest(result, nameOf(actor), nameOf(opponent)));
    }

    private static String nameOf(LivingEntity entity) {
        return entity.getDisplayName().getString();
    }

    @Override
    public Optional<JsonObject> settings() {
        return ActionSettings.get(id);
    }
}
