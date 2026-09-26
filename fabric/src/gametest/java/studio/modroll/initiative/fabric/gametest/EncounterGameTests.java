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
}
