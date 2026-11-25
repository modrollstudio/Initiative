package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.MountScenarios;

/** NeoForge registration shim: delegates to the shared {@link MountScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class MountGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "mountRiderAndMountShareOneTurn", timeoutTicks = 400)
    public void riderAndMountShareOneTurn(GameTestHelper helper) {
        MountScenarios.riderAndMountShareOneTurn(helper);
    }

    @GameTest(template = TEMPLATE, batch = "mountWithTheToggleOffTheMountTakesItsOwnTurn", timeoutTicks = 400)
    public void withTheToggleOffTheMountTakesItsOwnTurn(GameTestHelper helper) {
        MountScenarios.withTheToggleOffTheMountTakesItsOwnTurn(helper);
    }
}
