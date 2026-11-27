package studio.modroll.initiative.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.initiative.gametest.SpellSupportScenarios;

/** Fabric registration shim: delegates to the shared {@link SpellSupportScenarios} bodies. */
public class SpellSupportGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spellSupport_anUnlistedActionChecksAndStepsForFree")
    public void anUnlistedActionChecksAndStepsForFree(GameTestHelper helper) {
        SpellSupportScenarios.anUnlistedActionChecksAndStepsForFree(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spellSupport_turnListenersFollowTheTurns")
    public void turnListenersFollowTheTurns(GameTestHelper helper) {
        SpellSupportScenarios.turnListenersFollowTheTurns(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spellSupport_engageStartsAFight")
    public void engageStartsAFight(GameTestHelper helper) {
        SpellSupportScenarios.engageStartsAFight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spellSupport_aPreparedAttackCarriesTheFightsRules")
    public void aPreparedAttackCarriesTheFightsRules(GameTestHelper helper) {
        SpellSupportScenarios.aPreparedAttackCarriesTheFightsRules(helper);
    }
}
