package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.EncounterScenarios;

/** Fabric registration shim: delegates to the shared {@link EncounterScenarios} bodies. */
public class EncounterGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m1Formation")
    public void attackFormsEncounterSuppressingBoth(GameTestHelper helper) {
        EncounterScenarios.attackFormsEncounterSuppressingBoth(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m1ExitPaths")
    public void everyExitPathClearsSuppression(GameTestHelper helper) {
        EncounterScenarios.everyExitPathClearsSuppression(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m1Outside")
    public void entityOutsideRadiusIsUntouched(GameTestHelper helper) {
        EncounterScenarios.entityOutsideRadiusIsUntouched(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m1Toggle")
    public void disabledFlagFormsNoEncounter(GameTestHelper helper) {
        EncounterScenarios.disabledFlagFormsNoEncounter(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m1Whiff")
    public void whiffedAttackStillFormsEncounter(GameTestHelper helper) {
        EncounterScenarios.whiffedAttackStillFormsEncounter(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "sightWall")
    public void wallKeepsAHostileOutOfTheFight(GameTestHelper helper) {
        EncounterScenarios.wallKeepsAHostileOutOfTheFight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "sightAttacker")
    public void attackerBehindAWallJoins(GameTestHelper helper) {
        EncounterScenarios.attackerBehindAWallJoins(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "sightGained")
    public void gainingSightMidFightJoins(GameTestHelper helper) {
        EncounterScenarios.gainingSightMidFightJoins(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "sightPlayer")
    public void playerBehindAWallJoinsOnceTheyCanSee(GameTestHelper helper) {
        EncounterScenarios.playerBehindAWallJoinsOnceTheyCanSee(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "neverJoins")
    public void taggedMobNeverJoins(GameTestHelper helper) {
        EncounterScenarios.taggedMobNeverJoins(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "sightToggle")
    public void sightNotRequiredPullsThroughWalls(GameTestHelper helper) {
        EncounterScenarios.sightNotRequiredPullsThroughWalls(helper);
    }
}
