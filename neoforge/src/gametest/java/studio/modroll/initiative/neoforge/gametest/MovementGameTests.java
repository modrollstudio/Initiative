package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.MovementScenarios;

/** NeoForge registration shim: delegates to the shared {@link MovementScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class MovementGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "movementDefault")
    public void aTurnGivesTenBlocksByDefault(GameTestHelper helper) {
        MovementScenarios.aTurnGivesTenBlocksByDefault(helper);
    }

    @GameTest(template = TEMPLATE, batch = "movementSwiftness")
    public void swiftnessGivesMoreBlocks(GameTestHelper helper) {
        MovementScenarios.swiftnessGivesMoreBlocks(helper);
    }

    @GameTest(template = TEMPLATE, batch = "movementSlowness")
    public void slownessGivesFewerBlocks(GameTestHelper helper) {
        MovementScenarios.slownessGivesFewerBlocks(helper);
    }

    @GameTest(template = TEMPLATE, batch = "movementHeld")
    public void aHeldPlayerStartsItsTurnWithItsFullBudget(GameTestHelper helper) {
        MovementScenarios.aHeldPlayerStartsItsTurnWithItsFullBudget(helper);
    }
}
