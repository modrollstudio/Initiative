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
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.HudConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.hud.TurnOrderSnapshot;
import studio.modroll.initiative.hud.TurnOrderSnapshots;
import studio.modroll.initiative.hud.TurnOrderSync;

/**
 * M2c HUD snapshot scenarios. Same deterministic setup as M2a/M2b (trigger radius 1, explicit
 * attacks, scripted d20s, AI off); a capturing sender stands in for the network so every
 * server-side trigger is asserted without a real client.
 */
public final class HudScenarios {

    private static final EncounterConfig TIGHT_BUBBLE = new EncounterConfig(true, true, true, false, true, 1.0, 8.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());

    private HudScenarios() {}

    static final class CapturingSender implements TurnOrderSync.Sender {
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
            helper.fail("a snapshot must have been sent to " + player.getUUID());
            return null;
        }
    }

    public static void startBroadcastsSnapshotInInitiativeOrder(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20, 10), husk1, husk2);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    TurnOrderSnapshot snapshot = sender.lastFor(helper, player);
                    requireEntryOrder(helper, snapshot, husk1, husk2, player);
                    requireCurrentActor(helper, snapshot, husk1);
                    Encounter encounter = encounter(helper, player);
                    if (!snapshot.encounterId().equals(encounter.id())) {
                        helper.fail("the snapshot must be keyed by the encounter id");
                    }
                    if (!snapshot.equals(TurnOrderSnapshots.build(helper.getLevel(), encounter))) {
                        helper.fail("the sent snapshot must be exactly the built snapshot");
                    }
                    requireEntryDetails(helper, snapshot, husk1, "minecraft:husk", false);
                    requireEntryDetails(helper, snapshot, player, "minecraft:player", true);
                    cleanUp(sender, player, husk1, husk2);
                })
                .thenSucceed();
    }

    public static void turnAdvanceMovesCurrentActor(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20, 10), husk1, husk2);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireCurrentActor(helper, sender.lastFor(helper, player), husk1);
                    encounter(helper, player).turnOrder().endTurn();
                })
                .thenExecuteAfter(2, () -> {
                    TurnOrderSnapshot snapshot = sender.lastFor(helper, player);
                    requireCurrentActor(helper, snapshot, husk2);
                    requireEntryOrder(helper, snapshot, husk1, husk2, player);
                    cleanUp(sender, player, husk1, husk2);
                })
                .thenSucceed();
    }

    public static void lateJoinerAppearsInSortedSlot(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk late = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireEntryOrder(helper, sender.lastFor(helper, player), husk, player);
                    ScenarioSupport.forceNaturals(
                            helper,
                            List.of(10),
                            () -> late.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f));
                })
                .thenExecuteAfter(2, () -> {
                    TurnOrderSnapshot snapshot = sender.lastFor(helper, player);
                    requireEntryOrder(helper, snapshot, husk, late, player);
                    requireCurrentActor(helper, snapshot, husk);
                    cleanUp(sender, player, husk, late);
                })
                .thenSucceed();
    }

    public static void removalDropsEntryKeepingRelativeOrder(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        Husk husk3 = ScenarioSupport.spawnHusk(helper, 6, 6);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20, 15, 10), husk1, husk2, husk3);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireEntryOrder(helper, sender.lastFor(helper, player), husk1, husk2, husk3, player);
                    husk2.kill();
                })
                .thenExecuteAfter(2, () -> {
                    TurnOrderSnapshot snapshot = sender.lastFor(helper, player);
                    requireEntryOrder(helper, snapshot, husk1, husk3, player);
                    requireCurrentActor(helper, snapshot, husk1);
                    cleanUp(sender, player, husk1, husk3);
                })
                .thenSucceed();
    }

    public static void actingParticipantRemovalPassesCurrentActor(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk1 = ScenarioSupport.spawnHusk(helper, 2, 2);
        Husk husk2 = ScenarioSupport.spawnHusk(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20, 10), husk1, husk2);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireCurrentActor(helper, sender.lastFor(helper, player), husk1);
                    husk1.discard();
                })
                .thenExecuteAfter(2, () -> {
                    TurnOrderSnapshot snapshot = sender.lastFor(helper, player);
                    requireEntryOrder(helper, snapshot, husk2, player);
                    requireCurrentActor(helper, snapshot, husk2);
                    cleanUp(sender, player, husk2);
                })
                .thenSucceed();
    }

    public static void encounterEndBroadcastsEmptySnapshot(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20), husk);
        UUID[] encounterId = new UUID[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    encounterId[0] = encounter(helper, player).id();
                    husk.kill();
                })
                .thenExecuteAfter(2, () -> {
                    TurnOrderSnapshot snapshot = sender.lastFor(helper, player);
                    if (!snapshot.isEmpty()) {
                        helper.fail("the encounter end must broadcast an empty snapshot");
                    }
                    if (!snapshot.encounterId().equals(encounterId[0])) {
                        helper.fail("the clearing snapshot must carry the ended encounter's id");
                    }
                    cleanUp(sender, player);
                })
                .thenSucceed();
    }

    public static void leavingPlayerReceivesEmptySnapshot(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    sender.lastFor(helper, player);
                    Vec3 far = helper.absoluteVec(new Vec3(30, 1, 30));
                    player.moveTo(far.x, far.y, far.z, 0, 0);
                })
                .thenExecuteAfter(2, () -> {
                    if (!sender.lastFor(helper, player).isEmpty()) {
                        helper.fail("a player leaving the encounter must have its bar cleared");
                    }
                    cleanUp(sender, player, husk);
                })
                .thenSucceed();
    }

    /** Since M9 the strip carries the countdown, so an otherwise unchanged turn resends once a second. */
    public static void unchangedStateResendsOnlyTheClock(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(true));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20), husk);
        int[] settled = new int[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> settled[0] = sender.sent.size())
                .thenExecuteAfter(41, () -> {
                    TurnOrderSnapshot first = sender.sent.get(settled[0] - 1).snapshot();
                    int resends = sender.sent.size() - settled[0];
                    if (resends > 3) {
                        helper.fail("an unchanged turn state must resend no faster than its clock, but sent " + resends
                                + " in 41 ticks");
                    }
                    for (CapturingSender.Sent sent : sender.sent.subList(settled[0], sender.sent.size())) {
                        if (!sent.snapshot().entries().equals(first.entries())
                                || sent.snapshot().currentActorId() != first.currentActorId()) {
                            helper.fail("only the countdown may change while the turn state stands still");
                        }
                    }
                    cleanUp(sender, player, husk);
                })
                .thenSucceed();
    }

    public static void disabledHudSendsNothing(GameTestHelper helper) {
        CapturingSender sender = prepare(helper, new HudConfig(false));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, List.of(1, 20), husk);
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    if (!sender.sent.isEmpty()) {
                        helper.fail("hud.enabled=false must suppress every snapshot send");
                    }
                    cleanUp(sender, player, husk);
                })
                .thenSucceed();
    }

    private static CapturingSender prepare(GameTestHelper helper, HudConfig hud) {
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ScenarioSupport.ACTIONS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideHudForTesting(hud);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
        CapturingSender sender = new CapturingSender();
        TurnOrderSync.overrideSenderForTesting(sender);
        return sender;
    }

    private static Encounter encounter(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must exist while its snapshots are inspected");
        }
        return encounter;
    }

    private static void requireEntryOrder(GameTestHelper helper, TurnOrderSnapshot snapshot, LivingEntity... expected) {
        List<Integer> actual = snapshot.entries().stream()
                .map(TurnOrderSnapshot.Entry::entityId)
                .toList();
        List<Integer> wanted =
                List.of(expected).stream().map(LivingEntity::getId).toList();
        if (!actual.equals(wanted)) {
            helper.fail("snapshot entries must be exactly " + wanted + " but were " + actual);
        }
    }

    private static void requireCurrentActor(GameTestHelper helper, TurnOrderSnapshot snapshot, LivingEntity expected) {
        if (snapshot.currentActorId() != expected.getId()) {
            helper.fail("the current actor must be " + expected.getId() + " but was " + snapshot.currentActorId());
        }
        boolean present = snapshot.entries().stream().anyMatch(entry -> entry.entityId() == snapshot.currentActorId());
        if (!present) {
            helper.fail("the current actor must always resolve to an entry in the snapshot");
        }
    }

    private static void requireEntryDetails(
            GameTestHelper helper, TurnOrderSnapshot snapshot, LivingEntity entity, String typeId, boolean isPlayer) {
        TurnOrderSnapshot.Entry entry = snapshot.entries().stream()
                .filter(candidate -> candidate.entityId() == entity.getId())
                .findFirst()
                .orElse(null);
        if (entry == null) {
            helper.fail("every participant must have a snapshot entry");
        }
        String name = entity.getDisplayName().getString();
        if (!entry.displayName().equals(name) || !entry.entityTypeId().equals(typeId) || entry.isPlayer() != isPlayer) {
            helper.fail("entry for " + name + " must be (" + name + ", " + typeId + ", " + isPlayer + ") but was ("
                    + entry.displayName() + ", " + entry.entityTypeId() + ", " + entry.isPlayer() + ")");
        }
    }

    private static void cleanUp(CapturingSender sender, LivingEntity... entities) {
        TurnOrderSync.clearSenderOverrideForTesting();
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
