package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.EncounterScenarios;

/** NeoForge registration shim: delegates to the shared {@link EncounterScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class EncounterGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m1Formation")
    public void attackFormsEncounterSuppressingBoth(GameTestHelper helper) {
        EncounterScenarios.attackFormsEncounterSuppressingBoth(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m1ExitPaths")
    public void everyExitPathClearsSuppression(GameTestHelper helper) {
        EncounterScenarios.everyExitPathClearsSuppression(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m1Outside")
    public void entityOutsideRadiusIsUntouched(GameTestHelper helper) {
        EncounterScenarios.entityOutsideRadiusIsUntouched(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m1Toggle")
    public void disabledFlagFormsNoEncounter(GameTestHelper helper) {
        EncounterScenarios.disabledFlagFormsNoEncounter(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m1Whiff")
    public void whiffedAttackStillFormsEncounter(GameTestHelper helper) {
        EncounterScenarios.whiffedAttackStillFormsEncounter(helper);
    }

    @GameTest(template = TEMPLATE, batch = "sightWall")
    public void wallKeepsAHostileOutOfTheFight(GameTestHelper helper) {
        EncounterScenarios.wallKeepsAHostileOutOfTheFight(helper);
    }

    @GameTest(template = TEMPLATE, batch = "sightAttacker")
    public void attackerBehindAWallJoins(GameTestHelper helper) {
        EncounterScenarios.attackerBehindAWallJoins(helper);
    }

    @GameTest(template = TEMPLATE, batch = "sightGained")
    public void gainingSightMidFightJoins(GameTestHelper helper) {
        EncounterScenarios.gainingSightMidFightJoins(helper);
    }

    @GameTest(template = TEMPLATE, batch = "sightPlayer")
    public void playerBehindAWallJoinsOnceTheyCanSee(GameTestHelper helper) {
        EncounterScenarios.playerBehindAWallJoinsOnceTheyCanSee(helper);
    }

    @GameTest(template = TEMPLATE, batch = "neverJoins")
    public void taggedMobNeverJoins(GameTestHelper helper) {
        EncounterScenarios.taggedMobNeverJoins(helper);
    }

    @GameTest(template = TEMPLATE, batch = "sightToggle")
    public void sightNotRequiredPullsThroughWalls(GameTestHelper helper) {
        EncounterScenarios.sightNotRequiredPullsThroughWalls(helper);
    }
}
