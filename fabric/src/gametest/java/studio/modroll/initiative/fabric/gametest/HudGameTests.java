package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.HudScenarios;

/** Fabric registration shim: delegates to the shared {@link HudScenarios} bodies. */
public class HudGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudStart")
    public void startBroadcastsSnapshotInInitiativeOrder(GameTestHelper helper) {
        HudScenarios.startBroadcastsSnapshotInInitiativeOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudAdvance")
    public void turnAdvanceMovesCurrentActor(GameTestHelper helper) {
        HudScenarios.turnAdvanceMovesCurrentActor(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudJoin")
    public void lateJoinerAppearsInSortedSlot(GameTestHelper helper) {
        HudScenarios.lateJoinerAppearsInSortedSlot(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudRemoval")
    public void removalDropsEntryKeepingRelativeOrder(GameTestHelper helper) {
        HudScenarios.removalDropsEntryKeepingRelativeOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudActingRemoval")
    public void actingParticipantRemovalPassesCurrentActor(GameTestHelper helper) {
        HudScenarios.actingParticipantRemovalPassesCurrentActor(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudEnd")
    public void encounterEndBroadcastsEmptySnapshot(GameTestHelper helper) {
        HudScenarios.encounterEndBroadcastsEmptySnapshot(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudPlayerLeave")
    public void leavingPlayerReceivesEmptySnapshot(GameTestHelper helper) {
        HudScenarios.leavingPlayerReceivesEmptySnapshot(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudNoResend")
    public void unchangedStateResendsOnlyTheClock(GameTestHelper helper) {
        HudScenarios.unchangedStateResendsOnlyTheClock(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2cHudDisabled")
    public void disabledHudSendsNothing(GameTestHelper helper) {
        HudScenarios.disabledHudSendsNothing(helper);
    }
}
