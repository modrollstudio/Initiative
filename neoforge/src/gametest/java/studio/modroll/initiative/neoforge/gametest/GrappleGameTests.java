package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.GrappleScenarios;

/** NeoForge registration shim: delegates to the shared {@link GrappleScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class GrappleGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m8GrappleWin")
    public void grappleWinHoldsTheTarget(GameTestHelper helper) {
        GrappleScenarios.grappleWinHoldsTheTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8GrappleLoss")
    public void grappleLossLeavesTheTargetFree(GameTestHelper helper) {
        GrappleScenarios.grappleLossLeavesTheTargetFree(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8GrappleHands")
    public void grappleWithBothHandsFullIsRejected(GameTestHelper helper) {
        GrappleScenarios.grappleWithBothHandsFullIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8GrappleOff")
    public void disabledGrappleIsRejected(GameTestHelper helper) {
        GrappleScenarios.disabledGrappleIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8GrappleSpeed")
    public void grappledTargetHasNoMovementOnItsTurn(GameTestHelper helper) {
        GrappleScenarios.grappledTargetHasNoMovementOnItsTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8EscapeOffered")
    public void escapeIsOnlyOfferedWhileGrappled(GameTestHelper helper) {
        GrappleScenarios.escapeIsOnlyOfferedWhileGrappled(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8EscapeWin")
    public void escapeWinClearsTheHoldAndReturnsMovement(GameTestHelper helper) {
        GrappleScenarios.escapeWinClearsTheHoldAndReturnsMovement(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8EscapeLoss")
    public void escapeLossKeepsTheHoldAndSpendsTheAction(GameTestHelper helper) {
        GrappleScenarios.escapeLossKeepsTheHoldAndSpendsTheAction(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8GrappleLeave")
    public void aGrapplerLeavingFreesItsTarget(GameTestHelper helper) {
        GrappleScenarios.aGrapplerLeavingFreesItsTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8GrappleBreak")
    public void aHoldBreaksBeyondItsDistance(GameTestHelper helper) {
        GrappleScenarios.aHoldBreaksBeyondItsDistance(helper);
    }
}
