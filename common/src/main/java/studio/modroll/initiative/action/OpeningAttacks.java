package studio.modroll.initiative.action;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import studio.modroll.critfall.api.AttackDelivery;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * The hit that brings an outsider into a running fight. Attacking someone already in it spends the
 * attacker's turn for the round, so the hit is that turn's attack and is rolled like one — a swing
 * as the Attack action rolls it, an arrow as a turn's shot lands — instead of landing as plain
 * damage. It can miss.
 *
 * <p>The join and the roll meet inside the one hurt, and whether Critfall's damage listener or
 * Initiative's runs first is the loader's business. So either can join: when Critfall's ran first
 * its combat interaction has already joined the attacker and left the hit waiting
 * ({@link Encounter#markOpeningAttack}); when Initiative's runs first it joins the attacker itself,
 * through the same {@link EncounterManager#onCombat}, and the canceled hurt never reaches Critfall.
 * The roll happens once either way. A hit Initiative cannot roll — a blast, a spell, a snowball, or
 * an arrow with ranged attacks off — joins as before and lands as it is.
 */
final class OpeningAttacks {

    private OpeningAttacks() {}

    /** True when this hurt was an outsider's opening attack, now rolled; the loader cancels it. */
    static boolean intercept(ServerLevel level, LivingEntity attacker, LivingEntity target, DamageSource source) {
        AttackDelivery delivery = rollableDelivery(attacker, source);
        if (delivery == null) {
            return false;
        }
        Encounter encounter =
                EncounterManager.encounterContaining(level, target.getUUID()).orElse(null);
        if (encounter == null || !encounter.joinedBefore(target.getUUID(), level.getGameTime())) {
            return false;
        }
        if (EncounterManager.encounterContaining(level, attacker.getUUID()).isEmpty()) {
            EncounterManager.onCombat(attacker, target, delivery);
        }
        if (!encounter.contains(attacker.getUUID()) || !encounter.consumeOpeningAttack(attacker.getUUID())) {
            return false;
        }
        if (delivery == AttackDelivery.MELEE) {
            ActionEconomy.openingAttack(level, encounter, attacker, target);
        } else {
            RangedAttacks.landOpeningShot(level, encounter, attacker, target, source);
        }
        return true;
    }

    /** A swing of the attacker's own or an arrow it loosed; anything else is not Initiative's to roll. */
    private static AttackDelivery rollableDelivery(LivingEntity attacker, DamageSource source) {
        if (source.getDirectEntity() == attacker
                && !source.is(DamageTypeTags.IS_PROJECTILE)
                && (source.is(DamageTypes.PLAYER_ATTACK)
                        || source.is(DamageTypes.MOB_ATTACK)
                        || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
            return AttackDelivery.MELEE;
        }
        if (source.getDirectEntity() instanceof AbstractArrow && RangedAttacks.active()) {
            return AttackDelivery.PROJECTILE;
        }
        return null;
    }
}
