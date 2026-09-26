package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.RangedScenarios;

/** NeoForge registration shim: delegates to the shared {@link RangedScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class RangedGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "rangedPull")
    public void aRangedHitFromOutsideBindsTheShooterToTheTurnOrder(GameTestHelper helper) {
        RangedScenarios.aRangedHitFromOutsideBindsTheShooterToTheTurnOrder(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedOneEncounter")
    public void theShooterJoinsTheFightItShotInto(GameTestHelper helper) {
        RangedScenarios.theShooterJoinsTheFightItShotInto(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedOtherFight")
    public void aShooterInAnotherFightStaysThere(GameTestHelper helper) {
        RangedScenarios.aShooterInAnotherFightStaysThere(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedOwnerless")
    public void anArrowWithNoLivingShooterAddsNothing(GameTestHelper helper) {
        RangedScenarios.anArrowWithNoLivingShooterAddsNothing(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedPvp")
    public void aPlayersArrowIntoAPlayerFormsNoHostile(GameTestHelper helper) {
        RangedScenarios.aPlayersArrowIntoAPlayerFormsNoHostile(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedSightsLost")
    public void aShooterThatLosesItsTargetLeaves(GameTestHelper helper) {
        RangedScenarios.aShooterThatLosesItsTargetLeaves(helper);
    }

    @GameTest(template = TEMPLATE, batch = "rangedToggle")
    public void disabledFlagLeavesTheShooterToTheLeaveRadius(GameTestHelper helper) {
        RangedScenarios.disabledFlagLeavesTheShooterToTheLeaveRadius(helper);
    }
}
