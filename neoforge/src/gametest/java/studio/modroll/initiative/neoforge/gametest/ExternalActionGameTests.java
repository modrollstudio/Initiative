package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.ExternalActionScenarios;

/** NeoForge registration shim: delegates to the shared {@link ExternalActionScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class ExternalActionGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "m7ExternalOutOfTurn")
    public void externalActionIsRejectedOutOfTurn(GameTestHelper helper) {
        ExternalActionScenarios.externalActionIsRejectedOutOfTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m7ExternalSpends")
    public void externalActionRunsAndSpendsTheAction(GameTestHelper helper) {
        ExternalActionScenarios.externalActionRunsAndSpendsTheAction(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m7ExternalTarget")
    public void externalActionRejectsAFriendlyTarget(GameTestHelper helper) {
        ExternalActionScenarios.externalActionRejectsAFriendlyTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m7ExternalDisabled")
    public void disabledExternalActionIsRejected(GameTestHelper helper) {
        ExternalActionScenarios.disabledExternalActionIsRejected(helper);
    }

    @GameTest(template = TEMPLATE, batch = "m7ExternalAnimation")
    public void externalActionRollAnimatesLikeABuiltin(GameTestHelper helper) {
        ExternalActionScenarios.externalActionRollAnimatesLikeABuiltin(helper);
    }
}
