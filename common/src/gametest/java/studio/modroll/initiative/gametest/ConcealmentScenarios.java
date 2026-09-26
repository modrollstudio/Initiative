package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackResult;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.AttackAttempt;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.action.Concealment;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;

/**
 * Hidden participants are suppressed from mob targeting. The mobs here have their AI off and the
 * scenarios call {@code setTarget} themselves: that call is the funnel every targeting goal ends in
 * and the one both loaders hook, so driving it directly tests the gate without depending on a mob
 * pathing or seeing anything — neither of which is reliable in a plot millions of blocks out.
 */
public final class ConcealmentScenarios {

    private static final EncounterConfig TIGHT_BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, 1.0, 8.0);
    private static final EncounterConfig PROVOKED = new EncounterConfig(true, true, true, true, true, true, 6.0, 30.0);
    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, true, true, 3.0, 1.0, 0, 0,
            true, 8.0, true, 1.0, 0, 0);
    private static final ActionConfig SUPPRESSION_OFF = new ActionConfig(
            true, 6.0, true, 4.0, true, true, true, true, true, true, true, true, 3.0, 0, 0, false, true, 3.0, 1.0, 0,
            0, true, 8.0, true, 1.0, 0, 0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final List<Integer> HUSK_FIRST = List.of(1, 20);
    private static final List<Integer> HIDER_ALLY_HUSK = List.of(20, 1, 19);

    /** One tick for the encounter reconcile to mirror the hidden flag, one for margin. */
    private static final int RECONCILE_TICKS = 2;

    private ConcealmentScenarios() {}

    public static void aHiddenParticipantIsNotNewlyTargeted(GameTestHelper helper) {
        prepare(helper, TIGHT_BUBBLE, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        encounterOf(helper, player).flags().setHidden(player.getUUID());
        helper.startSequence()
                .thenExecuteAfter(RECONCILE_TICKS, () -> {
                    requireConcealed(helper, player);
                    husk.setTarget(player);
                    requireNoTarget(helper, husk, "a mob must not acquire a hidden participant");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /**
     * Refusing the assignment is not enough on its own: the mob keeps whatever it had, and its
     * targeting goal re-asserts that remembered target every tick.
     */
    public static void hidingTakesAwayATargetTheMobAlreadyHeld(GameTestHelper helper) {
        prepare(helper, TIGHT_BUBBLE, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        husk.setTarget(player);
        if (!player.equals(husk.getTarget())) {
            helper.fail("the husk must hold the player as its target before the hide is worth testing");
        }
        encounter.flags().setHidden(player.getUUID());
        helper.startSequence()
                .thenExecuteAfter(RECONCILE_TICKS, () -> {
                    requireNoTarget(helper, husk, "hiding must take away a target the mob already held");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void breakingHiddenLetsTheMobTargetAgain(GameTestHelper helper) {
        prepare(helper, TIGHT_BUBBLE, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        encounter.flags().setHidden(player.getUUID());
        helper.startSequence()
                .thenExecuteAfter(RECONCILE_TICKS, () -> {
                    husk.setTarget(player);
                    requireNoTarget(helper, husk, "the hider must be untargetable before the break");
                    ScenarioSupport.forceD20Faces(5, 15); // hidden grants advantage: two faces
                    try {
                        AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), player, husk);
                        ScenarioSupport.requireAttack(
                                helper, attempt.status(), AttackStatus.PERFORMED, "the hider's own attack");
                    } finally {
                        RollService.resetRoller();
                    }
                    if (encounter.flags().isHidden(player.getUUID())) {
                        helper.fail("attacking must break the hidden state");
                    }
                })
                .thenExecuteAfter(RECONCILE_TICKS, () -> {
                    if (Concealment.isConcealed(player.getUUID())) {
                        helper.fail("a broken hidden state must reveal the participant on the next reconcile");
                    }
                    husk.setTarget(player);
                    if (!player.equals(husk.getTarget())) {
                        helper.fail("a revealed participant must be targetable again");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** Leaving the bubble and the encounter ending are separate exits; neither may leak. */
    public static void everyExitPathRevealsTheHider(GameTestHelper helper) {
        prepare(helper, TIGHT_BUBBLE, ACTIONS);
        Player hider = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Player ally = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formFocusFire(helper, HIDER_ALLY_HUSK, hider, ally, husk);
        Encounter encounter = encounterOf(helper, hider);
        encounter.flags().setHidden(hider.getUUID());
        encounter.flags().setHidden(ally.getUUID());
        Vec3 outside = helper.absoluteVec(new Vec3(1, 1, 12));
        helper.startSequence()
                .thenExecuteAfter(RECONCILE_TICKS, () -> {
                    requireConcealed(helper, hider);
                    requireConcealed(helper, ally);
                    hider.teleportTo(outside.x, outside.y, outside.z);
                })
                .thenExecuteAfter(RECONCILE_TICKS, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), hider.getUUID())
                            .isPresent()) {
                        helper.fail("the hider must leave the encounter beyond the leave radius");
                    }
                    if (Concealment.isConcealed(hider.getUUID())) {
                        helper.fail("leaving the encounter must reveal the hider");
                    }
                    husk.setTarget(hider);
                    if (!hider.equals(husk.getTarget())) {
                        helper.fail("a participant that left must be targetable again");
                    }
                    ally.discard();
                })
                .thenExecuteAfter(RECONCILE_TICKS, () -> {
                    Set<?> leaked = Concealment.concealedUuids();
                    if (!leaked.isEmpty()) {
                        helper.fail("no concealed UUID may remain once all encounters end, but found " + leaked);
                    }
                    cleanUp(hider, husk);
                })
                .thenSucceed();
    }

    /** Off is the pre-branch behaviour exactly: targeting untouched, the roll effects still there. */
    public static void theToggleOffLeavesTargetingAlone(GameTestHelper helper) {
        prepare(helper, TIGHT_BUBBLE, SUPPRESSION_OFF);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, HUSK_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        encounter.flags().setHidden(player.getUUID());
        helper.startSequence()
                .thenExecuteAfter(RECONCILE_TICKS, () -> {
                    Set<?> concealed = Concealment.concealedUuids();
                    if (!concealed.isEmpty()) {
                        helper.fail("nothing may be concealed while the toggle is off, but found " + concealed);
                    }
                    husk.setTarget(player);
                    if (!player.equals(husk.getTarget())) {
                        helper.fail("with the toggle off a mob must still target a hidden participant");
                    }
                    ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(15, 5);
                    try {
                        AttackAttempt attempt = ActionEconomy.attack(helper.getLevel(), husk, player);
                        ScenarioSupport.requireAttack(
                                helper, attempt.status(), AttackStatus.PERFORMED, "an attack on a hidden player");
                        AttackResult result = attempt.result().orElseThrow();
                        if (dice.d20Draws != 2 || result.natural() != 5) {
                            helper.fail("the toggle must leave the hidden disadvantage alone but drew " + dice.d20Draws
                                    + " d20s keeping " + result.natural());
                        }
                    } finally {
                        RollService.resetRoller();
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /**
     * A neutral is in the fight only while it is hostile to someone in it, and concealment is what
     * takes its target away — so it must not be read as the mob calming down and let out of the
     * fight the hider is still in. Once the hider is seen again the ordinary leave path resumes.
     */
    public static void aHiddenPlayerHoldsAProvokedNeutralInTheFight(GameTestHelper helper) {
        prepare(helper, PROVOKED, ACTIONS);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Panda panda = helper.spawn(EntityType.PANDA, new BlockPos(3, 1, 3));
        panda.setNoAi(true);
        ScenarioSupport.forceD20Faces(20, 1);
        panda.setTarget(player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    RollService.resetRoller();
                    encounterOf(helper, panda).flags().setHidden(player.getUUID());
                })
                .thenExecuteAfter(RECONCILE_TICKS + 1, () -> {
                    requireNoTarget(helper, panda, "hiding must take the panda's target away");
                    if (EncounterManager.encounterContaining(helper.getLevel(), panda.getUUID())
                            .isEmpty()) {
                        helper.fail("a neutral whose target concealment took must stay in the fight");
                    }
                    encounterOf(helper, panda).flags().breakHidden(player.getUUID());
                })
                .thenExecuteAfter(RECONCILE_TICKS + 1, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), panda.getUUID())
                            .isPresent()) {
                        helper.fail("with the hider seen again the targetless neutral must leave as it always did");
                    }
                    if (Concealment.isConcealed(player.getUUID())) {
                        helper.fail("the hider must be revealed once its hidden state broke");
                    }
                    cleanUp(player, panda);
                })
                .thenSucceed();
    }

    private static void requireConcealed(GameTestHelper helper, Player player) {
        if (!Concealment.isConcealed(player.getUUID())) {
            helper.fail("the reconcile must conceal a hidden participant");
        }
    }

    private static void requireNoTarget(GameTestHelper helper, Mob mob, String what) {
        if (mob.getTarget() != null) {
            helper.fail(what + ", but it targeted " + mob.getTarget());
        }
    }

    private static Encounter encounterOf(GameTestHelper helper, LivingEntity entity) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), entity.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before the scenario can continue");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, EncounterConfig encounters, ActionConfig actions) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(encounters);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(actions);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
