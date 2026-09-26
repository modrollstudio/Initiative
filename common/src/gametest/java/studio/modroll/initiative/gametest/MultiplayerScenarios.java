package studio.modroll.initiative.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.CombatSuppression;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.NativeActions;
import studio.modroll.initiative.action.Reactions;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ActionUiConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.HudConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.RollAnimationConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.hud.TurnOrderSnapshot;
import studio.modroll.initiative.hud.TurnOrderSync;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;
import studio.modroll.initiative.turn.AiFreeze;
import studio.modroll.initiative.turn.InitiativeEntry;
import studio.modroll.initiative.ui.ActionUiSnapshot;
import studio.modroll.initiative.ui.ActionUiSnapshots;

/**
 * M9: the encounter with more than one human in it. Everything asserted here is server state or the
 * packets a player is sent — the shared die, the countdown every client reads, the turn passing
 * between two players, and the exit paths that must not leave a condition behind when one of them
 * drops out.
 */
public final class MultiplayerScenarios {

    private static final EncounterConfig TIGHT_BUBBLE = new EncounterConfig(true, true, true, false, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 200, 200, 20.0, 12, false, false, false, Set.of());
    private static final TurnConfig SHORT_TURNS = new TurnConfig(true, 20, 20, 20.0, 12, true, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    private static final RollAnimationConfig SHARED = new RollAnimationConfig(true, 10, 10, 2, true, true);
    private static final RollAnimationConfig NOT_SHARED = new RollAnimationConfig(true, 10, 10, 2, true, false);

    /** Focus-fire join order is first attacker, target, second attacker — so the first player acts. */
    private static final List<Integer> ALLY_LAST = List.of(20, 5, 1);

    private MultiplayerScenarios() {}

    static final class RollSender implements RollAnimationSync.Sender {
        record Sent(UUID player, RollAnimation animation) {}

        final List<Sent> sent = new ArrayList<>();

        @Override
        public void send(Player player, RollAnimation animation) {
            sent.add(new Sent(player.getUUID(), animation));
        }

        List<Sent> since(int mark) {
            return List.copyOf(sent.subList(mark, sent.size()));
        }
    }

    static final class HudSender implements TurnOrderSync.Sender {
        record Sent(UUID player, TurnOrderSnapshot snapshot) {}

        final List<Sent> sent = new ArrayList<>();

        @Override
        public void send(Player player, TurnOrderSnapshot snapshot) {
            sent.add(new Sent(player.getUUID(), snapshot));
        }

        TurnOrderSnapshot lastFor(GameTestHelper helper, Player player) {
            for (int i = sent.size() - 1; i >= 0; i--) {
                if (sent.get(i).player().equals(player.getUUID())) {
                    return sent.get(i).snapshot();
                }
            }
            helper.fail("a turn-order snapshot must have been sent to " + player.getUUID());
            return null;
        }
    }

    /** The communal moment: one die, every player in the encounter, and nobody outside it. */
    public static void everyPlayerInTheEncounterWatchesTheSameRoll(GameTestHelper helper) {
        RollSender sender = prepareRolls(helper, SHARED);
        Player actor = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player ally = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Player bystander = ScenarioSupport.spawnPlayer(helper, 5, 5);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, actor, ally, husk);
        int mark = sender.sent.size();
        attackWithFaces(helper, actor, husk, 15);
        List<RollSender.Sent> broadcast = sender.since(mark);
        requireWatchers(helper, broadcast, List.of(actor, ally), bystander);
        if (broadcast.get(0).animation().actor().detail().kept() != 15) {
            helper.fail("every watcher must be sent the one die that was rolled");
        }
        cleanUpRolls(sender, actor, ally, bystander, husk);
        helper.succeed();
    }

    /** A mob's swing at one player has to read to the other, so the die says whose it is. */
    public static void theAnimationNamesItsRoller(GameTestHelper helper) {
        RollSender sender = prepareRolls(helper, SHARED);
        Player actor = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player ally = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, actor, ally, husk);
        Encounter encounter = encounterOf(helper, actor);
        int mark = sender.sent.size();
        Vec3 from = actor.position();
        ScenarioSupport.forceD20Faces(15);
        try {
            Reactions.onMovedWithinTurn(helper.getLevel(), encounter, actor, from, from.add(6, 0, 0));
        } finally {
            RollService.resetRoller();
        }
        String expected = husk.getDisplayName().getString();
        for (RollSender.Sent sent : sender.since(mark)) {
            if (!expected.equals(sent.animation().actor().roller())) {
                helper.fail("a mob's opportunity attack must be attributed to " + expected + " but read "
                        + sent.animation().actor().roller());
            }
        }
        if (sender.since(mark).size() != 2) {
            helper.fail("both players must watch the reaction, but it reached "
                    + sender.since(mark).size());
        }
        cleanUpRolls(sender, actor, ally, husk);
        helper.succeed();
    }

    /** Sharing off is exactly M6 again: the roll goes to the one player at the exchange. */
    public static void sharedVisibilityOffKeepsTheRollAtTheExchange(GameTestHelper helper) {
        RollSender sender = prepareRolls(helper, NOT_SHARED);
        Player actor = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player ally = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, actor, ally, husk);
        int mark = sender.sent.size();
        attackWithFaces(helper, actor, husk, 15);
        List<RollSender.Sent> broadcast = sender.since(mark);
        if (broadcast.size() != 1 || !broadcast.get(0).player().equals(actor.getUUID())) {
            helper.fail("with shared visibility off only the roller may watch, but it reached " + broadcast.size()
                    + " players");
        }
        cleanUpRolls(sender, actor, ally, husk);
        helper.succeed();
    }

    /** A second player attacking in joins the same encounter, in initiative order, with its own UI. */
    public static void aSecondPlayerJoinsTheOrderAndGetsItsOwnUi(GameTestHelper helper) {
        prepareServerState(helper, ACTIONS, TURNS);
        Player first = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player second = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, first, second, husk);
        Encounter encounter = encounterOf(helper, first);
        if (!encounter.contains(second.getUUID())) {
            helper.fail("a player attacking into an existing encounter must join it, not open a second one");
        }
        if (!EncounterManager.encounterContaining(helper.getLevel(), second.getUUID())
                .orElseThrow()
                .id()
                .equals(encounter.id())) {
            helper.fail("both players must be in the one encounter");
        }
        requireOrder(helper, encounter, List.of(first.getUUID(), husk.getUUID(), second.getUUID()));
        if (!ActionUiSnapshots.build(helper.getLevel(), encounter, first).yourTurn()) {
            helper.fail("the higher initiative must hold the turn");
        }
        if (ActionUiSnapshots.build(helper.getLevel(), encounter, second).yourTurn()) {
            helper.fail("a late joiner behind the current actor must wait its slot");
        }
        discard(first, second, husk);
        helper.succeed();
    }

    /** Each player's bar is its own: what one spends never shows on the other's readout. */
    public static void eachPlayersUiShowsOnlyItsOwnBudget(GameTestHelper helper) {
        prepareServerState(helper, ACTIONS, TURNS);
        Player first = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player second = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, first, second, husk);
        Encounter encounter = encounterOf(helper, first);
        if (ActionEconomy.dodge(helper.getLevel(), first) != ActionEconomy.ActionOutcome.PERFORMED) {
            helper.fail("the acting player's dodge must be performed");
        }
        ActionUiSnapshot spent = ActionUiSnapshots.build(helper.getLevel(), encounter, first);
        if (spent.budget().action()) {
            helper.fail("the acting player must have spent its own action");
        }
        if (!ActionUiSnapshots.build(helper.getLevel(), encounter, second).equals(ActionUiSnapshot.INACTIVE)) {
            helper.fail("a waiting player's UI must say nothing but that it is not their turn");
        }
        encounter.turnOrder().endTurn();
        encounter.turnOrder().endTurn();
        ActionUiSnapshot theirs = ActionUiSnapshots.build(helper.getLevel(), encounter, second);
        if (!theirs.yourTurn() || !theirs.budget().action() || theirs.budget().movementRemaining() != 6.0) {
            helper.fail("the next player's turn must start on a whole budget of its own");
        }
        discard(first, second, husk);
        helper.succeed();
    }

    /** PvP stays out of encounters: two players are one side, so only ally actions reach across. */
    public static void playersAreAlliesAndCannotFightEachOther(GameTestHelper helper) {
        prepareServerState(helper, ACTIONS, TURNS);
        Player first = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player second = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, first, second, husk);
        Encounter encounter = encounterOf(helper, first);
        if (encounter.side(first.getUUID()) != encounter.side(second.getUUID())) {
            helper.fail("every player in an encounter is on the player side");
        }
        ScenarioSupport.requireAttack(
                helper,
                ActionEconomy.attack(helper.getLevel(), first, second).status(),
                ActionEconomy.AttackStatus.INVALID_TARGET,
                "an attack on an ally");
        if (NativeActions.grapple(helper.getLevel(), first, second).status()
                != ActionEconomy.ActionOutcome.INVALID_TARGET) {
            helper.fail("a grapple only reaches an enemy, so it must refuse an ally");
        }
        if (ActionEconomy.help(helper.getLevel(), first, second) != ActionEconomy.ActionOutcome.PERFORMED) {
            helper.fail("Help is the action that does reach an ally");
        }
        if (!encounter.flags().hasHelpAdvantage(second.getUUID())) {
            helper.fail("the helped player must carry the advantage");
        }
        discard(first, second, husk);
        helper.succeed();
    }

    /** A player who drops out must take everything of theirs with them and pass the turn on. */
    public static void aDisconnectedPlayerLeavesNothingBehind(GameTestHelper helper) {
        prepareServerState(helper, ACTIONS, TURNS);
        Player leaves = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player stays = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 1);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, leaves, stays, husk);
        Encounter encounter = encounterOf(helper, leaves);
        ScenarioSupport.forceD20Faces(20, 1);
        try {
            if (!NativeActions.grapple(helper.getLevel(), leaves, husk).grappled()) {
                helper.fail("the setup grapple must take hold");
            }
        } finally {
            RollService.resetRoller();
        }
        UUID gone = leaves.getUUID();
        AiFreeze.freeze(gone);
        leaves.discard();
        EncounterManager.playerDisconnected(helper.getLevel(), gone);

        if (encounter.contains(gone)) {
            helper.fail("a disconnected player must be out of the encounter");
        }
        if (gone.equals(encounter.turnOrder().currentTurn().orElse(null))) {
            helper.fail("the turn must not stall on a player who is no longer there");
        }
        if (encounter.flags().isGrappled(husk.getUUID())) {
            helper.fail("the hold of a departed grappler must be released");
        }
        if (CombatSuppression.suppressedUuids().contains(gone)) {
            helper.fail("a disconnected player must not stay suppressed");
        }
        if (AiFreeze.isFrozen(gone)) {
            helper.fail("a disconnected player must not stay frozen");
        }
        if (!encounter.contains(stays.getUUID()) || !encounter.contains(husk.getUUID())) {
            helper.fail("the encounter must carry on for whoever is left");
        }
        discard(stays, husk);
        helper.succeed();
    }

    /** The stall-breaker with humans on the clock: the turn ends on time and passes on. */
    public static void theTurnTimerEndsAPlayersTurnAndAdvances(GameTestHelper helper) {
        prepareServerState(helper, ACTIONS, SHORT_TURNS);
        Player first = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player second = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, first, second, husk);
        Encounter encounter = encounterOf(helper, first);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (!first.getUUID()
                            .equals(encounter.turnOrder().currentTurn().orElse(null))) {
                        helper.fail("the first player must open the round");
                    }
                    if (!AiFreeze.isFrozen(husk.getUUID())) {
                        helper.fail("a mob must stay frozen while a player acts, however many players there are");
                    }
                })
                .thenExecuteAfter(25, () -> {
                    UUID current = encounter.turnOrder().currentTurn().orElse(null);
                    if (first.getUUID().equals(current)) {
                        helper.fail("a player's turn must end when the timer runs out");
                    }
                    if (current == null || !encounter.contains(current)) {
                        helper.fail("the timeout must hand the turn to a participant but gave it to " + current);
                    }
                    if (encounter.turnOrder().order().size() != 3) {
                        helper.fail("a timeout must not drop anyone from the order");
                    }
                    discard(first, second, husk);
                })
                .thenSucceed();
    }

    /** The waiting players read the clock off the strip, which is why it rides on the snapshot. */
    public static void theTurnStripCountsDownForEveryPlayer(GameTestHelper helper) {
        prepareServerState(helper, ACTIONS, TURNS);
        HudSender sender = new HudSender();
        TurnOrderSync.overrideSenderForTesting(sender);
        Player first = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player second = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, first, second, husk);
        int[] opening = new int[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    TurnOrderSnapshot watching = sender.lastFor(helper, second);
                    if (watching.currentActorId() != first.getId()) {
                        helper.fail("a waiting player must be told whose turn it is");
                    }
                    if (watching.secondsRemaining() <= 0) {
                        helper.fail("a waiting player must be told how long that turn has left");
                    }
                    opening[0] = watching.secondsRemaining();
                })
                .thenExecuteAfter(30, () -> {
                    if (sender.lastFor(helper, second).secondsRemaining() >= opening[0]) {
                        helper.fail("the strip's countdown must count down");
                    }
                    if (sender.lastFor(helper, first).secondsRemaining()
                            != sender.lastFor(helper, second).secondsRemaining()) {
                        helper.fail("every player must read the same clock");
                    }
                    TurnOrderSync.clearSenderOverrideForTesting();
                    discard(first, second, husk);
                })
                .thenSucceed();
    }

    /** One encounter per entity: a hostile already fighting is not dragged into a second bubble. */
    public static void aHostileAlreadyFightingJoinsNoSecondEncounter(GameTestHelper helper) {
        prepareServerState(helper, ACTIONS, TURNS);
        Player first = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player second = ScenarioSupport.spawnPlayer(helper, 8, 8);
        Husk engaged = ScenarioSupport.spawnHusk(helper, 2, 1);
        Husk other = ScenarioSupport.spawnHusk(helper, 9, 8);
        ScenarioSupport.formEncounter(helper, first, List.of(20, 1), engaged);
        ScenarioSupport.formEncounter(helper, second, List.of(20, 1), other);
        Encounter theirs = encounterOf(helper, second);
        engaged.invulnerableTime = 0;
        ScenarioSupport.formEncounter(helper, second, List.of(), engaged);
        if (theirs.contains(engaged.getUUID())) {
            helper.fail("a hostile already in an encounter must not be pulled into a second one");
        }
        if (!encounterOf(helper, first).contains(engaged.getUUID())) {
            helper.fail("it must stay in the encounter it was already fighting");
        }
        discard(first, second, engaged, other);
        helper.succeed();
    }

    /**
     * The other half of the one-encounter-per-entity rule: when a disconnect ends an encounter, the
     * mobs it held must be free to fight someone else. A membership that outlived its encounter
     * would leave them permanently unable to join anyone.
     */
    public static void aMobFreedByADisconnectJoinsAnotherPlayersEncounter(GameTestHelper helper) {
        prepareServerState(helper, ACTIONS, TURNS);
        Player leaves = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk freed = ScenarioSupport.spawnHusk(helper, 2, 1);
        Player elsewhere = ScenarioSupport.spawnPlayer(helper, 5, 5);
        Husk theirs = ScenarioSupport.spawnHusk(helper, 6, 5);
        ScenarioSupport.formEncounter(helper, leaves, List.of(20, 1), freed);
        ScenarioSupport.formEncounter(helper, elsewhere, List.of(20, 1), theirs);
        Encounter surviving = encounterOf(helper, elsewhere);
        UUID gone = leaves.getUUID();
        leaves.discard();
        EncounterManager.playerDisconnected(helper.getLevel(), gone);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), freed.getUUID())
                            .isPresent()) {
                        helper.fail("an encounter with no players left must release its mobs");
                    }
                    if (RollService.isSuppressed(freed)) {
                        helper.fail("a released mob must be back on Critfall's real-time pipeline");
                    }
                    freed.invulnerableTime = 0;
                    ScenarioSupport.forceNaturals(
                            helper,
                            List.of(10),
                            () -> elsewhere.hurt(
                                    helper.getLevel().damageSources().mobAttack(freed), 1.0f));
                })
                .thenExecute(() -> {
                    if (!surviving.contains(freed.getUUID())) {
                        helper.fail("the freed mob must be able to join the surviving encounter");
                    }
                    requireOrder(helper, surviving, List.of(elsewhere.getUUID(), freed.getUUID(), theirs.getUUID()));
                    if (!RollService.isSuppressed(freed)) {
                        helper.fail("joining an encounter must suppress the mob again");
                    }
                    discard(elsewhere, freed, theirs);
                })
                .thenSucceed();
    }

    private static void requireWatchers(
            GameTestHelper helper, List<RollSender.Sent> broadcast, List<Player> expected, Player excluded) {
        List<UUID> watchers = broadcast.stream().map(RollSender.Sent::player).toList();
        for (Player player : expected) {
            if (!watchers.contains(player.getUUID())) {
                helper.fail("every player in the encounter must watch the roll, but " + player.getUUID() + " did not");
            }
        }
        if (watchers.contains(excluded.getUUID())) {
            helper.fail("a player outside the encounter must be sent nothing");
        }
        if (watchers.size() != expected.size()) {
            helper.fail("the roll must reach the encounter's players exactly once each, but reached " + watchers);
        }
    }

    private static void requireOrder(GameTestHelper helper, Encounter encounter, List<UUID> expected) {
        List<UUID> actual = encounter.turnOrder().order().stream()
                .map(InitiativeEntry::participant)
                .toList();
        if (!actual.equals(expected)) {
            helper.fail("the turn order must read " + expected + " but read " + actual);
        }
    }

    private static void attackWithFaces(GameTestHelper helper, Player player, Husk husk, int... faces) {
        ScenarioSupport.forceD20Faces(faces);
        try {
            ActionEconomy.AttackStatus status =
                    ActionEconomy.attack(helper.getLevel(), player, husk).status();
            if (status != ActionEconomy.AttackStatus.PERFORMED) {
                helper.fail("the attack must resolve but was " + status);
            }
        } finally {
            RollService.resetRoller();
        }
    }

    private static Encounter encounterOf(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static RollSender prepareRolls(GameTestHelper helper, RollAnimationConfig animation) {
        prepareServerState(helper, ACTIONS, TURNS);
        InitiativeConfig.overrideRollAnimationForTesting(animation);
        RollSender sender = new RollSender();
        RollAnimationSync.overrideSenderForTesting(sender);
        return sender;
    }

    private static void prepareServerState(GameTestHelper helper, ActionConfig actions, TurnConfig turns) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(turns);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideActionUiForTesting(new ActionUiConfig(true));
        InitiativeConfig.overrideHudForTesting(new HudConfig(true));
        InitiativeConfig.overrideRollAnimationForTesting(SHARED);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static void cleanUpRolls(RollSender sender, LivingEntity... entities) {
        RollAnimationSync.clearSenderOverrideForTesting();
        InitiativeConfig.overrideRollAnimationForTesting(SHARED);
        discard(entities);
    }

    private static void discard(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
