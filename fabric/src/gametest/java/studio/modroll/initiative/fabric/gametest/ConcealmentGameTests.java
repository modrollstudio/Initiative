package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.ConcealmentScenarios;

/** Fabric registration shim: delegates to the shared {@link ConcealmentScenarios} bodies. */
public class ConcealmentGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "concealAcquire")
    public void aHiddenParticipantIsNotNewlyTargeted(GameTestHelper helper) {
        ConcealmentScenarios.aHiddenParticipantIsNotNewlyTargeted(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "concealExisting")
    public void hidingTakesAwayATargetTheMobAlreadyHeld(GameTestHelper helper) {
        ConcealmentScenarios.hidingTakesAwayATargetTheMobAlreadyHeld(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "concealBreak")
    public void breakingHiddenLetsTheMobTargetAgain(GameTestHelper helper) {
        ConcealmentScenarios.breakingHiddenLetsTheMobTargetAgain(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "concealExit")
    public void everyExitPathRevealsTheHider(GameTestHelper helper) {
        ConcealmentScenarios.everyExitPathRevealsTheHider(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "concealToggle")
    public void theToggleOffLeavesTargetingAlone(GameTestHelper helper) {
        ConcealmentScenarios.theToggleOffLeavesTargetingAlone(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "concealNeutral")
    public void aHiddenPlayerHoldsAProvokedNeutralInTheFight(GameTestHelper helper) {
        ConcealmentScenarios.aHiddenPlayerHoldsAProvokedNeutralInTheFight(helper);
    }
}
