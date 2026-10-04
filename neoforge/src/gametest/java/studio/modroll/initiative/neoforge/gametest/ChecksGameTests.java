package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.ChecksScenarios;

/** NeoForge registration shim: delegates to the shared {@link ChecksScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class ChecksGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "checksSkills")
    public void contestsRollTheRightSkills(GameTestHelper helper) {
        ChecksScenarios.contestsRollTheRightSkills(helper);
    }

    @GameTest(template = TEMPLATE, batch = "checksShove")
    public void shoveRollsSkillsAndShowsTheContest(GameTestHelper helper) {
        ChecksScenarios.shoveRollsSkillsAndShowsTheContest(helper);
    }

    @GameTest(template = TEMPLATE, batch = "checksGrapple")
    public void grappleRollsSkills(GameTestHelper helper) {
        ChecksScenarios.grappleRollsSkills(helper);
    }

    @GameTest(template = TEMPLATE, batch = "checksEscape")
    public void escapeRollsSkills(GameTestHelper helper) {
        ChecksScenarios.escapeRollsSkills(helper);
    }

    @GameTest(template = TEMPLATE, batch = "checksHide")
    public void hideStoresTheStealthTotal(GameTestHelper helper) {
        ChecksScenarios.hideStoresTheStealthTotal(helper);
    }

    @GameTest(template = TEMPLATE, batch = "checksKeenWatcher")
    public void aKeenEnemyRevealsTheHiderOnItsTurn(GameTestHelper helper) {
        ChecksScenarios.aKeenEnemyRevealsTheHiderOnItsTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "checksDullWatcher")
    public void aDullEnemyLeavesTheHiderHidden(GameTestHelper helper) {
        ChecksScenarios.aDullEnemyLeavesTheHiderHidden(helper);
    }

    @GameTest(template = TEMPLATE, batch = "checksToggledOff")
    public void toggledOffUsesFlatBonuses(GameTestHelper helper) {
        ChecksScenarios.toggledOffUsesFlatBonuses(helper);
    }

    @GameTest(template = TEMPLATE, batch = "checksAbsent")
    public void withoutChecksUsesFlatBonuses(GameTestHelper helper) {
        ChecksScenarios.withoutChecksUsesFlatBonuses(helper);
    }
}
