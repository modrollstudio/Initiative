package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.NativeActionScenarios;

/** NeoForge registration shim: delegates to the shared {@link NativeActionScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class NativeActionGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m5aShoveWin")
    public void shoveWinKnocksTargetBack(GameTestHelper helper) {
        NativeActionScenarios.shoveWinKnocksTargetBack(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aShoveLoss")
    public void shoveLossDoesNothingButSpendsAction(GameTestHelper helper) {
        NativeActionScenarios.shoveLossDoesNothingButSpendsAction(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aShoveReach")
    public void shoveOutOfReachIsRejected(GameTestHelper helper) {
        NativeActionScenarios.shoveOutOfReachIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aShoveTurn")
    public void shoveOutOfTurnIsRejected(GameTestHelper helper) {
        NativeActionScenarios.shoveOutOfTurnIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aShoveOff")
    public void shoveOffIsRejected(GameTestHelper helper) {
        NativeActionScenarios.shoveOffIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aNoAction")
    public void nativeActionWithoutAnActionIsRejected(GameTestHelper helper) {
        NativeActionScenarios.nativeActionWithoutAnActionIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aBlink")
    public void blinkConsumesPearlAndAction(GameTestHelper helper) {
        NativeActionScenarios.blinkConsumesPearlAndAction(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aBlinkNoPearl")
    public void blinkWithoutPearlIsRejected(GameTestHelper helper) {
        NativeActionScenarios.blinkWithoutPearlIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aBlinkOff")
    public void blinkOffIsRejected(GameTestHelper helper) {
        NativeActionScenarios.blinkOffIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aReelWin")
    public void reelWinPullsTargetTowardActor(GameTestHelper helper) {
        NativeActionScenarios.reelWinPullsTargetTowardActor(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aReelLoss")
    public void reelLossDoesNotMoveButUsesRodAndAction(GameTestHelper helper) {
        NativeActionScenarios.reelLossDoesNotMoveButUsesRodAndAction(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aReelNoRod")
    public void reelWithoutRodIsRejected(GameTestHelper helper) {
        NativeActionScenarios.reelWithoutRodIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m5aReelOff")
    public void reelOffIsRejected(GameTestHelper helper) {
        NativeActionScenarios.reelOffIsRejected(helper);
    }
}
