package studio.modroll.initiative.action;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * What an arrow does to a target its hurt landed on, past the damage: a Punch knockback, the arrow's
 * own effects (a tipped arrow's potion, a spectral arrow's glow) and, for a burning arrow, five
 * seconds of fire. Initiative cancels the arrow's hurt and drives a rolled one in its place, so
 * vanilla never gets to run this half for a hit. It lives on {@code AbstractArrow}'s protected side,
 * so each loader's arrow mixin implements this.
 */
public interface StruckArrow {

    void initiative$struck(LivingEntity target, DamageSource source);
}
