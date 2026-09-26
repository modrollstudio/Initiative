package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.FreezeScenarios;

/** Fabric registration shim: delegates to the shared {@link FreezeScenarios} bodies. */
public class FreezeGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2bHold")
    public void frozenMobHoldsStillAndActsOnItsTurn(GameTestHelper helper) {
        FreezeScenarios.frozenMobHoldsStillAndActsOnItsTurn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2bExits")
    public void everyExitPathThawsFrozenState(GameTestHelper helper) {
        FreezeScenarios.everyExitPathThawsFrozenState(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2bDamage")
    public void frozenMobStillTakesDamage(GameTestHelper helper) {
        FreezeScenarios.frozenMobStillTakesDamage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2bExclude")
    public void excludedTypeIsNeverFrozenButKeepsItsTurnSlot(GameTestHelper helper) {
        FreezeScenarios.excludedTypeIsNeverFrozenButKeepsItsTurnSlot(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2bEffects")
    public void frozenMobStillTicksStatusEffects(GameTestHelper helper) {
        FreezeScenarios.frozenMobStillTicksStatusEffects(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "m2bToggle")
    public void freezeFlagOffThawsAndFreezesNothing(GameTestHelper helper) {
        FreezeScenarios.freezeFlagOffThawsAndFreezesNothing(helper);
    }
}
