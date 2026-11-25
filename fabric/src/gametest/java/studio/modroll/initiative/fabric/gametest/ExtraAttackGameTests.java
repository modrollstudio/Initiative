package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.ExtraAttackScenarios;

/** Fabric registration shim: delegates to the shared {@link ExtraAttackScenarios} bodies. */
public class ExtraAttackGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "extraAttackALevelFiveFighterAttacksTwice")
    public void aLevelFiveFighterAttacksTwice(GameTestHelper helper) {
        ExtraAttackScenarios.aLevelFiveFighterAttacksTwice(helper);
    }

    @GameTest(
            template = FabricGameTest.EMPTY_STRUCTURE,
            batch = "extraAttackALevelFourFighterAndALevelFiveWizardAttackOnce")
    public void aLevelFourFighterAndALevelFiveWizardAttackOnce(GameTestHelper helper) {
        ExtraAttackScenarios.aLevelFourFighterAndALevelFiveWizardAttackOnce(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "extraAttackExtraAttackOffLeavesOneAttack")
    public void extraAttackOffLeavesOneAttack(GameTestHelper helper) {
        ExtraAttackScenarios.extraAttackOffLeavesOneAttack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "extraAttackAShotCanBeTheSecondAttack")
    public void aShotCanBeTheSecondAttack(GameTestHelper helper) {
        ExtraAttackScenarios.aShotCanBeTheSecondAttack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "extraAttackAPlayerWithNoClassAttacksOnce")
    public void aPlayerWithNoClassAttacksOnce(GameTestHelper helper) {
        ExtraAttackScenarios.aPlayerWithNoClassAttacksOnce(helper);
    }
}
