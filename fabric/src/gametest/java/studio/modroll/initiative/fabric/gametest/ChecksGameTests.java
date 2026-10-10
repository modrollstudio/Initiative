package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.ChecksScenarios;

/** Fabric registration shim: delegates to the shared {@link ChecksScenarios} bodies. */
public class ChecksGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksSkills")
    public void contestsRollTheRightSkills(GameTestHelper helper) {
        ChecksScenarios.contestsRollTheRightSkills(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksShove")
    public void shoveRollsSkillsAndShowsTheContest(GameTestHelper helper) {
        ChecksScenarios.shoveRollsSkillsAndShowsTheContest(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksGrapple")
    public void grappleRollsSkills(GameTestHelper helper) {
        ChecksScenarios.grappleRollsSkills(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksEscape")
    public void escapeRollsSkills(GameTestHelper helper) {
        ChecksScenarios.escapeRollsSkills(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksHide")
    public void hideStoresTheStealthTotal(GameTestHelper helper) {
        ChecksScenarios.hideStoresTheStealthTotal(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksHideMode")
    public void hideShowsTheModeRolled(GameTestHelper helper) {
        ChecksScenarios.hideShowsTheModeRolled(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksHideCanceled")
    public void aCanceledHideFailsWithoutDice(GameTestHelper helper) {
        ChecksScenarios.aCanceledHideFailsWithoutDice(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksShoveCanceled")
    public void aCanceledShoveFailsWithoutDice(GameTestHelper helper) {
        ChecksScenarios.aCanceledShoveFailsWithoutDice(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksInitiative")
    public void initiativeAddsDexterity(GameTestHelper helper) {
        ChecksScenarios.initiativeAddsDexterity(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksKeenWatcher")
    public void aKeenEnemyRevealsTheHiderOnItsTurn(GameTestHelper helper) {
        ChecksScenarios.aKeenEnemyRevealsTheHiderOnItsTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksDullWatcher")
    public void aDullEnemyLeavesTheHiderHidden(GameTestHelper helper) {
        ChecksScenarios.aDullEnemyLeavesTheHiderHidden(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksToggledOff")
    public void toggledOffUsesFlatBonuses(GameTestHelper helper) {
        ChecksScenarios.toggledOffUsesFlatBonuses(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "checksAbsent")
    public void withoutChecksUsesFlatBonuses(GameTestHelper helper) {
        ChecksScenarios.withoutChecksUsesFlatBonuses(helper);
    }
}
