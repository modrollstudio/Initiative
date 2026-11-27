package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.TurnHoldScenarios;

/** NeoForge registration shim: delegates to the shared {@link TurnHoldScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class TurnHoldGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "turnHold")
    public void aHeldFightWaitsForTheHit(GameTestHelper helper) {
        TurnHoldScenarios.aHeldFightWaitsForTheHit(helper);
    }
}
