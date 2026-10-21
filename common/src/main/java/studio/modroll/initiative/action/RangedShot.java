package studio.modroll.initiative.action;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/**
 * One ranged attack as it was loosed: everything about it that is fixed the moment it is fired rather
 * than when it lands. Every projectile of one volley (a Multishot crossbow) shares the record, so the
 * attacker's advantage, which firing spends, is spent once and counts for each arrow.
 *
 * @param shooter the participant who fired
 * @param origin where the shooter stood, which range is measured from
 * @param eye the shooter's eye, which cover is traced from
 * @param advantage a Help grant or the shooter's own hiding, spent by firing
 * @param inMelee a hostile participant stood within melee reach of the shooter
 * @param firedAt the game tick it was fired, which ties a volley's projectiles together
 */
public record RangedShot(UUID shooter, Vec3 origin, Vec3 eye, boolean advantage, boolean inMelee, long firedAt) {}
