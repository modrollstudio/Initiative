package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.ExtraAttackScenarios;

/** NeoForge registration shim: delegates to the shared {@link ExtraAttackScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class ExtraAttackGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "extraAttackALevelFiveFighterAttacksTwice")
    public void aLevelFiveFighterAttacksTwice(GameTestHelper helper) {
        ExtraAttackScenarios.aLevelFiveFighterAttacksTwice(helper);
    }

    @GameTest(template = TEMPLATE, batch = "extraAttackALevelFourFighterAndALevelFiveWizardAttackOnce")
    public void aLevelFourFighterAndALevelFiveWizardAttackOnce(GameTestHelper helper) {
        ExtraAttackScenarios.aLevelFourFighterAndALevelFiveWizardAttackOnce(helper);
    }

    @GameTest(template = TEMPLATE, batch = "extraAttackExtraAttackOffLeavesOneAttack")
    public void extraAttackOffLeavesOneAttack(GameTestHelper helper) {
        ExtraAttackScenarios.extraAttackOffLeavesOneAttack(helper);
    }

    @GameTest(template = TEMPLATE, batch = "extraAttackAShotCanBeTheSecondAttack")
    public void aShotCanBeTheSecondAttack(GameTestHelper helper) {
        ExtraAttackScenarios.aShotCanBeTheSecondAttack(helper);
    }

    @GameTest(template = TEMPLATE, batch = "extraAttackAPlayerWithNoClassAttacksOnce")
    public void aPlayerWithNoClassAttacksOnce(GameTestHelper helper) {
        ExtraAttackScenarios.aPlayerWithNoClassAttacksOnce(helper);
    }
}
