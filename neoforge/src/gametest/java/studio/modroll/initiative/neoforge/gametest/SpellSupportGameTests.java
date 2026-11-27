package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.SpellSupportScenarios;

/** NeoForge registration shim: delegates to the shared {@link SpellSupportScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpellSupportGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "spellSupport_anUnlistedActionChecksAndStepsForFree")
    public void anUnlistedActionChecksAndStepsForFree(GameTestHelper helper) {
        SpellSupportScenarios.anUnlistedActionChecksAndStepsForFree(helper);
    }

    @GameTest(template = TEMPLATE, batch = "spellSupport_turnListenersFollowTheTurns")
    public void turnListenersFollowTheTurns(GameTestHelper helper) {
        SpellSupportScenarios.turnListenersFollowTheTurns(helper);
    }

    @GameTest(template = TEMPLATE, batch = "spellSupport_engageStartsAFight")
    public void engageStartsAFight(GameTestHelper helper) {
        SpellSupportScenarios.engageStartsAFight(helper);
    }

    @GameTest(template = TEMPLATE, batch = "spellSupport_aPreparedAttackCarriesTheFightsRules")
    public void aPreparedAttackCarriesTheFightsRules(GameTestHelper helper) {
        SpellSupportScenarios.aPreparedAttackCarriesTheFightsRules(helper);
    }
}
