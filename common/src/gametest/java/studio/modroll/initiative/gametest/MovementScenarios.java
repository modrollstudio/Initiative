package studio.modroll.initiative.gametest;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ConfigSource;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * The turn's movement budget, read with the built-in action defaults so the ten blocks under test
 * are the shipped number. The husk stands out of the player's way and never acts on it.
 */
public final class MovementScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, true, 1.0, 8.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, true, Set.of());

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> HUSK_FIRST = List.of(1, 20);

    private static final double DEFAULT_BUDGET = 10.0;
    private static final int EFFECT_TICKS = 2400;
    private static final double TOLERANCE = 1e-6;

    private MovementScenarios() {}

    public static void aTurnGivesTenBlocksByDefault(GameTestHelper helper) {
        requireBudgetWith(helper, null, DEFAULT_BUDGET, "a player at walking speed");
    }

    public static void swiftnessGivesMoreBlocks(GameTestHelper helper) {
        requireBudgetWith(
                helper,
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, 0),
                DEFAULT_BUDGET * 1.2,
                "Swiftness I, +20% speed");
    }

    public static void slownessGivesFewerBlocks(GameTestHelper helper) {
        requireBudgetWith(
                helper,
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS, 0),
                DEFAULT_BUDGET * 0.85,
                "Slowness I, -15% speed");
    }

    /** The off-turn root takes speed to zero and is still on the player as its own turn begins. */
    public static void aHeldPlayerStartsItsTurnWithItsFullBudget(GameTestHelper helper) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        helper.startSequence()
                .thenExecuteAfter(
                        2, () -> encounterOf(helper, player).turnOrder().endTurn())
                .thenExecuteAfter(1, () -> {
                    requireBudget(helper, player, DEFAULT_BUDGET, "a player released from the off-turn hold");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    private static void requireBudgetWith(
            GameTestHelper helper, MobEffectInstance effect, double expected, String who) {
        prepare(helper);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 4, 4);
        if (effect != null) {
            player.addEffect(effect);
        }
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    requireBudget(helper, player, expected, who);
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    private static void requireBudget(GameTestHelper helper, Player player, double expected, String who) {
        double budget =
                ActionEconomy.budgetOf(encounterOf(helper, player), player).movementRemaining();
        if (Math.abs(budget - expected) > TOLERANCE) {
            helper.fail(who + " must start the turn with " + expected + " blocks but had " + budget);
        }
    }

    private static Encounter encounterOf(GameTestHelper helper, LivingEntity entity) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), entity.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before its budget can be read");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ActionConfig.fromJson(ConfigSource.of(new JsonObject())));
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
