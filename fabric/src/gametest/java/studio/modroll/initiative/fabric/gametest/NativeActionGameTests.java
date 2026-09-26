package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.NativeActionScenarios;

/** Fabric registration shim: delegates to the shared {@link NativeActionScenarios} bodies. */
public class NativeActionGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aShoveWin")
    public void shoveWinKnocksTargetBack(GameTestHelper helper) {
        NativeActionScenarios.shoveWinKnocksTargetBack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aShoveLoss")
    public void shoveLossDoesNothingButSpendsAction(GameTestHelper helper) {
        NativeActionScenarios.shoveLossDoesNothingButSpendsAction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aShoveReach")
    public void shoveOutOfReachIsRejected(GameTestHelper helper) {
        NativeActionScenarios.shoveOutOfReachIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aShoveTurn")
    public void shoveOutOfTurnIsRejected(GameTestHelper helper) {
        NativeActionScenarios.shoveOutOfTurnIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aShoveOff")
    public void shoveOffIsRejected(GameTestHelper helper) {
        NativeActionScenarios.shoveOffIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aNoAction")
    public void nativeActionWithoutAnActionIsRejected(GameTestHelper helper) {
        NativeActionScenarios.nativeActionWithoutAnActionIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aBlink")
    public void blinkConsumesPearlAndAction(GameTestHelper helper) {
        NativeActionScenarios.blinkConsumesPearlAndAction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aBlinkNoPearl")
    public void blinkWithoutPearlIsRejected(GameTestHelper helper) {
        NativeActionScenarios.blinkWithoutPearlIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aBlinkOff")
    public void blinkOffIsRejected(GameTestHelper helper) {
        NativeActionScenarios.blinkOffIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aReelWin")
    public void reelWinPullsTargetTowardActor(GameTestHelper helper) {
        NativeActionScenarios.reelWinPullsTargetTowardActor(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aReelLoss")
    public void reelLossDoesNotMoveButUsesRodAndAction(GameTestHelper helper) {
        NativeActionScenarios.reelLossDoesNotMoveButUsesRodAndAction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aReelNoRod")
    public void reelWithoutRodIsRejected(GameTestHelper helper) {
        NativeActionScenarios.reelWithoutRodIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m5aReelOff")
    public void reelOffIsRejected(GameTestHelper helper) {
        NativeActionScenarios.reelOffIsRejected(helper);
    }
}
