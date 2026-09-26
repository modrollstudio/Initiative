package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.ActionScenarios;

/** Fabric registration shim: delegates to the shared {@link ActionScenarios} bodies. */
public class ActionGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3Attack")
    public void attackOnYourTurnRollsTheD20(GameTestHelper helper) {
        ActionScenarios.attackOnYourTurnRollsTheD20(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3FocusFire")
    public void focusFireLandsBothDrivenHits(GameTestHelper helper) {
        ActionScenarios.focusFireLandsBothDrivenHits(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3NatOne")
    public void forcedNatOneMissesWithoutDamage(GameTestHelper helper) {
        ActionScenarios.forcedNatOneMissesWithoutDamage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m8AttackReach")
    public void attackBeyondReachIsRejectedWithoutARoll(GameTestHelper helper) {
        ActionScenarios.attackBeyondReachIsRejectedWithoutARoll(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3OutOfTurn")
    public void outOfTurnAttackIsRejectedWithoutARoll(GameTestHelper helper) {
        ActionScenarios.outOfTurnAttackIsRejectedWithoutARoll(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3SecondAttack")
    public void secondAttackInOneTurnIsRejected(GameTestHelper helper) {
        ActionScenarios.secondAttackInOneTurnIsRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3EndTurn")
    public void explicitEndTurnAdvancesOnlyForTheActor(GameTestHelper helper) {
        ActionScenarios.explicitEndTurnAdvancesOnlyForTheActor(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3Movement")
    public void movementBudgetIsConsumedAndEnforced(GameTestHelper helper) {
        ActionScenarios.movementBudgetIsConsumedAndEnforced(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3MobTurn")
    public void mobTurnDrivesItsAttackThroughTheApi(GameTestHelper helper) {
        ActionScenarios.mobTurnDrivesItsAttackThroughTheApi(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3DamageCancel")
    public void vanillaDamageBetweenParticipantsIsCanceled(GameTestHelper helper) {
        ActionScenarios.vanillaDamageBetweenParticipantsIsCanceled(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m3Disabled")
    public void actionsDisabledRestoresRealTimeWindows(GameTestHelper helper) {
        ActionScenarios.actionsDisabledRestoresRealTimeWindows(helper);
    }
}
