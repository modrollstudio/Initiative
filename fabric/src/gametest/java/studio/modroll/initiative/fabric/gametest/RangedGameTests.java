package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.RangedScenarios;

/** Fabric registration shim: delegates to the shared {@link RangedScenarios} bodies. */
public class RangedGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedPull")
    public void aRangedHitFromOutsideBindsTheShooterToTheTurnOrder(GameTestHelper helper) {
        RangedScenarios.aRangedHitFromOutsideBindsTheShooterToTheTurnOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedOneEncounter")
    public void theShooterJoinsTheFightItShotInto(GameTestHelper helper) {
        RangedScenarios.theShooterJoinsTheFightItShotInto(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedOtherFight")
    public void aShooterInAnotherFightStaysThere(GameTestHelper helper) {
        RangedScenarios.aShooterInAnotherFightStaysThere(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedOwnerless")
    public void anArrowWithNoLivingShooterAddsNothing(GameTestHelper helper) {
        RangedScenarios.anArrowWithNoLivingShooterAddsNothing(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedPvp")
    public void aPlayersArrowIntoAPlayerFormsNoHostile(GameTestHelper helper) {
        RangedScenarios.aPlayersArrowIntoAPlayerFormsNoHostile(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedSightsLost")
    public void aShooterThatLosesItsTargetLeaves(GameTestHelper helper) {
        RangedScenarios.aShooterThatLosesItsTargetLeaves(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rangedToggle")
    public void disabledFlagLeavesTheShooterToTheLeaveRadius(GameTestHelper helper) {
        RangedScenarios.disabledFlagLeavesTheShooterToTheLeaveRadius(helper);
    }
}
