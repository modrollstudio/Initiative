package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.NativeActions;
import studio.modroll.initiative.checks.ChecksBridge;
import studio.modroll.initiative.checks.ChecksIntegration;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ChecksConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.gametest.RollAnimationScenarios.CapturingSender;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;
import studio.modroll.initiative.turn.InitiativeDerivation;
import studio.modroll.initiative.turn.InitiativeEntry;

/**
 * Initiative, Shove, Grapple, Escape and Hide with Checks installed. The GameTest datapack profiles
 * pin every modifier these scenarios read, so each scripted face lands on a known total:
 *
 * <ul>
 *   <li>player: Dexterity +2, Athletics +4, Acrobatics +2, Stealth +5
 *   <li>spider (nimble, dull): Athletics +0, Acrobatics +6, passive Perception 7
 *   <li>vindicator (strong, keen): Dexterity -1, Athletics +3, Acrobatics -1, passive Perception 17
 * </ul>
 *
 * The flat config bonuses are all 0, so the faces are picked where the flat roll and the Checks roll
 * disagree on the winner. A run started with {@code -PwithoutChecks} has no Checks to roll: the
 * scenarios that need it pass there without testing anything, and the flat ones run for real.
 */
public final class ChecksScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 40.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, false, true, true, true, true, true, true, false, 3.0, 0, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    private static final ChecksConfig CHECKS_ON = new ChecksConfig(true);
    private static final boolean WITHOUT_CHECKS = Boolean.getBoolean("initiative.gametest.withoutChecks");

    private static final int PLAYER_DEXTERITY = 2;
    private static final int PLAYER_ATHLETICS = 4;
    private static final int PLAYER_STEALTH = 5;
    private static final int SPIDER_ACROBATICS = 6;
    private static final int SPIDER_PASSIVE_PERCEPTION = 7;
    private static final int VINDICATOR_DEXTERITY = -1;
    private static final int VINDICATOR_ATHLETICS = 3;
    private static final int VINDICATOR_PASSIVE_PERCEPTION = 17;

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final int TICKS_INTO_ENEMY_TURN = 3;

    private ChecksScenarios() {}

    /** The defender, and an escaper, roll whichever of Athletics and Acrobatics is higher. */
    public static void contestsRollTheRightSkills(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 3, 1);
        Mob vindicator = spawnStill(helper, EntityType.VINDICATOR, 1, 3);
        requireTotals(
                helper,
                rolled(helper, 12, 10, () -> ChecksBridge.athleticsContest(player, spider)),
                12 + PLAYER_ATHLETICS,
                10 + SPIDER_ACROBATICS,
                "a shove into a nimble defender: Athletics against its Acrobatics");
        requireTotals(
                helper,
                rolled(helper, 12, 10, () -> ChecksBridge.athleticsContest(player, vindicator)),
                12 + PLAYER_ATHLETICS,
                10 + VINDICATOR_ATHLETICS,
                "a shove into a strong defender: Athletics against its Athletics");
        requireTotals(
                helper,
                rolled(helper, 10, 12, () -> ChecksBridge.escapeContest(spider, player)),
                10 + SPIDER_ACROBATICS,
                12 + PLAYER_ATHLETICS,
                "a nimble escaper: its Acrobatics against the grappler's Athletics");
        requireTotals(
                helper,
                rolled(helper, 10, 10, () -> ChecksBridge.escapeContest(player, vindicator)),
                10 + PLAYER_ATHLETICS,
                10 + VINDICATOR_ATHLETICS,
                "a strong escaper: its Athletics against the grappler's Athletics");
        cleanUp(player, spider, vindicator);
        helper.succeed();
    }

    /** 12 + 4 ties 10 + 6, and a tie goes to the defender; the flat roll would have won. */
    public static void shoveRollsSkillsAndShowsTheContest(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        CapturingSender sender = captureAnimations();
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, spider);
        withFaces(
                () -> {
                    NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, spider);
                    if (attempt.status() != ActionEconomy.ActionOutcome.PERFORMED || attempt.knockedBack()) {
                        helper.fail("Acrobatics +6 must resist an Athletics +4 shove on a tie but was " + attempt);
                    }
                },
                12,
                10);
        if (sender.lastFor(helper, player).kind() != RollAnimation.Kind.CONTEST) {
            helper.fail("a Checks shove must still play the contest animation");
        }
        cleanUp(player, spider);
        helper.succeed();
    }

    /** 10 + 4 beats 10 + 3; the flat roll would have tied and lost. */
    public static void grappleRollsSkills(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob vindicator = spawnStill(helper, EntityType.VINDICATOR, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, vindicator);
        withFaces(
                () -> {
                    NativeActions.GrappleAttempt attempt = NativeActions.grapple(helper.getLevel(), player, vindicator);
                    if (!attempt.grappled()) {
                        helper.fail("Athletics +4 must out-grapple Athletics +3 on equal faces but was " + attempt);
                    }
                },
                10,
                10);
        cleanUp(player, vindicator);
        helper.succeed();
    }

    /** 8 + 4 beats the grappler's 11 + 0: it rolls Athletics, not its far better Acrobatics. */
    public static void escapeRollsSkills(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, spider);
        encounter(helper, player).flags().setGrappled(player.getUUID(), spider.getUUID());
        withFaces(
                () -> {
                    NativeActions.EscapeAttempt attempt = NativeActions.escape(helper.getLevel(), player);
                    if (!attempt.escaped()) {
                        helper.fail("Athletics +4 must break a hold held with Athletics +0 but was " + attempt);
                    }
                },
                8,
                11);
        cleanUp(player, spider);
        helper.succeed();
    }

    public static void hideStoresTheStealthTotal(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        CapturingSender sender = captureAnimations();
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 6, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, spider);
        Encounter encounter = encounter(helper, player);
        hide(helper, player, 12);
        if (!encounter.flags().isHidden(player.getUUID())) {
            helper.fail("a Checks Hide must hide the hider");
        }
        Integer stored = encounter.flags().hiddenStealth().get(player.getUUID());
        if (stored == null || stored != 12 + PLAYER_STEALTH) {
            helper.fail("Hide must keep the Stealth total " + (12 + PLAYER_STEALTH) + " but kept " + stored);
        }
        RollAnimation shown = sender.lastFor(helper, player);
        if (shown.kind() != RollAnimation.Kind.CHECK || shown.actor().detail().kept() != 12) {
            helper.fail("the Stealth roll must play as a check animation but was " + shown);
        }
        cleanUp(player, spider);
        helper.succeed();
    }

    /** Advantage from a check event listener: both d20s show, the higher kept, and the mode is the one rolled. */
    public static void hideShowsTheModeRolled(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        CapturingSender sender = captureAnimations();
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 6, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, spider);
        Encounter encounter = encounter(helper, player);
        CheckEventScript.withAdvantage(() -> hide(helper, player, 5, 12));
        Integer stored = encounter.flags().hiddenStealth().get(player.getUUID());
        if (stored == null || stored != 12 + PLAYER_STEALTH) {
            helper.fail(
                    "Hide must keep the higher face's Stealth total " + (12 + PLAYER_STEALTH) + " but kept " + stored);
        }
        RollDetail shown = sender.lastFor(helper, player).actor().detail();
        if (shown.mode() != RollMode.ADVANTAGE
                || shown.kept() != 12
                || !shown.dropped().equals(OptionalInt.of(5))) {
            helper.fail("the Stealth roll must show advantage keeping 12 over 5 but was " + shown);
        }
        cleanUp(player, spider);
        helper.succeed();
    }

    /** A canceled Stealth check rolled nothing: the Hide fails, and no dice are shown. */
    public static void aCanceledHideFailsWithoutDice(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 6, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, spider);
        // Only once the encounter has formed: its initiative rolls play their own animations.
        CapturingSender sender = captureAnimations();
        Encounter encounter = encounter(helper, player);
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(12);
        try {
            CheckEventScript.canceling(() -> {
                ActionEconomy.HideAttempt attempt = ActionEconomy.hide(helper.getLevel(), player);
                if (attempt.status() != ActionEconomy.ActionOutcome.PERFORMED || attempt.hidden()) {
                    helper.fail("a canceled Stealth check must spend the Hide and fail it but was " + attempt);
                }
            });
        } finally {
            RollService.resetRoller();
        }
        if (encounter.flags().isHidden(player.getUUID())
                || encounter.flags().hiddenStealth().containsKey(player.getUUID())) {
            helper.fail("a canceled Stealth check must neither hide nor keep a total");
        }
        requireNoDice(helper, sender, player, dice);
        cleanUp(player, spider);
        helper.succeed();
    }

    /** A canceled contest rolled nothing: the Shove is lost, never a 0 against 0, and no dice are shown. */
    public static void aCanceledShoveFailsWithoutDice(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, spider);
        // Only once the encounter has formed: its initiative rolls play their own animations.
        CapturingSender sender = captureAnimations();
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(20, 1);
        try {
            CheckEventScript.canceling(() -> {
                NativeActions.ShoveAttempt attempt = NativeActions.shove(helper.getLevel(), player, spider);
                if (attempt.status() != ActionEconomy.ActionOutcome.PERFORMED || attempt.knockedBack()) {
                    helper.fail("a canceled contest must spend the Shove and lose it but was " + attempt);
                }
            });
        } finally {
            RollService.resetRoller();
        }
        requireNoDice(helper, sender, player, dice);
        cleanUp(player, spider);
        helper.succeed();
    }

    /** Each side adds its Dexterity modifier to the d20, a negative one included. */
    public static void initiativeAddsDexterity(GameTestHelper helper) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob vindicator = spawnStill(helper, EntityType.VINDICATOR, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, vindicator);
        Encounter encounter = encounter(helper, player);
        requireInitiative(helper, encounter, player, 20, PLAYER_DEXTERITY);
        requireInitiative(helper, encounter, vindicator, 1, VINDICATOR_DEXTERITY);
        cleanUp(player, vindicator);
        helper.succeed();
    }

    /** Stealth 12 + 5 = 17, met exactly by the vindicator's passive Perception 17. */
    public static void aKeenEnemyRevealsTheHiderOnItsTurn(GameTestHelper helper) {
        hideThenPassTheTurn(helper, EntityType.VINDICATOR, VINDICATOR_PASSIVE_PERCEPTION, false);
    }

    /** The same Stealth 17 against the spider's passive Perception 7. */
    public static void aDullEnemyLeavesTheHiderHidden(GameTestHelper helper) {
        hideThenPassTheTurn(helper, EntityType.SPIDER, SPIDER_PASSIVE_PERCEPTION, true);
    }

    /** Checks installed but switched off: the flat bonuses come back. */
    public static void toggledOffUsesFlatBonuses(GameTestHelper helper) {
        prepare(helper, new ChecksConfig(false));
        requireFlatRolls(helper);
    }

    /**
     * Checks treated as not installed: the same flat rolls, with the config left on. In a run without
     * Checks this is the real thing.
     */
    public static void withoutChecksUsesFlatBonuses(GameTestHelper helper) {
        prepare(helper, CHECKS_ON);
        boolean installed = ChecksIntegration.present();
        ChecksIntegration.setPresent(false);
        try {
            requireFlatRolls(helper);
        } finally {
            ChecksIntegration.setPresent(installed);
        }
    }

    private static void hideThenPassTheTurn(
            GameTestHelper helper, EntityType<? extends Mob> watcherType, int passivePerception, boolean staysHidden) {
        if (!prepareWithChecks(helper)) {
            return;
        }
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob watcher = spawnStill(helper, watcherType, 6, 1);
        if (ChecksBridge.passivePerception(watcher) != passivePerception) {
            helper.fail("the GameTest profile must give the watcher passive Perception " + passivePerception);
        }
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, watcher);
        Encounter encounter = encounter(helper, player);
        hide(helper, player, 12);
        encounter.turnOrder().endTurn();
        helper.startSequence()
                .thenExecuteAfter(TICKS_INTO_ENEMY_TURN, () -> {
                    if (!watcher.getUUID()
                            .equals(encounter.turnOrder().currentTurn().orElse(null))) {
                        helper.fail("the watcher's turn must be under way");
                    }
                    if (encounter.flags().isHidden(player.getUUID()) != staysHidden) {
                        helper.fail("passive Perception " + passivePerception + " against Stealth "
                                + (12 + PLAYER_STEALTH) + " must " + (staysHidden ? "not " : "")
                                + "reveal the hider");
                    }
                    cleanUp(player, watcher);
                })
                .thenSucceed();
    }

    /**
     * Initiative adds the movement-speed bonus, not Dexterity, and 12 against 10 with flat bonuses of
     * 0 wins both the shove and the Hide contest.
     */
    private static void requireFlatRolls(GameTestHelper helper) {
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Mob spider = spawnStill(helper, EntityType.SPIDER, 3, 1);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, spider);
        Encounter encounter = encounter(helper, player);
        requireInitiative(helper, encounter, player, 20, speedBonus(player));
        requireInitiative(helper, encounter, spider, 1, speedBonus(spider));
        withFaces(
                () -> {
                    if (!NativeActions.shove(helper.getLevel(), player, spider).knockedBack()) {
                        helper.fail("a flat 12 against 10 must win the shove");
                    }
                },
                12,
                10);
        startThePlayersNextTurn(encounter);
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(12, 10);
        try {
            ActionEconomy.hide(helper.getLevel(), player);
        } finally {
            RollService.resetRoller();
        }
        if (dice.d20Draws != 2 || !encounter.flags().isHidden(player.getUUID())) {
            helper.fail("a flat Hide must be a won two-dice contest but drew " + dice.d20Draws);
        }
        if (!encounter.flags().hiddenStealth().isEmpty()) {
            helper.fail("a flat Hide must keep no Stealth total");
        }
        cleanUp(player, spider);
        helper.succeed();
    }

    /** The shove spent the action; the Hide needs the player's next turn. */
    private static void startThePlayersNextTurn(Encounter encounter) {
        encounter.turnOrder().endTurn();
        encounter.turnOrder().endTurn();
    }

    private static void hide(GameTestHelper helper, Player player, int... faces) {
        withFaces(
                () -> {
                    ActionEconomy.HideAttempt attempt = ActionEconomy.hide(helper.getLevel(), player);
                    if (attempt.status() != ActionEconomy.ActionOutcome.PERFORMED || !attempt.hidden()) {
                        helper.fail("a Checks Hide must always hide but was " + attempt);
                    }
                },
                faces);
    }

    private static ContestResult rolled(
            GameTestHelper helper, int initiatorFace, int opponentFace, Supplier<Optional<ContestResult>> contest) {
        ScenarioSupport.forceD20Faces(initiatorFace, opponentFace);
        try {
            Optional<ContestResult> result = contest.get();
            if (result.isEmpty()) {
                helper.fail("nothing canceled this contest, so it must have rolled");
            }
            return result.orElseThrow();
        } finally {
            RollService.resetRoller();
        }
    }

    private static void requireInitiative(
            GameTestHelper helper, Encounter encounter, LivingEntity entity, int natural, int bonus) {
        InitiativeEntry entry = encounter.turnOrder().order().stream()
                .filter(one -> one.participant().equals(entity.getUUID()))
                .findFirst()
                .orElse(null);
        if (entry == null || entry.bonus() != bonus || entry.total() != natural + bonus) {
            helper.fail(entity.getType().getDescriptionId() + " must roll initiative " + natural + " + " + bonus
                    + " but was " + entry);
        }
    }

    private static int speedBonus(LivingEntity entity) {
        return InitiativeDerivation.bonus(entity.getAttributeValue(Attributes.MOVEMENT_SPEED), TURNS);
    }

    private static void requireNoDice(
            GameTestHelper helper, CapturingSender sender, Player player, ScenarioSupport.ScriptedDice dice) {
        if (dice.d20Draws != 0) {
            helper.fail("a canceled roll must draw no d20 but drew " + dice.d20Draws);
        }
        if (sender.sent.stream().anyMatch(sent -> sent.player().equals(player.getUUID()))) {
            helper.fail("a canceled roll must show no dice");
        }
    }

    private static void withFaces(Runnable action, int... faces) {
        ScenarioSupport.forceD20Faces(faces);
        try {
            action.run();
        } finally {
            RollService.resetRoller();
        }
    }

    private static void requireTotals(
            GameTestHelper helper, ContestResult result, int initiatorTotal, int opponentTotal, String what) {
        if (result.initiatorTotal() != initiatorTotal || result.opponentTotal() != opponentTotal) {
            helper.fail(what + " must total " + initiatorTotal + " vs " + opponentTotal + " but was "
                    + result.initiatorTotal() + " vs " + result.opponentTotal());
        }
    }

    private static <T extends Mob> T spawnStill(GameTestHelper helper, EntityType<T> type, int x, int z) {
        T mob = helper.spawn(type, new BlockPos(x, 1, z));
        mob.setNoAi(true);
        return mob;
    }

    private static CapturingSender captureAnimations() {
        CapturingSender sender = new CapturingSender();
        RollAnimationSync.overrideSenderForTesting(sender);
        return sender;
    }

    private static Encounter encounter(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, ChecksConfig checks) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideChecksForTesting(checks);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
        if (WITHOUT_CHECKS && ChecksIntegration.present()) {
            helper.fail("a run started with -PwithoutChecks must not have Checks installed");
        }
    }

    /**
     * The flat-bonus scenarios pass with or without Checks; the rest need it installed. In a run
     * without Checks this passes the scenario untested and answers false, so the caller returns.
     */
    private static boolean prepareWithChecks(GameTestHelper helper) {
        prepare(helper, CHECKS_ON);
        if (WITHOUT_CHECKS) {
            helper.succeed();
            return false;
        }
        if (!ChecksIntegration.present()) {
            helper.fail("the GameTest runs must have Checks installed, or be started with -PwithoutChecks");
        }
        return true;
    }

    private static void cleanUp(LivingEntity... entities) {
        RollAnimationSync.clearSenderOverrideForTesting();
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
