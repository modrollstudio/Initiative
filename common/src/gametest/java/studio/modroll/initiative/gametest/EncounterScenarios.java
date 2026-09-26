package studio.modroll.initiative.gametest;

import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.CombatSuppression;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.DiceRoller;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * M1 encounter-bubble scenarios. Each runs in its own GameTest batch: encounters scan a radius of
 * real world space, so concurrent tests in one batch could pull each other's mobs into a bubble.
 * Turns are disabled here — bubble mechanics must hold on their own (see TurnOrderScenarios).
 */
public final class EncounterScenarios {

    private static final EncounterConfig TEST_CONFIG =
            new EncounterConfig(true, true, true, false, true, true, 3.0, 5.0);
    private static final TurnConfig TURNS_DISABLED =
            new TurnConfig(false, 100, 100, 20.0, 12, false, false, false, Set.of());

    private EncounterScenarios() {}

    public static void attackFormsEncounterSuppressingBoth(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Player player = spawnPlayer(helper, 1, 1);
        Husk husk = spawnHusk(helper, 2, 2);
        husk.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("a player attacking a hostile must form an encounter");
        }
        if (!encounter.contains(husk.getUUID())) {
            helper.fail("the attacked hostile must be in the encounter");
        }
        if (!RollService.isSuppressed(player)) {
            helper.fail("the player must have the suppression flag inside the bubble");
        }
        if (!RollService.isSuppressed(husk)) {
            helper.fail("the hostile must have the suppression flag inside the bubble");
        }
        player.discard();
        husk.discard();
        helper.succeed();
    }

    public static void everyExitPathClearsSuppression(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Player player = spawnPlayer(helper, 1, 1);
        Husk dies = spawnHusk(helper, 1, 2);
        Husk flees = spawnHusk(helper, 2, 1);
        dies.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        if (!RollService.isSuppressed(flees)) {
            helper.fail("a hostile inside the trigger radius must be pulled into the encounter");
        }
        Vec3 outside = helper.absoluteVec(new Vec3(1, 1, 10));
        helper.startSequence()
                .thenExecute(() -> flees.teleportTo(outside.x, outside.y, outside.z))
                .thenExecuteAfter(2, () -> {
                    if (RollService.isSuppressed(flees)) {
                        helper.fail("fleeing beyond the leave radius must clear the suppression flag");
                    }
                    flees.discard();
                    dies.kill();
                })
                .thenExecuteAfter(2, () -> {
                    if (RollService.isSuppressed(dies)) {
                        helper.fail("death must clear the suppression flag");
                    }
                    player.discard();
                })
                .thenExecuteAfter(2, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                            .isPresent()) {
                        helper.fail("the encounter must end once no valid participants remain");
                    }
                    Set<UUID> leaked = CombatSuppression.suppressedUuids();
                    if (!leaked.isEmpty()) {
                        helper.fail("no suppressed UUID may remain after all encounters end, but found " + leaked);
                    }
                })
                .thenSucceed();
    }

    public static void entityOutsideRadiusIsUntouched(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Player player = spawnPlayer(helper, 1, 1);
        Husk target = spawnHusk(helper, 2, 2);
        Husk outsider = spawnHusk(helper, 2, 10);
        target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        if (RollService.isSuppressed(outsider)) {
            helper.fail("an entity outside the trigger radius must keep Critfall's real-time combat");
        }
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (RollService.isSuppressed(outsider)) {
                        helper.fail("an entity that stays outside the radius must never join");
                    }
                    if (EncounterManager.encounterContaining(helper.getLevel(), outsider.getUUID())
                            .isPresent()) {
                        helper.fail("an entity outside the radius must not be an encounter member");
                    }
                    player.discard();
                    target.discard();
                    outsider.discard();
                })
                .thenSucceed();
    }

    /**
     * The trigger listens to Critfall's CombatInteractionEvent, which fires before all of
     * Critfall's own gating — so a swing Critfall would resolve as a miss or fumble (forced here
     * via minimum dice) must still start an encounter.
     */
    public static void whiffedAttackStillFormsEncounter(GameTestHelper helper) {
        prepare(helper, TEST_CONFIG);
        Player player = spawnPlayer(helper, 1, 1);
        Husk husk = spawnHusk(helper, 2, 2);
        RollService.setRoller(new DiceRoller(minimumDice()));
        try {
            husk.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        } finally {
            RollService.resetRoller();
        }
        if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .isEmpty()) {
            helper.fail("a hit Critfall would cancel must still form an encounter");
        }
        if (!RollService.isSuppressed(player) || !RollService.isSuppressed(husk)) {
            helper.fail("both participants must be suppressed even when the triggering hit whiffs");
        }
        player.discard();
        husk.discard();
        helper.succeed();
    }

    public static void disabledFlagFormsNoEncounter(GameTestHelper helper) {
        prepare(helper, new EncounterConfig(false, true, true, false, true, true, 3.0, 5.0));
        Player player = spawnPlayer(helper, 1, 1);
        Husk husk = spawnHusk(helper, 2, 2);
        husk.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        if (EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .isPresent()) {
            helper.fail("no encounter may form while the feature flag is off");
        }
        if (RollService.isSuppressed(player) || RollService.isSuppressed(husk)) {
            helper.fail("nothing may be suppressed while the feature flag is off");
        }
        player.discard();
        husk.discard();
        helper.succeed();
    }

    private static void prepare(GameTestHelper helper, EncounterConfig config) {
        InitiativeConfig.overrideEncountersForTesting(config);
        InitiativeConfig.overrideTurnsForTesting(TURNS_DISABLED);
        InitiativeConfig.overrideActionsForTesting(ScenarioSupport.ACTIONS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    /** Every bounded draw returns 0, so every die shows its lowest face (a d20 rolls 1). */
    private static RandomGenerator minimumDice() {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new IllegalStateException("forced dice must only draw bounded ints");
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private static Player spawnPlayer(GameTestHelper helper, double x, double z) {
        return ScenarioSupport.spawnPlayer(helper, x, z);
    }

    private static Husk spawnHusk(GameTestHelper helper, int x, int z) {
        return ScenarioSupport.spawnHusk(helper, x, z);
    }
}
