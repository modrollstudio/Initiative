package studio.modroll.initiative.gametest;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.DiceRoller;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.CoverConfig;
import studio.modroll.initiative.config.GrappleConfig;

/** Spawn/cleanup/roll-scripting helpers shared by the encounter, turn-order and freeze scenarios. */
final class ScenarioSupport {

    /**
     * The M3 action economy stays off in the pre-M3 suites: their assertions describe encounter,
     * turn-order, freeze and HUD semantics in isolation, and config overrides persist across
     * batches, so every suite must pin the economy explicitly to stay order-independent.
     */
    static final ActionConfig ACTIONS_OFF = new ActionConfig(
            false, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 10, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    /**
     * Pre-M5b suites drive attacks in open plots and must see exactly the no-cover, free-placement
     * world; every suite that reaches the attack path pins this so cover stays out of their
     * assertions regardless of batch order.
     */
    static final CoverConfig COVER_OFF = new CoverConfig(false, 2, 5, 0.5, 0.75, true, false, 1.0);

    /**
     * The M8-fix grapple defaults. Every suite that reaches the registry pins them: the bar and the
     * gate query {@code InitiativeConfig.grapple()} for the grapple and escape entries, and config
     * overrides persist across batches.
     */
    static final GrappleConfig GRAPPLE_DEFAULT = new GrappleConfig(true, 3.0, 0, 0, 5.0, true, 0, 0);

    private ScenarioSupport() {}

    /** Earlier batches leave mobs in the world; a radius scan here must only ever see our own. */
    static void discardLeftoverHostilesNearby(GameTestHelper helper) {
        discardLeftoverNearby(helper, mob -> mob instanceof Enemy);
    }

    /** For the suites where a passive or neutral leftover would be swept up too. */
    static void discardLeftoverMobsNearby(GameTestHelper helper) {
        discardLeftoverNearby(helper, mob -> true);
    }

    private static void discardLeftoverNearby(GameTestHelper helper, Predicate<Mob> which) {
        Vec3 center = helper.absoluteVec(new Vec3(2, 1, 2));
        AABB sweep = AABB.ofSize(center, 40, 40, 40);
        for (Mob mob : helper.getLevel().getEntitiesOfClass(Mob.class, sweep, which)) {
            mob.discard();
        }
    }

    /**
     * Feet in the plot floor's own layer: the player is placed inside the floor block rather than
     * on top of it, and the floor then holds it exactly where it was put. Physics still ticks — a
     * player moved off the plot falls — but nothing nudges one standing in the plot, which is all
     * the scenarios need; no assertion may depend on mobs seeing or pathing to the player.
     */
    static Player spawnPlayer(GameTestHelper helper, double x, double z) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(x, 1, z));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static Husk spawnHusk(GameTestHelper helper, int x, int z) {
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(x, 1, z));
        husk.setNoAi(true);
        return husk;
    }

    /** For the freeze scenarios: the whole point is watching real AI stop and resume. */
    static Husk spawnHuskWithAi(GameTestHelper helper, int x, int z) {
        return helper.spawn(EntityType.HUSK, new BlockPos(x, 1, z));
    }

    /**
     * The player attacks each mob in sequence. The attacker joins before the victim, so the join
     * order — and with it the draw order of {@code naturals} — is player, mob 1, mob 2, ...
     */
    static void formEncounter(GameTestHelper helper, Player player, List<Integer> naturals, Mob... mobs) {
        forceNaturals(helper, naturals, () -> {
            for (Mob mob : mobs) {
                mob.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
            }
        });
    }

    /**
     * Two attackers focus one target: each forming hit joins its own attacker, so i-frames are
     * cleared between them to guarantee both {@code onCombat} fire. The naturals are drawn in join
     * order — first attacker, target, second attacker.
     */
    static void formFocusFire(GameTestHelper helper, List<Integer> naturals, Player first, Player second, Mob target) {
        forceNaturals(helper, naturals, () -> {
            target.hurt(helper.getLevel().damageSources().playerAttack(first), 1.0f);
            target.invulnerableTime = 0;
            target.hurt(helper.getLevel().damageSources().playerAttack(second), 1.0f);
        });
    }

    /** Forces every die to its max (or min) face and counts draws; the caller must reset the roller. */
    static ScriptedDice forceCountedDice(boolean max) {
        ScriptedDice dice = new ScriptedDice();
        RollService.setRoller(new DiceRoller(new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new IllegalStateException("forced dice must only draw bounded ints");
            }

            @Override
            public int nextInt(int bound) {
                if (bound == 20) {
                    dice.d20Draws++;
                }
                return max ? bound - 1 : 0;
            }
        }));
        return dice;
    }

    /**
     * Scripts successive d20 faces (1..20) and counts draws; every other die rolls its max face so
     * damage stays deterministic. Two draws for one attack prove advantage/disadvantage engaged.
     * The caller must reset the roller.
     */
    static ScriptedDice forceD20Faces(int... faces) {
        ScriptedDice dice = new ScriptedDice();
        Queue<Integer> queue = new ArrayDeque<>();
        for (int face : faces) {
            queue.add(face);
        }
        RollService.setRoller(new DiceRoller(new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new IllegalStateException("forced dice must only draw bounded ints");
            }

            @Override
            public int nextInt(int bound) {
                if (bound != 20) {
                    return bound - 1;
                }
                dice.d20Draws++;
                Integer face = queue.poll();
                if (face == null) {
                    throw new IllegalStateException("scripted d20 faces exhausted");
                }
                return face - 1;
            }
        }));
        return dice;
    }

    static final class ScriptedDice {
        int d20Draws;
    }

    static void requireAttack(GameTestHelper helper, AttackStatus actual, AttackStatus expected, String what) {
        if (actual != expected) {
            helper.fail(what + " must be " + expected + " but was " + actual);
        }
    }

    static void forceNaturals(GameTestHelper helper, List<Integer> naturals, Runnable action) {
        Queue<Integer> queue = new ArrayDeque<>(naturals);
        RollService.setRoller(new DiceRoller(new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new IllegalStateException("forced dice must only draw bounded ints");
            }

            @Override
            public int nextInt(int bound) {
                Integer natural = queue.poll();
                if (natural == null) {
                    throw new IllegalStateException("an unexpected roll drew from the scripted dice");
                }
                return natural - 1;
            }
        }));
        try {
            action.run();
        } finally {
            RollService.resetRoller();
        }
        if (!queue.isEmpty()) {
            helper.fail("scripted dice left over: expected " + naturals.size() + " initiative rolls");
        }
    }
}
