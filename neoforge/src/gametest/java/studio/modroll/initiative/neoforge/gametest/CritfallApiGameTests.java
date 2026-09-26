package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.CritfallApiScenarios;

/** NeoForge registration shim: delegates to the shared {@link CritfallApiScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class CritfallApiGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void setRollerForcesExactRolls(GameTestHelper helper) {
        CritfallApiScenarios.setRollerForcesExactRolls(helper);
    }

    @GameTest(template = TEMPLATE)
    public void suppressionFlagRoundTrips(GameTestHelper helper) {
        CritfallApiScenarios.suppressionFlagRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE)
    public void effectiveProfilesAreQueryable(GameTestHelper helper) {
        CritfallApiScenarios.effectiveProfilesAreQueryable(helper);
    }

    @GameTest(template = TEMPLATE)
    public void attackRollResolvesWithoutTouchingTheWorld(GameTestHelper helper) {
        CritfallApiScenarios.attackRollResolvesWithoutTouchingTheWorld(helper);
    }

    @GameTest(template = TEMPLATE)
    public void drivenAttackAppliesDamageThroughTheApi(GameTestHelper helper) {
        CritfallApiScenarios.drivenAttackAppliesDamageThroughTheApi(helper);
    }
}
