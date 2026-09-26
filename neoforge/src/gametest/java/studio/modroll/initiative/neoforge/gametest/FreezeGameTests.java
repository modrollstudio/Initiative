package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.FreezeScenarios;

/** NeoForge registration shim: delegates to the shared {@link FreezeScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class FreezeGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m2bHold")
    public void frozenMobHoldsStillAndActsOnItsTurn(GameTestHelper helper) {
        FreezeScenarios.frozenMobHoldsStillAndActsOnItsTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2bExits")
    public void everyExitPathThawsFrozenState(GameTestHelper helper) {
        FreezeScenarios.everyExitPathThawsFrozenState(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2bDamage")
    public void frozenMobStillTakesDamage(GameTestHelper helper) {
        FreezeScenarios.frozenMobStillTakesDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2bExclude")
    public void excludedTypeIsNeverFrozenButKeepsItsTurnSlot(GameTestHelper helper) {
        FreezeScenarios.excludedTypeIsNeverFrozenButKeepsItsTurnSlot(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2bEffects")
    public void frozenMobStillTicksStatusEffects(GameTestHelper helper) {
        FreezeScenarios.frozenMobStillTicksStatusEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m2bToggle")
    public void freezeFlagOffThawsAndFreezesNothing(GameTestHelper helper) {
        FreezeScenarios.freezeFlagOffThawsAndFreezesNothing(helper);
    }
}
