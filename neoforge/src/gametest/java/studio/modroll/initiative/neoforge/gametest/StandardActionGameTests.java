package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.StandardActionScenarios;

/** NeoForge registration shim: delegates to the shared {@link StandardActionScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class StandardActionGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m4aDash")
    public void dashExtendsMovementAndStillStops(GameTestHelper helper) {
        StandardActionScenarios.dashExtendsMovementAndStillStops(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4aDodge")
    public void dodgingTargetIsAttackedWithDisadvantage(GameTestHelper helper) {
        StandardActionScenarios.dodgingTargetIsAttackedWithDisadvantage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4aMobDodge")
    public void mobAttackingADodgingPlayerRollsWithDisadvantage(GameTestHelper helper) {
        StandardActionScenarios.mobAttackingADodgingPlayerRollsWithDisadvantage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4aHelp")
    public void helpedAllyAttacksWithAdvantageAndConsumesIt(GameTestHelper helper) {
        StandardActionScenarios.helpedAllyAttacksWithAdvantageAndConsumesIt(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4aDisengage")
    public void disengageSetsTheFlagAndSpendsTheAction(GameTestHelper helper) {
        StandardActionScenarios.disengageSetsTheFlagAndSpendsTheAction(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4aDodgeAction")
    public void dodgeActionSetsTheFlagAndSpendsTheAction(GameTestHelper helper) {
        StandardActionScenarios.dodgeActionSetsTheFlagAndSpendsTheAction(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4aOutOfTurn")
    public void outOfTurnStandardActionIsRejected(GameTestHelper helper) {
        StandardActionScenarios.outOfTurnStandardActionIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4aDisabled")
    public void disabledActionIsUnavailableWithOthersUnaffected(GameTestHelper helper) {
        StandardActionScenarios.disabledActionIsUnavailableWithOthersUnaffected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m4aFlagClear")
    public void dodgeClearsAtTheDodgersNextTurn(GameTestHelper helper) {
        StandardActionScenarios.dodgeClearsAtTheDodgersNextTurn(helper);
    }
}
