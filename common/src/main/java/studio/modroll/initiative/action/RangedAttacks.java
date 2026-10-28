package studio.modroll.initiative.action;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import studio.modroll.critfall.api.AttackContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.combat.MissedShotHolder;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.CoverConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;

/**
 * Ranged attacks in an encounter: a bow or crossbow shot, or a thrown trident. Firing is the attack.
 * It happens on the shooter's own turn and spends its action, the way a swing does, and what is
 * fixed at that moment — where it stood, any advantage it spent, a hostile at its elbow — is kept
 * with the projectile as a {@link RangedShot}. The arrow then flies for real, and each participant
 * it reaches gets Critfall's attack roll against its AC in place of the vanilla hit, with Critfall's
 * damage on a hit. A miss is handed to Critfall's own fly-past, so the arrow goes on past the target
 * and may still reach someone behind it. Three hooks reach this class: a use hook ({@link
 * #refusesUse}), a projectile-spawn hook ({@link #admits}) and the damage interception ({@link
 * #land}); mobs shoot through the same spawn hook from their turn ({@link #shoot}).
 */
public final class RangedAttacks {

    /** The projectiles a driven mob shot spawned, collected while the mob's own attack is running. */
    private static List<AbstractArrow> loosed;

    private RangedAttacks() {}

    /** Part of the action economy, with its own toggle; off, a shot between participants is only canceled. */
    static boolean active() {
        return ActionEconomy.active() && InitiativeConfig.actions().rangedAttacks();
    }

    /** A bow or trident fires when the draw is released; a crossbow fires on use once it is loaded. */
    public static boolean firesOnUse(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof CrossbowItem) {
            return CrossbowItem.isCharged(stack);
        }
        return item instanceof ProjectileWeaponItem || item instanceof TridentItem;
    }

    /**
     * The use hooks' question for a participant player: why this use may not start, or null when it
     * may. Drawing and firing wait for your turn and need your action; loading a crossbow is never a
     * shot, so it may happen at any time.
     */
    public static AttackStatus refusesUse(ServerLevel level, Player player, ItemStack stack) {
        if (!active() || !firesOnUse(stack)) {
            return null;
        }
        Encounter encounter =
                EncounterManager.encounterContaining(level, player.getUUID()).orElse(null);
        return encounter == null ? null : refusal(encounter, player);
    }

    /** The client already started the draw or fired, so its inventory is resent as well as the reason. */
    public static void refuseUse(Player player, AttackStatus status) {
        ActionEconomy.sendRejection(player, status);
        player.inventoryMenu.sendAllDataToRemote();
    }

    /**
     * Lowers a bow or trident a held player is still drawing: the draw began on its turn, and
     * releasing it now would be a shot off turn, refused only after the arrow was already spent.
     */
    public static void lowerDrawnWeapon(Player player) {
        if (active() && player.isUsingItem() && firesOnUse(player.getUseItem())) {
            player.stopUsingItem();
        }
    }

    /**
     * The spawn hooks' question: false refuses the projectile. A participant's arrow or trident is a
     * ranged attack, so the first one of a volley must be fired on the shooter's turn with its action
     * left, and spends it; the rest of that volley, loosed in the same tick, rides on the same shot.
     */
    public static boolean admits(ServerLevel level, Entity entity) {
        if (!active()
                || !(entity instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof LivingEntity shooter)) {
            return true;
        }
        Encounter encounter =
                EncounterManager.encounterContaining(level, shooter.getUUID()).orElse(null);
        if (encounter == null) {
            return true;
        }
        long now = level.getGameTime();
        RangedShot shot = encounter.volley(shooter.getUUID(), now).orElse(null);
        if (shot == null) {
            AttackStatus refused = refusal(encounter, shooter);
            if (refused != null) {
                if (shooter instanceof Player player) {
                    ActionEconomy.sendRejection(player, refused);
                }
                return false;
            }
            shot = fire(level, encounter, shooter, now);
        }
        encounter.recordShot(arrow.getUUID(), shot);
        if (loosed != null) {
            loosed.add(arrow);
        }
        return true;
    }

    /**
     * The roll for a shot's projectile reaching a participant, called from the damage interception
     * that cancels the projectile's vanilla hurt. A projectile fired as no shot is not rolled: a mob
     * shoots in real time until it joins, so its last arrow is often still in the air when it does.
     * Its hurt stays canceled, and it flies on like a miss, since vanilla would bounce it back off the
     * canceled hurt toward its own shooter. Past long range there is no attack at all and the arrow
     * flies on too. Otherwise the roll goes through Critfall with this shot's roll mode and the
     * target's cover, traced from where the shooter fired. With ranged attacks off nothing here runs
     * and the arrow is only canceled, as before.
     */
    static void land(
            ServerLevel level, Encounter encounter, LivingEntity shooter, LivingEntity target, DamageSource source) {
        if (!active() || !(source.getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        RangedShot shot = encounter.shot(arrow.getUUID());
        if (shot == null || !shot.shooter().equals(shooter.getUUID())) {
            flyPast(arrow, target);
            return;
        }
        ActionConfig actions = InitiativeConfig.actions();
        double range = shot.origin().distanceTo(target.position());
        if (range > actions.rangedLongRangeBlocks()) {
            flyPast(arrow, target);
            if (shooter instanceof Player player) {
                player.displayClientMessage(
                        Component.translatable("initiative.action.out_of_range").withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        boolean disadvantage = shot.inMelee()
                || range > actions.rangedNormalRangeBlocks()
                || ActionEconomy.targetImposesDisadvantage(encounter, actions, target);
        RollMode mode = ActionEconomy.resolveMode(shot.advantage(), disadvantage);
        AttackContext context = attackContext(arrow, source).withMode(mode);
        CoverConfig cover = InitiativeConfig.cover();
        int acBonus = Cover.acBonus(Cover.against(level, shooter, shot.eye(), target, cover), cover);
        if (acBonus != 0) {
            context = context.withDefenderAcBonus(acBonus);
        }
        AttackResult result = RollService.performAttack(shooter, target, context);
        if (result.isHit()) {
            encounter.flags().breakHidden(target.getUUID());
            strike(level, arrow, target, source);
        } else {
            flyPast(arrow, target);
        }
        RollAnimationSync.play(
                shooter,
                target,
                RollAnimation.attack(result, shooter.getDisplayName().getString()));
    }

    /**
     * An outsider's joining arrow, rolled as the turn shot it counts as. It was loosed before its
     * shooter was in the fight, so it carries no shot: one is recorded for it now, from where the
     * shooter stands, spending no action — the turn it counts as is the one the order will skip.
     */
    static void landOpeningShot(
            ServerLevel level, Encounter encounter, LivingEntity shooter, LivingEntity target, DamageSource source) {
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        ActionConfig actions = InitiativeConfig.actions();
        encounter.recordShot(
                arrow.getUUID(),
                new RangedShot(
                        shooter.getUUID(),
                        shooter.position(),
                        shooter.getEyePosition(),
                        ActionEconomy.spendAttackerAdvantage(encounter, actions, shooter),
                        hostileWithinReach(level, encounter, shooter, actions.attackReachBlocks()),
                        level.getGameTime()));
        land(level, encounter, shooter, target, source);
    }

    /** A mob that shoots rather than swings on its turn: a ranged attacker with a bow, crossbow or trident in hand. */
    static boolean shootsOnItsTurn(Mob mob) {
        return active()
                && mob instanceof RangedAttackMob
                && (isRangedWeapon(mob.getMainHandItem()) || isRangedWeapon(mob.getOffhandItem()));
    }

    /**
     * A mob's shot on its turn, once the target is within long range and in sight; until then it
     * keeps walking. It fires through the mob's own attack, so a stray's arrow still slows and a
     * pillager's Multishot still spreads, but aimed true: the dice decide whether it hits, so the
     * difficulty's scatter is taken out of the aim. The shot spends the turn's action through
     * {@link #admits} like any other.
     */
    static void shoot(ServerLevel level, Encounter encounter, Mob mob, LivingEntity target) {
        if (mob.distanceTo(target) > InitiativeConfig.actions().rangedLongRangeBlocks()
                || !mob.hasLineOfSight(target)) {
            return;
        }
        mob.lookAt(target, 360.0F, 360.0F);
        InteractionHand hand = mob.getMainHandItem().getItem() instanceof CrossbowItem
                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;
        ItemStack crossbow = mob.getItemInHand(hand);
        if (mob instanceof CrossbowAttackMob crossbowMob && crossbow.getItem() instanceof CrossbowItem item) {
            if (!CrossbowItem.isCharged(crossbow)) {
                crossbow.releaseUsing(level, mob, 0); // a full-length draw loads it
            }
            item.performShooting(level, mob, hand, crossbow, CrossbowItem.MOB_ARROW_POWER, 0.0F, target);
            crossbowMob.onCrossbowAttackPerformed();
            return;
        }
        loosed = new ArrayList<>();
        try {
            ((RangedAttackMob) mob).performRangedAttack(target, 1.0F);
            loosed.forEach(projectile -> aimTrue(projectile, target));
        } finally {
            loosed = null;
        }
    }

    /** The mob's own aim — at a third of the target's height, lifted for the drop — without the scatter. */
    private static void aimTrue(AbstractArrow projectile, LivingEntity target) {
        double dx = target.getX() - projectile.getX();
        double dy = target.getY(1.0 / 3.0) - projectile.getY();
        double dz = target.getZ() - projectile.getZ();
        double lift = Math.sqrt(dx * dx + dz * dz) * 0.2;
        projectile.shoot(
                dx, dy + lift, dz, (float) projectile.getDeltaMovement().length(), 0.0F);
    }

    /** A landed projectile is spent; one that is gone can carry no shot. */
    static void forgetLanded(ServerLevel level, Encounter encounter) {
        encounter.forgetShots(projectile -> level.getEntity(projectile) == null);
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TridentItem;
    }

    /** Like the Attack action: a player's shot costs the action under {@code single_attack_per_turn}, a mob's always. */
    private static boolean costsAction(LivingEntity shooter, ActionConfig actions) {
        return !(shooter instanceof Player) || actions.singleAttackPerTurn();
    }

    private static AttackStatus refusal(Encounter encounter, LivingEntity shooter) {
        if (!shooter.getUUID().equals(encounter.turnOrder().currentTurn().orElse(null))) {
            return AttackStatus.NOT_YOUR_TURN;
        }
        if (costsAction(shooter, InitiativeConfig.actions())
                && !ActionEconomy.budgetOf(encounter, shooter).actionAvailable()) {
            return AttackStatus.NO_ACTION;
        }
        return null;
    }

    /** Firing spends the action and the shooter's advantage, so both are settled here, once per volley. */
    private static RangedShot fire(ServerLevel level, Encounter encounter, LivingEntity shooter, long now) {
        ActionConfig actions = InitiativeConfig.actions();
        if (costsAction(shooter, actions)) {
            ActionEconomy.budgetOf(encounter, shooter).useAction();
        }
        return new RangedShot(
                shooter.getUUID(),
                shooter.position(),
                shooter.getEyePosition(),
                ActionEconomy.spendAttackerAdvantage(encounter, actions, shooter),
                hostileWithinReach(level, encounter, shooter, actions.attackReachBlocks()),
                now);
    }

    /**
     * 5e's ranged attack in melee: a hostile that can see the shooter stands within reach of it.
     * Reach is the Attack action's own, measured horizontally like every reach in Initiative.
     */
    private static boolean hostileWithinReach(
            ServerLevel level, Encounter encounter, LivingEntity shooter, double reach) {
        Encounter.Side side = encounter.side(shooter.getUUID());
        for (UUID participant : encounter.participants()) {
            if (encounter.side(participant) == side
                    || !(level.getEntity(participant) instanceof LivingEntity hostile)
                    || !hostile.isAlive()) {
                continue;
            }
            double dx = hostile.getX() - shooter.getX();
            double dz = hostile.getZ() - shooter.getZ();
            if (dx * dx + dz * dz <= reach * reach && hostile.hasLineOfSight(shooter)) {
                return true;
            }
        }
        return false;
    }

    /** Critfall's own split: a trident is thrown and is its own weapon; an arrow is fired from a bow or crossbow. */
    private static AttackContext attackContext(AbstractArrow arrow, DamageSource source) {
        ItemStack weapon = arrow.getWeaponItem() == null ? ItemStack.EMPTY : arrow.getWeaponItem();
        return arrow instanceof ThrownTrident
                ? AttackContext.thrown(source, weapon)
                : AttackContext.projectile(source, weapon);
    }

    /**
     * Critfall 0.2.16's missed shot: the arrow goes on past the target instead of bouncing back off
     * the hurt that was canceled, and never strikes that target again.
     */
    private static void flyPast(AbstractArrow arrow, LivingEntity target) {
        if (arrow instanceof MissedShotHolder missed) {
            missed.critfall$addMissed(target);
        }
    }

    /**
     * The rest of a vanilla arrow hit, which the canceled hurt skipped: Punch, a tipped arrow's
     * effects, a burning arrow's fire, the weapon's enchantment effects and the hit sound. Then the arrow is spent in the
     * target — or, with Piercing, flies on past it — rather than bouncing off. A trident drops at
     * the target either way, as vanilla has it.
     */
    private static void strike(ServerLevel level, AbstractArrow arrow, LivingEntity target, DamageSource source) {
        if (arrow instanceof StruckArrow struck) {
            struck.initiative$struck(target, source);
        }
        EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, arrow.getWeaponItem());
        if (arrow instanceof ThrownTrident) {
            return;
        }
        arrow.playSound(
                arrow.shotFromCrossbow() ? SoundEvents.CROSSBOW_HIT : SoundEvents.ARROW_HIT,
                1.0F,
                1.2F / (arrow.getRandom().nextFloat() * 0.2F + 0.9F));
        flyPast(arrow, target);
        if (arrow.getPierceLevel() <= 0) {
            target.setArrowCount(target.getArrowCount() + 1);
            arrow.discard();
        }
    }
}
