package studio.modroll.initiative.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.gametest.TeleportScenarios;

/** NeoForge registration shim: delegates to the shared {@link TeleportScenarios} bodies. */
@GameTestHolder(Initiative.MOD_ID)
@PrefixGameTestTemplate(false)
public class TeleportGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, batch = "teleportFar")
    public void aPearlPastYourMovementIsRefused(GameTestHelper helper) {
        TeleportScenarios.aPearlPastYourMovementIsRefused(helper);
    }

    @GameTest(template = TEMPLATE, batch = "teleportNear")
    public void aPearlInsideYourMovementCostsTheDistance(GameTestHelper helper) {
        TeleportScenarios.aPearlInsideYourMovementCostsTheDistance(helper);
    }

    @GameTest(template = TEMPLATE, batch = "teleportOffTurn")
    public void aTeleportOffYourTurnIsRefused(GameTestHelper helper) {
        TeleportScenarios.aTeleportOffYourTurnIsRefused(helper);
    }

    @GameTest(template = TEMPLATE, batch = "teleportToggle")
    public void theToggleOffLetsATeleportThrough(GameTestHelper helper) {
        TeleportScenarios.theToggleOffLetsATeleportThrough(helper);
    }
}
