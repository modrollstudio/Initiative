package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.HudScenarios;

/** NeoForge registration shim: delegates to the shared {@link HudScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class HudGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m2cHudStart")
    public void startBroadcastsSnapshotInInitiativeOrder(GameTestHelper helper) {
        HudScenarios.startBroadcastsSnapshotInInitiativeOrder(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2cHudAdvance")
    public void turnAdvanceMovesCurrentActor(GameTestHelper helper) {
        HudScenarios.turnAdvanceMovesCurrentActor(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2cHudJoin")
    public void lateJoinerAppearsInSortedSlot(GameTestHelper helper) {
        HudScenarios.lateJoinerAppearsInSortedSlot(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2cHudRemoval")
    public void removalDropsEntryKeepingRelativeOrder(GameTestHelper helper) {
        HudScenarios.removalDropsEntryKeepingRelativeOrder(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2cHudActingRemoval")
    public void actingParticipantRemovalPassesCurrentActor(GameTestHelper helper) {
        HudScenarios.actingParticipantRemovalPassesCurrentActor(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2cHudEnd")
    public void encounterEndBroadcastsEmptySnapshot(GameTestHelper helper) {
        HudScenarios.encounterEndBroadcastsEmptySnapshot(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2cHudPlayerLeave")
    public void leavingPlayerReceivesEmptySnapshot(GameTestHelper helper) {
        HudScenarios.leavingPlayerReceivesEmptySnapshot(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2cHudNoResend")
    public void unchangedStateResendsOnlyTheClock(GameTestHelper helper) {
        HudScenarios.unchangedStateResendsOnlyTheClock(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2cHudDisabled")
    public void disabledHudSendsNothing(GameTestHelper helper) {
        HudScenarios.disabledHudSendsNothing(helper);
    }
}
