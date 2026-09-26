package studio.modroll.initiative.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.BuiltinActions;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.api.ActionTargeting.Side;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ActionUiConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.ui.ActionInvoke;
import studio.modroll.initiative.ui.ActionInvokePayload;
import studio.modroll.initiative.ui.ActionUiSnapshot;
import studio.modroll.initiative.ui.ActionUiSnapshots;
import studio.modroll.initiative.ui.ActionUiSync;

/**
 * M8: the data behind the buttons. Pixels are manual-verify; what is asserted here is that the
 * snapshot the client renders is exactly the registry seen through the gate, and that a click takes
 * the same path a command does.
 */
public final class ActionUiScenarios {

    private static final ResourceLocation PROBE = ResourceLocation.fromNamespaceAndPath("initiativetest", "ui_probe");

    private static final EncounterConfig TIGHT_BUBBLE = new EncounterConfig(true, true, true, false, true, 1.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 200, 200, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);
    private static final ActionConfig DASH_OFF = new ActionConfig(
            true, 6.0, true, 4.0, false, true, false, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> PLAYER_FIRST_TWO_HOSTILES = List.of(20, 5, 1);
    private static final List<Integer> ALLY_LAST = List.of(20, 5, 1);

    private static int probes;

    static {
        ActionRegistry.register(Action.builder(PROBE)
                .cost(ActionCost.BONUS_ACTION)
                .targeting(ActionTargeting.entity(Side.ENEMY))
                .effect(context -> {
                    probes++;
                    return ActionResult.performed();
                })
                .build());
    }

    private ActionUiScenarios() {}

    static final class CapturingSender implements ActionUiSync.Sender {
        record Sent(UUID player, ActionUiSnapshot snapshot) {}

        final List<Sent> sent = new ArrayList<>();

        @Override
        public void send(Player player, ActionUiSnapshot snapshot) {
            sent.add(new Sent(player.getUUID(), snapshot));
        }

        ActionUiSnapshot lastFor(GameTestHelper helper, Player player) {
            for (int i = sent.size() - 1; i >= 0; i--) {
                if (sent.get(i).player().equals(player.getUUID())) {
                    return sent.get(i).snapshot();
                }
            }
            helper.fail("an action UI snapshot must have been sent to " + player.getUUID());
            return null;
        }
    }

    /** The bar is the registry: everything registered, nothing else, whoever registered it. */
    public static void barMirrorsTheRegistryForTheActingPlayer(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    ActionUiSnapshot snapshot = sender.lastFor(helper, player);
                    if (!snapshot.yourTurn()) {
                        helper.fail("the acting player's snapshot must say it is their turn");
                    }
                    requireIds(helper, snapshot, expectedIds(player));
                    if (snapshot.entry(BuiltinActions.OPPORTUNITY_ATTACK).isPresent()) {
                        helper.fail("a reaction is triggered, never a button");
                    }
                    if (snapshot.entry(PROBE).isEmpty()) {
                        helper.fail("a mod-registered action must reach the bar with no UI code of its own");
                    }
                    cleanUp(sender, player, husk);
                })
                .thenSucceed();
    }

    /** Every button the spent resource pays for greys together, with the gate's own reason. */
    public static void spendingTheActionGreysTheActionsItPaysFor(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        requireAvailable(helper, snapshot(helper, player), BuiltinActions.DODGE, true);
        if (ActionEconomy.dodge(helper.getLevel(), player) != ActionEconomy.ActionOutcome.PERFORMED) {
            helper.fail("the setup dodge must be performed");
        }
        ActionUiSnapshot spent = snapshot(helper, player);
        requireUnavailable(helper, spent, BuiltinActions.DODGE, ActionStatus.NO_ACTION);
        requireUnavailable(helper, spent, BuiltinActions.SHOVE, ActionStatus.NO_ACTION);
        requireAvailable(helper, spent, BuiltinActions.END_TURN, true);
        requireAvailable(helper, spent, PROBE, true);
        if (spent.budget().action() || !spent.budget().bonusAction()) {
            helper.fail("spending the action must spend only the action");
        }
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    /** A disabled action does not exist in this world, so it is absent rather than greyed. */
    public static void disabledActionLeavesTheBarEntirely(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, DASH_OFF, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ActionUiSnapshot snapshot = snapshot(helper, player);
        if (snapshot.entry(BuiltinActions.DASH).isPresent()) {
            helper.fail("a config-disabled action must not be offered");
        }
        requireAvailable(helper, snapshot, BuiltinActions.DODGE, true);
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    /** Reach is the action's own declaration, so the offered targets differ per action. */
    public static void reachDecidesWhichTargetsAButtonOffers(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk near = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk far = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST_TWO_HOSTILES, near, far);
        ActionUiSnapshot snapshot = snapshot(helper, player);
        requireTargets(helper, snapshot, BuiltinActions.SHOVE, List.of(near.getId()));
        requireTargets(helper, snapshot, BuiltinActions.ATTACK, List.of(near.getId()));
        requireTargets(helper, snapshot, PROBE, List.of(near.getId(), far.getId()));
        cleanUp(sender, player, near, far);
        helper.succeed();
    }

    /** No one in reach is the same refusal the gate would give, said before the click. */
    public static void anActionWithNoTargetInReachIsGreyed(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk far = ScenarioSupport.spawnHusk(helper, 5, 5);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, far);
        ActionUiSnapshot snapshot = snapshot(helper, player);
        requireUnavailable(helper, snapshot, BuiltinActions.SHOVE, ActionStatus.INVALID_TARGET);
        requireTargets(helper, snapshot, BuiltinActions.SHOVE, List.of());
        requireUnavailable(helper, snapshot, BuiltinActions.ATTACK, ActionStatus.INVALID_TARGET);
        requireAvailable(helper, snapshot, PROBE, true);
        cleanUp(sender, player, far);
        helper.succeed();
    }

    /** The platform proof reaches the UI: a mod's action is clicked like any built-in. */
    public static void externalActionRunsThroughTheClickPath(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        int before = probes;
        require(helper, click(helper, player, PROBE, husk), ActionStatus.PERFORMED, "a clicked external action");
        if (probes != before + 1) {
            helper.fail("the clicked action must run its effect exactly once");
        }
        require(
                helper,
                click(helper, player, PROBE, husk),
                ActionStatus.NO_BONUS_ACTION,
                "a second click on a spent bonus action");
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    /** One gate, two front ends: a click and a command answer alike on the same turn state. */
    public static void clickAndCommandShareTheGate(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        require(helper, click(helper, player, BuiltinActions.DODGE, null), ActionStatus.PERFORMED, "a clicked dodge");
        ActionStatus command = ActionRegistry.invoke(
                        helper.getLevel(), player, BuiltinActions.DODGE, ActionRequest.none())
                .status();
        ActionStatus clicked = click(helper, player, BuiltinActions.DODGE, null).status();
        if (command != ActionStatus.NO_ACTION || clicked != command) {
            helper.fail(
                    "the click must answer exactly what the command does but was " + clicked + " against " + command);
        }
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    /** The button that fixes the immersion problem: the turn ends when the player says so. */
    public static void endTurnButtonEndsTheTurn(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        require(helper, click(helper, player, BuiltinActions.END_TURN, null), ActionStatus.PERFORMED, "End Turn");
        UUID current = encounter(helper, player).turnOrder().currentTurn().orElse(null);
        if (!husk.getUUID().equals(current)) {
            helper.fail("End Turn must pass the turn on");
        }
        cleanUp(sender, player, husk);
        helper.succeed();
    }

    /** A spent action no longer ends the turn: the player hides, sees the bar, and ends when ready. */
    public static void aSpentActionNoLongerEndsThePlayersTurn(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        ScenarioSupport.forceD20Faces(20, 1);
        try {
            require(helper, click(helper, player, BuiltinActions.HIDE, null), ActionStatus.PERFORMED, "a clicked hide");
        } finally {
            RollService.resetRoller();
        }
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    if (!player.getUUID()
                            .equals(encounter(helper, player)
                                    .turnOrder()
                                    .currentTurn()
                                    .orElse(null))) {
                        helper.fail("with end_turn_when_spent off the turn must wait for the player");
                    }
                    requireAvailable(helper, snapshot(helper, player), BuiltinActions.END_TURN, true);
                    cleanUp(sender, player, husk);
                })
                .thenSucceed();
    }

    /** The readout is live: what the player spends is what the bar shows. */
    public static void budgetAndClockTrackTheTurn(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        int[] seconds = new int[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    ActionUiSnapshot snapshot = sender.lastFor(helper, player);
                    seconds[0] = snapshot.turnSecondsRemaining();
                    if (snapshot.turnSecondsTotal() != 10) {
                        helper.fail(
                                "a 200 tick timeout must read as 10 seconds but was " + snapshot.turnSecondsTotal());
                    }
                    if (snapshot.budget().movementRemaining() != 6.0
                            || !snapshot.budget().action()) {
                        helper.fail("a fresh turn must show its whole budget");
                    }
                    player.moveTo(player.getX() + 2.0, player.getY(), player.getZ(), 0, 0);
                })
                .thenExecuteAfter(30, () -> {
                    ActionUiSnapshot snapshot = sender.lastFor(helper, player);
                    if (snapshot.budget().movementRemaining() > 4.1) {
                        helper.fail("walked movement must come off the readout but it showed "
                                + snapshot.budget().movementRemaining());
                    }
                    if (snapshot.turnSecondsRemaining() >= seconds[0]) {
                        helper.fail("the turn clock must count down");
                    }
                    cleanUp(sender, player, husk);
                })
                .thenSucceed();
    }

    /** Only the acting player gets a bar; everyone else's clears. That is the multiplayer shape. */
    public static void aWaitingPlayerGetsNoBar(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player ally = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 3, 3);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, player, ally, husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    if (!sender.lastFor(helper, player).yourTurn()) {
                        helper.fail("the acting player must have a bar");
                    }
                    if (sender.lastFor(helper, ally).yourTurn()) {
                        helper.fail("a waiting player must not be offered a turn's actions");
                    }
                    if (click(helper, ally, BuiltinActions.DODGE, null).status() != ActionStatus.NOT_YOUR_TURN) {
                        helper.fail("a waiting player's click must be refused server-side");
                    }
                    cleanUp(sender, player, ally, husk);
                })
                .thenSucceed();
    }

    /** Off is command-only, exactly as before M8: nothing sent, no click accepted. */
    public static void disabledUiSendsNothingAndRefusesClicks(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, ACTIONS, new ActionUiConfig(false));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    if (!sender.sent.isEmpty()) {
                        helper.fail("action_ui.enabled=false must suppress every snapshot");
                    }
                    require(
                            helper,
                            click(helper, player, BuiltinActions.DODGE, null),
                            ActionStatus.INACTIVE,
                            "a click with the UI off");
                    if (ActionEconomy.dodge(helper.getLevel(), player) != ActionEconomy.ActionOutcome.PERFORMED) {
                        helper.fail("the commands must keep working with the UI off");
                    }
                    cleanUp(sender, player, husk);
                })
                .thenSucceed();
    }

    private static ActionResult click(
            GameTestHelper helper, Player player, ResourceLocation action, LivingEntity target) {
        ActionInvokePayload payload = target == null
                ? ActionInvokePayload.untargeted(action)
                : ActionInvokePayload.on(action, target.getId());
        return ActionInvoke.perform(helper.getLevel(), player, payload);
    }

    private static ActionUiSnapshot snapshot(GameTestHelper helper, Player player) {
        return ActionUiSnapshots.build(helper.getLevel(), encounter(helper, player), player);
    }

    private static List<ResourceLocation> expectedIds(Player player) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Action action : ActionRegistry.all()) {
            if (action.cost() != ActionCost.REACTION && action.enabled() && action.availableTo(player)) {
                ids.add(action.id());
            }
        }
        return ids;
    }

    private static void requireIds(GameTestHelper helper, ActionUiSnapshot snapshot, List<ResourceLocation> expected) {
        List<ResourceLocation> actual =
                snapshot.entries().stream().map(ActionUiSnapshot.Entry::id).toList();
        if (!actual.equals(expected)) {
            helper.fail(
                    "the bar must be the registry in registration order: expected " + expected + " but was " + actual);
        }
    }

    private static void requireAvailable(
            GameTestHelper helper, ActionUiSnapshot snapshot, ResourceLocation id, boolean available) {
        ActionUiSnapshot.Entry entry = entry(helper, snapshot, id);
        if (entry.isAvailable() != available) {
            helper.fail(id + " must be " + (available ? "available" : "greyed") + " but was " + entry.unavailable());
        }
    }

    private static void requireUnavailable(
            GameTestHelper helper, ActionUiSnapshot snapshot, ResourceLocation id, ActionStatus expected) {
        ActionUiSnapshot.Entry entry = entry(helper, snapshot, id);
        if (!entry.unavailable().equals(Optional.of(expected))) {
            helper.fail(id + " must be greyed with " + expected + " but was " + entry.unavailable());
        }
    }

    private static void requireTargets(
            GameTestHelper helper, ActionUiSnapshot snapshot, ResourceLocation id, List<Integer> expected) {
        List<Integer> actual =
                entry(helper, snapshot, id).validTargetIds().stream().sorted().toList();
        if (!actual.equals(expected.stream().sorted().toList())) {
            helper.fail(id + " must offer targets " + expected + " but offered " + actual);
        }
    }

    private static ActionUiSnapshot.Entry entry(GameTestHelper helper, ActionUiSnapshot snapshot, ResourceLocation id) {
        ActionUiSnapshot.Entry entry = snapshot.entry(id).orElse(null);
        if (entry == null) {
            helper.fail(id + " must be on the bar");
        }
        return entry;
    }

    private static void require(GameTestHelper helper, ActionResult result, ActionStatus expected, String what) {
        if (result.status() != expected) {
            helper.fail(what + " must be " + expected + " but was " + result.status());
        }
    }

    private static Encounter encounter(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must exist while its action UI is inspected");
        }
        return encounter;
    }

    private static CapturingSender prepare(GameTestHelper helper, ActionConfig actions, ActionUiConfig ui) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideActionUiForTesting(ui);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
        CapturingSender sender = new CapturingSender();
        ActionUiSync.overrideSenderForTesting(sender);
        return sender;
    }

    private static void cleanUp(CapturingSender sender, LivingEntity... entities) {
        ActionUiSync.clearSenderOverrideForTesting();
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
