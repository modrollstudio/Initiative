package studio.modroll.initiative.action;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.config.CoverConfig;

/**
 * Line-of-sight cover. The tier is derived from the fraction of sampled rays from the attacker's
 * eye to the target's hitbox that solid blocks obstruct; the tier maps to a defender AC bonus that
 * feeds Critfall's own roll via {@code AttackContext.withDefenderAcBonus}.
 *
 * <p>Sampling: eight rays, one to each corner of the target's bounding box, pulled slightly inward so
 * a target flush against a wall is sampled by its body rather than its skin. A block obstructs a ray
 * when Minecraft's own collision clip ({@link ClipContext.Block#COLLIDER}) stops it before the
 * corner — the same physical predicate vanilla uses for line of sight and projectile travel, so any
 * block with a collision shape counts and no hand-maintained block list is needed. Entities never
 * obstruct (deferred). Eight fixed rays with no early-out; attacks are infrequent enough that the
 * per-attack cost is negligible.
 */
public final class Cover {

    private static final double HITBOX_INSET = 0.1;

    public enum Tier {
        NONE,
        HALF,
        THREE_QUARTER,
        TOTAL
    }

    private Cover() {}

    /** The cover tier the target enjoys against this attacker right now; recomputed per attack. */
    public static Tier against(Level level, LivingEntity attacker, LivingEntity target, CoverConfig cover) {
        if (!cover.enabled()) {
            return Tier.NONE;
        }
        return tierOf(obstructedFraction(level, attacker, target), cover);
    }

    private static double obstructedFraction(Level level, LivingEntity attacker, LivingEntity target) {
        Vec3 eye = attacker.getEyePosition();
        AABB box = target.getBoundingBox().deflate(HITBOX_INSET);
        int obstructed = 0;
        int rays = 0;
        for (double x : new double[] {box.minX, box.maxX}) {
            for (double y : new double[] {box.minY, box.maxY}) {
                for (double z : new double[] {box.minZ, box.maxZ}) {
                    rays++;
                    if (isObstructed(level, attacker, eye, new Vec3(x, y, z))) {
                        obstructed++;
                    }
                }
            }
        }
        return (double) obstructed / rays;
    }

    private static boolean isObstructed(Level level, LivingEntity attacker, Vec3 eye, Vec3 corner) {
        ClipContext context =
                new ClipContext(eye, corner, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker);
        return level.clip(context).getType() == HitResult.Type.BLOCK;
    }

    static Tier tierOf(double obstructedFraction, CoverConfig cover) {
        if (obstructedFraction >= 1.0) {
            return Tier.TOTAL;
        }
        if (obstructedFraction >= cover.threeQuarterCoverThreshold()) {
            return Tier.THREE_QUARTER;
        }
        if (obstructedFraction >= cover.halfCoverThreshold()) {
            return Tier.HALF;
        }
        return Tier.NONE;
    }

    static int acBonus(Tier tier, CoverConfig cover) {
        return switch (tier) {
            case NONE -> 0;
            case HALF -> cover.halfCoverAcBonus();
            case THREE_QUARTER, TOTAL -> cover.threeQuarterCoverAcBonus();
        };
    }
}
