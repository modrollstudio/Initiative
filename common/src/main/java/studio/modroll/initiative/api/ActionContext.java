package studio.modroll.initiative.api;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.combat.ContestResult;

/**
 * Everything an action effect may touch. Deliberately narrow: it carries no Initiative internal
 * type, so a mod's action and a built-in see the same surface. Rolls are presented through
 * {@link #showRoll} rather than by reaching for the animation sync, which is what makes the roll
 * drama uniform across every registered action.
 */
public interface ActionContext {

    ServerLevel level();

    LivingEntity actor();

    Optional<LivingEntity> target();

    Optional<Vec3> position();

    boolean isParticipant(LivingEntity entity);

    boolean areEnemies(LivingEntity a, LivingEntity b);

    List<LivingEntity> participants();

    /** Spends movement budget if it can be afforded; returns whether it was. */
    boolean spendMovement(double blocks);

    /** Adds movement to this turn's budget, on top of whatever is left (Dash and its kin). */
    void grantMovement(double blocks);

    void showRoll(AttackResult result, LivingEntity opponent);

    void showRoll(ContestResult result, LivingEntity opponent);

    /** This action's datapack settings, when a pack supplied any. */
    Optional<JsonObject> settings();
}
