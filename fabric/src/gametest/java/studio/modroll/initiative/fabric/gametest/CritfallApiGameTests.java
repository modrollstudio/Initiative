package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.CritfallApiScenarios;

/** Fabric registration shim: delegates to the shared {@link CritfallApiScenarios} bodies. */
public class CritfallApiGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void setRollerForcesExactRolls(GameTestHelper helper) {
        CritfallApiScenarios.setRollerForcesExactRolls(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void suppressionFlagRoundTrips(GameTestHelper helper) {
        CritfallApiScenarios.suppressionFlagRoundTrips(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void effectiveProfilesAreQueryable(GameTestHelper helper) {
        CritfallApiScenarios.effectiveProfilesAreQueryable(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void attackRollResolvesWithoutTouchingTheWorld(GameTestHelper helper) {
        CritfallApiScenarios.attackRollResolvesWithoutTouchingTheWorld(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void drivenAttackAppliesDamageThroughTheApi(GameTestHelper helper) {
        CritfallApiScenarios.drivenAttackAppliesDamageThroughTheApi(helper);
    }
}
