package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.ConcealmentScenarios;

/** NeoForge registration shim: delegates to the shared {@link ConcealmentScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class ConcealmentGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "concealAcquire")
    public void aHiddenParticipantIsNotNewlyTargeted(GameTestHelper helper) {
        ConcealmentScenarios.aHiddenParticipantIsNotNewlyTargeted(helper);
    }

    @GameTest(template = TEMPLATE, batch = "concealExisting")
    public void hidingTakesAwayATargetTheMobAlreadyHeld(GameTestHelper helper) {
        ConcealmentScenarios.hidingTakesAwayATargetTheMobAlreadyHeld(helper);
    }

    @GameTest(template = TEMPLATE, batch = "concealBreak")
    public void breakingHiddenLetsTheMobTargetAgain(GameTestHelper helper) {
        ConcealmentScenarios.breakingHiddenLetsTheMobTargetAgain(helper);
    }

    @GameTest(template = TEMPLATE, batch = "concealExit")
    public void everyExitPathRevealsTheHider(GameTestHelper helper) {
        ConcealmentScenarios.everyExitPathRevealsTheHider(helper);
    }

    @GameTest(template = TEMPLATE, batch = "concealToggle")
    public void theToggleOffLeavesTargetingAlone(GameTestHelper helper) {
        ConcealmentScenarios.theToggleOffLeavesTargetingAlone(helper);
    }

    @GameTest(template = TEMPLATE, batch = "concealNeutral")
    public void aHiddenPlayerHoldsAProvokedNeutralInTheFight(GameTestHelper helper) {
        ConcealmentScenarios.aHiddenPlayerHoldsAProvokedNeutralInTheFight(helper);
    }
}
