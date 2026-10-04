package studio.modroll.initiative.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.ActionOutcome;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.action.NativeActions;
import studio.modroll.initiative.action.Reactions;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.RollAnimationConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimation.Emphasis;
import studio.modroll.initiative.roll.RollAnimation.Kind;
import studio.modroll.initiative.roll.RollAnimationSync;

/**
 * M6 roll animation. The animation is pure presentation, so every scenario asserts what the acting
 * player is sent — never pixels — and the disabled scenario proves the combat resolves identically
 * with the animation off. A capturing sender stands in for the network, as in the M2c HUD suite.
 */
public final class RollAnimationScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0, 0,
            true, 8.0, true, 1.0, 0, 0);
    /** Attacking spends no action, so one turn can drive the animation-on and animation-off attacks. */
    private static final ActionConfig ACTIONS_MULTI = new ActionConfig(
            true, 6.0, false, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    private static final RollAnimationConfig ANIMATION_ON = new RollAnimationConfig(true, 10, 10, 2, true, true);
    private static final RollAnimationConfig ANIMATION_OFF = new RollAnimationConfig(false, 10, 10, 2, true, true);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);

    private RollAnimationScenarios() {}

    static final class CapturingSender implements RollAnimationSync.Sender {
        record Sent(UUID player, RollAnimation animation) {}

        final List<Sent> sent = new ArrayList<>();

        @Override
        public void send(Player player, RollAnimation animation) {
            sent.add(new Sent(player.getUUID(), animation));
        }

        RollAnimation lastFor(GameTestHelper helper, Player player) {
            for (int i = sent.size() - 1; i >= 0; i--) {
                if (sent.get(i).player().equals(player.getUUID())) {
                    return sent.get(i).animation();
                }
            }
            helper.fail("an animation must have been sent to " + player.getUUID());
            return null;
        }

        RollAnimation firstFor(GameTestHelper helper, Player player) {
            for (Sent one : sent) {
                if (one.player().equals(player.getUUID())) {
                    return one.animation();
                }
            }
            helper.fail("an animation must have been sent to " + player.getUUID());
            return null;
        }
    }

    public static void advantageAnimatesBothNaturalsKeepingTheHigher(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, ANIMATION_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player).flags().grantHelpAdvantage(player.getUUID(), husk.getUUID());
        attackWithFaces(helper, player, husk, 17, 4);
        RollAnimation animation = sender.lastFor(helper, player);
        requireKind(helper, animation, Kind.ATTACK);
        requireDice(helper, animation.actor(), RollMode.ADVANTAGE, 17, OptionalInt.of(4));
        if (animation.opponent().isPresent()) {
            helper.fail("an attack animation has no opposing side");
        }
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    public static void disadvantageAnimatesBothNaturalsKeepingTheLower(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, ANIMATION_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player).flags().setDodging(husk.getUUID());
        attackWithFaces(helper, player, husk, 17, 4);
        requireDice(helper, sender.lastFor(helper, player).actor(), RollMode.DISADVANTAGE, 4, OptionalInt.of(17));
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    public static void aNormalAttackAnimatesOneDie(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, ANIMATION_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        attackWithFaces(helper, player, husk, 15);
        requireDice(helper, sender.lastFor(helper, player).actor(), RollMode.NORMAL, 15, OptionalInt.empty());
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    public static void aContestAnimatesBothSides(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, ANIMATION_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ScenarioSupport.forceD20Faces(15, 5);
        try {
            NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, husk);
            if (attempt.status() != ActionOutcome.PERFORMED) {
                helper.fail("the shove must resolve but was " + attempt.status());
            }
        } finally {
            RollService.resetRoller();
        }
        RollAnimation animation = sender.lastFor(helper, player);
        requireKind(helper, animation, Kind.CONTEST);
        requireDice(helper, animation.actor(), RollMode.NORMAL, 15, OptionalInt.empty());
        if (animation.opponent().isEmpty()) {
            helper.fail("a contest animation must carry the opposing side");
        }
        requireDice(helper, animation.opponent().orElseThrow(), RollMode.NORMAL, 5, OptionalInt.empty());
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    public static void natTwentyAndNatOneAreFlaggedForEmphasis(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS_MULTI, ANIMATION_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        // Critfall's crit and fumble consequences draw their own d20, so each attack scripts a spare face.
        attackWithFaces(helper, player, husk, 20, 20);
        requireEmphasis(helper, sender.lastFor(helper, player), Emphasis.CRIT);
        attackWithFaces(helper, player, husk, 1, 1);
        requireEmphasis(helper, sender.lastFor(helper, player), Emphasis.FUMBLE);
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    /** A mob rolls, but the player it rolls against is the one at the table — they see the dice. */
    public static void aMobsOpportunityAttackAnimatesForTheMover(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, ANIMATION_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Vec3 from = player.position();
        ScenarioSupport.forceD20Faces(15);
        try {
            Reactions.onMovedWithinTurn(helper.getLevel(), encounter, player, from, from.add(6, 0, 0));
        } finally {
            RollService.resetRoller();
        }
        RollAnimation animation = sender.lastFor(helper, player);
        requireKind(helper, animation, Kind.ATTACK);
        requireDice(helper, animation.actor(), RollMode.NORMAL, 15, OptionalInt.empty());
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    /** The initiative d20 is the first roll of the encounter, so the joining player watches it too. */
    public static void theInitiativeRollAnimates(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, ANIMATION_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        RollAnimation animation = sender.firstFor(helper, player);
        requireKind(helper, animation, Kind.INITIATIVE);
        requireDice(helper, animation.actor(), RollMode.NORMAL, 20, OptionalInt.empty());
        requireEmphasis(helper, animation, Emphasis.CRIT);
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    /** The animation is theatre: the same forced roll must resolve identically with it off. */
    public static void disabledAnimationSendsNothingAndLeavesCombatUnchanged(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS_MULTI, ANIMATION_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk animated = ScenarioSupport.spawnHusk(helper, 3, 3);
        Husk silent = ScenarioSupport.spawnHusk(helper, 2, 3);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1, 1), animated, silent);
        Encounter encounter = encounterOf(helper, player);
        int round = encounter.turnOrder().round();
        UUID actor = encounter.turnOrder().currentTurn().orElse(null);

        float animatedBefore = animated.getHealth();
        attackWithFaces(helper, player, animated, 15);
        float animatedDamage = animatedBefore - animated.getHealth();
        int sentWithAnimation = sender.sent.size();

        InitiativeConfig.overrideRollAnimationForTesting(ANIMATION_OFF);
        float silentBefore = silent.getHealth();
        attackWithFaces(helper, player, silent, 15);
        float silentDamage = silentBefore - silent.getHealth();

        if (sender.sent.size() != sentWithAnimation) {
            helper.fail("a disabled animation must send nothing but sent " + (sender.sent.size() - sentWithAnimation)
                    + " packets");
        }
        if (animatedDamage <= 0 || animatedDamage != silentDamage) {
            helper.fail("the same roll must deal the same damage with the animation off, but dealt " + animatedDamage
                    + " then " + silentDamage);
        }
        if (encounter.turnOrder().round() != round
                || !encounter.turnOrder().currentTurn().orElse(null).equals(actor)) {
            helper.fail("the animation must not touch the turn state");
        }
        cleanUp(sender, player, animated, silent);
        helper.succeed();
    }

    private static void attackWithFaces(GameTestHelper helper, Player player, Husk husk, int... faces) {
        ScenarioSupport.forceD20Faces(faces);
        try {
            AttackStatus status =
                    ActionEconomy.attack(helper.getLevel(), player, husk).status();
            if (status != AttackStatus.PERFORMED) {
                helper.fail("the attack must resolve but was " + status);
            }
        } finally {
            RollService.resetRoller();
        }
    }

    private static void requireKind(GameTestHelper helper, RollAnimation animation, Kind expected) {
        if (animation.kind() != expected) {
            helper.fail("the animation kind must be " + expected + " but was " + animation.kind());
        }
    }

    private static void requireDice(
            GameTestHelper helper, RollAnimation.Roll roll, RollMode mode, int kept, OptionalInt dropped) {
        if (roll.detail().mode() != mode) {
            helper.fail("the roll mode must be " + mode + " but was "
                    + roll.detail().mode());
        }
        if (roll.detail().kept() != kept || !roll.detail().dropped().equals(dropped)) {
            helper.fail("the dice must be kept " + kept + " / dropped " + dropped + " but were kept "
                    + roll.detail().kept() + " / dropped " + roll.detail().dropped());
        }
    }

    private static void requireEmphasis(GameTestHelper helper, RollAnimation animation, Emphasis expected) {
        if (animation.actor().emphasis() != expected) {
            helper.fail("the roll must be flagged " + expected + " but was "
                    + animation.actor().emphasis());
        }
    }

    private static CapturingSender prepare(GameTestHelper helper, ActionConfig actions, RollAnimationConfig animation) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideRollAnimationForTesting(animation);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
        CapturingSender sender = new CapturingSender();
        RollAnimationSync.overrideSenderForTesting(sender);
        return sender;
    }

    private static Encounter encounterOf(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void cleanUp(CapturingSender sender, LivingEntity... entities) {
        RollAnimationSync.clearSenderOverrideForTesting();
        InitiativeConfig.overrideRollAnimationForTesting(ANIMATION_ON);
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
