package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.GrappleScenarios;

/** Fabric registration shim: delegates to the shared {@link GrappleScenarios} bodies. */
public class GrappleGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8GrappleWin")
    public void grappleWinHoldsTheTarget(GameTestHelper helper) {
        GrappleScenarios.grappleWinHoldsTheTarget(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8GrappleLoss")
    public void grappleLossLeavesTheTargetFree(GameTestHelper helper) {
        GrappleScenarios.grappleLossLeavesTheTargetFree(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8GrappleHands")
    public void grappleWithBothHandsFullIsRejected(GameTestHelper helper) {
        GrappleScenarios.grappleWithBothHandsFullIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8GrappleOff")
    public void disabledGrappleIsRejected(GameTestHelper helper) {
        GrappleScenarios.disabledGrappleIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8GrappleSpeed")
    public void grappledTargetHasNoMovementOnItsTurn(GameTestHelper helper) {
        GrappleScenarios.grappledTargetHasNoMovementOnItsTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8EscapeOffered")
    public void escapeIsOnlyOfferedWhileGrappled(GameTestHelper helper) {
        GrappleScenarios.escapeIsOnlyOfferedWhileGrappled(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8EscapeWin")
    public void escapeWinClearsTheHoldAndReturnsMovement(GameTestHelper helper) {
        GrappleScenarios.escapeWinClearsTheHoldAndReturnsMovement(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8EscapeLoss")
    public void escapeLossKeepsTheHoldAndSpendsTheAction(GameTestHelper helper) {
        GrappleScenarios.escapeLossKeepsTheHoldAndSpendsTheAction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8GrappleLeave")
    public void aGrapplerLeavingFreesItsTarget(GameTestHelper helper) {
        GrappleScenarios.aGrapplerLeavingFreesItsTarget(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8GrappleBreak")
    public void aHoldBreaksBeyondItsDistance(GameTestHelper helper) {
        GrappleScenarios.aHoldBreaksBeyondItsDistance(helper);
    }
}
