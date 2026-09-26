package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.StandardActionScenarios;

/** Fabric registration shim: delegates to the shared {@link StandardActionScenarios} bodies. */
public class StandardActionGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aDash")
    public void dashExtendsMovementAndStillStops(GameTestHelper helper) {
        StandardActionScenarios.dashExtendsMovementAndStillStops(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aDodge")
    public void dodgingTargetIsAttackedWithDisadvantage(GameTestHelper helper) {
        StandardActionScenarios.dodgingTargetIsAttackedWithDisadvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aMobDodge")
    public void mobAttackingADodgingPlayerRollsWithDisadvantage(GameTestHelper helper) {
        StandardActionScenarios.mobAttackingADodgingPlayerRollsWithDisadvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aHelp")
    public void helpedAllyAttacksWithAdvantageAndConsumesIt(GameTestHelper helper) {
        StandardActionScenarios.helpedAllyAttacksWithAdvantageAndConsumesIt(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aDisengage")
    public void disengageSetsTheFlagAndSpendsTheAction(GameTestHelper helper) {
        StandardActionScenarios.disengageSetsTheFlagAndSpendsTheAction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aDodgeAction")
    public void dodgeActionSetsTheFlagAndSpendsTheAction(GameTestHelper helper) {
        StandardActionScenarios.dodgeActionSetsTheFlagAndSpendsTheAction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aOutOfTurn")
    public void outOfTurnStandardActionIsRejected(GameTestHelper helper) {
        StandardActionScenarios.outOfTurnStandardActionIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aDisabled")
    public void disabledActionIsUnavailableWithOthersUnaffected(GameTestHelper helper) {
        StandardActionScenarios.disabledActionIsUnavailableWithOthersUnaffected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m4aFlagClear")
    public void dodgeClearsAtTheDodgersNextTurn(GameTestHelper helper) {
        StandardActionScenarios.dodgeClearsAtTheDodgersNextTurn(helper);
    }
}
