package studio.modroll.initiative.gametest;

import java.util.function.IntUnaryOperator;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.modroll.critfall.api.AttackContext;
import studio.modroll.critfall.api.EffectiveEntityProfile;
import studio.modroll.critfall.api.EffectiveItemProfile;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackOutcome;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.critfall.api.dice.DiceRoller;
import studio.modroll.critfall.api.dice.RollResult;

/**
 * The M0 integration smoke test: drives Critfall purely through its published Maven artifact and
 * public API — dice rolls, suppression, effective-profile queries, and a full driven attack.
 * Critfall 0.2.1's test-only RNG seam ({@code RollService.setRoller}) lets these scenarios force
 * exact dice from the consumer side.
 */
public final class CritfallApiScenarios {

    private CritfallApiScenarios() {}

    public static void setRollerForcesExactRolls(GameTestHelper helper) {
        RollService.setRoller(new DiceRoller(fixedDice(bound -> bound - 1)));
        try {
            RollResult maxed = RollService.roll("2d6+3");
            if (maxed.total() != 15) {
                helper.fail("forced-max 2d6+3 must total exactly 15 but rolled " + maxed.total());
            }
            RollService.setRoller(new DiceRoller(fixedDice(bound -> 0)));
            RollResult floored = RollService.roll("2d6+3");
            if (floored.total() != 5) {
                helper.fail("forced-min 2d6+3 must total exactly 5 but rolled " + floored.total());
            }
        } finally {
            RollService.resetRoller();
        }
        helper.succeed();
    }

    public static void suppressionFlagRoundTrips(GameTestHelper helper) {
        Husk husk = spawnCalm(helper, EntityType.HUSK, 1, 1);
        if (RollService.isSuppressed(husk)) {
            helper.fail("a fresh entity must not be suppressed");
        }
        RollService.suppress(husk);
        if (!RollService.isSuppressed(husk)) {
            helper.fail("suppress must set the suppression flag");
        }
        RollService.release(husk);
        if (RollService.isSuppressed(husk)) {
            helper.fail("release must clear the suppression flag");
        }
        helper.succeed();
    }

    public static void effectiveProfilesAreQueryable(GameTestHelper helper) {
        Husk husk = spawnCalm(helper, EntityType.HUSK, 1, 1);
        EffectiveEntityProfile entity = RollService.effectiveEntity(husk);
        if (entity.armorClass() <= 0) {
            helper.fail("effective AC must be positive but was " + entity.armorClass());
        }
        if (entity.critRange() < 2 || entity.critRange() > 20) {
            helper.fail("crit range must be 2..20 but was " + entity.critRange());
        }
        EffectiveItemProfile item = RollService.effectiveItem(new ItemStack(Items.IRON_SWORD));
        if (item == null) {
            helper.fail("effective item profile must resolve for an iron sword");
        }
        helper.succeed();
    }

    public static void attackRollResolvesWithoutTouchingTheWorld(GameTestHelper helper) {
        Husk attacker = spawnCalm(helper, EntityType.HUSK, 1, 1);
        Husk target = spawnCalm(helper, EntityType.HUSK, 3, 3);
        float before = target.getHealth();
        AttackContext ctx =
                AttackContext.melee(helper.getLevel().damageSources().mobAttack(attacker), attacker.getMainHandItem());
        AttackResult result = RollService.attackRoll(attacker, target, ctx);
        requireConsistent(helper, result);
        if (target.getHealth() != before) {
            helper.fail("attackRoll must resolve without applying damage");
        }
        helper.succeed();
    }

    public static void drivenAttackAppliesDamageThroughTheApi(GameTestHelper helper) {
        Husk attacker = spawnCalm(helper, EntityType.HUSK, 1, 1);
        Husk target = spawnCalm(helper, EntityType.HUSK, 3, 3);
        RollService.suppress(attacker);
        RollService.suppress(target);
        try {
            float before = target.getHealth();
            AttackContext ctx = AttackContext.melee(
                    helper.getLevel().damageSources().mobAttack(attacker), attacker.getMainHandItem());
            AttackResult result = RollService.performAttack(attacker, target, ctx);
            requireConsistent(helper, result);
            if (result.isHit()) {
                if (result.damage() <= 0) {
                    helper.fail("a hit must roll positive damage but rolled " + result.damage());
                }
                if (!(target.getHealth() < before)) {
                    helper.fail("a driven hit must apply its damage to the target");
                }
            } else if (target.getHealth() != before) {
                helper.fail("a driven miss must not damage the target");
            }
        } finally {
            RollService.release(attacker);
            RollService.release(target);
        }
        helper.succeed();
    }

    private static void requireConsistent(GameTestHelper helper, AttackResult result) {
        if (result.natural() < 1 || result.natural() > 20) {
            helper.fail("natural roll must be 1..20 but was " + result.natural());
        }
        boolean hitOutcome = result.outcome() == AttackOutcome.HIT || result.outcome() == AttackOutcome.CRIT;
        if (hitOutcome != result.isHit()) {
            helper.fail("outcome " + result.outcome() + " disagrees with isHit() " + result.isHit());
        }
    }

    private static RandomGenerator fixedDice(IntUnaryOperator dieFromBound) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new IllegalStateException("forced dice must only draw bounded ints");
            }

            @Override
            public int nextInt(int bound) {
                return dieFromBound.applyAsInt(bound);
            }
        };
    }

    private static <T extends Mob> T spawnCalm(GameTestHelper helper, EntityType<T> type, int x, int z) {
        T mob = helper.spawn(type, new BlockPos(x, 1, z));
        mob.setNoAi(true);
        return mob;
    }
}
