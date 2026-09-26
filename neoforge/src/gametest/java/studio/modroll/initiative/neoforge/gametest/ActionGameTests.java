package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.ActionScenarios;

/** NeoForge registration shim: delegates to the shared {@link ActionScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class ActionGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m3Attack")
    public void attackOnYourTurnRollsTheD20(GameTestHelper helper) {
        ActionScenarios.attackOnYourTurnRollsTheD20(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3FocusFire")
    public void focusFireLandsBothDrivenHits(GameTestHelper helper) {
        ActionScenarios.focusFireLandsBothDrivenHits(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3NatOne")
    public void forcedNatOneMissesWithoutDamage(GameTestHelper helper) {
        ActionScenarios.forcedNatOneMissesWithoutDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m8AttackReach")
    public void attackBeyondReachIsRejectedWithoutARoll(GameTestHelper helper) {
        ActionScenarios.attackBeyondReachIsRejectedWithoutARoll(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3OutOfTurn")
    public void outOfTurnAttackIsRejectedWithoutARoll(GameTestHelper helper) {
        ActionScenarios.outOfTurnAttackIsRejectedWithoutARoll(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3SecondAttack")
    public void secondAttackInOneTurnIsRejected(GameTestHelper helper) {
        ActionScenarios.secondAttackInOneTurnIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3EndTurn")
    public void explicitEndTurnAdvancesOnlyForTheActor(GameTestHelper helper) {
        ActionScenarios.explicitEndTurnAdvancesOnlyForTheActor(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3Movement")
    public void movementBudgetIsConsumedAndEnforced(GameTestHelper helper) {
        ActionScenarios.movementBudgetIsConsumedAndEnforced(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3MobTurn")
    public void mobTurnDrivesItsAttackThroughTheApi(GameTestHelper helper) {
        ActionScenarios.mobTurnDrivesItsAttackThroughTheApi(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3DamageCancel")
    public void vanillaDamageBetweenParticipantsIsCanceled(GameTestHelper helper) {
        ActionScenarios.vanillaDamageBetweenParticipantsIsCanceled(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m3Disabled")
    public void actionsDisabledRestoresRealTimeWindows(GameTestHelper helper) {
        ActionScenarios.actionsDisabledRestoresRealTimeWindows(helper);
    }
}
