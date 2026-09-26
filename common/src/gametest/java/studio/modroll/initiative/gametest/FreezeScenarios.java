package studio.modroll.initiative.gametest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.turn.AiFreeze;
import studio.modroll.initiative.turn.InitiativeEntry;
import studio.modroll.initiative.turn.TurnOrder;

/**
 * M2b freeze/thaw scenarios. Trigger radius 1 keeps joins explicit-attack-only (deterministic
 * scripted d20 order: attacker first, then victims in hurt order); husks here have their AI ON —
 * the point is watching real AI stop and resume. The player always rolls a natural 20, so the
 * first turn is the player's and every husk starts frozen. One batch per scenario, like M1/M2a.
 */
public final class FreezeScenarios {

    private static final EncounterConfig TIGHT_BUBBLE = new EncounterConfig(true, true, true, false, true, 1.0, 8.0);
    private static final TurnConfig FREEZE_ON =
            new TurnConfig(true, 1000, 1000, 20.0, 12, true, false, false, Set.of());

    private FreezeScenarios() {}

    /**
     * "Acting" is observed through the AI-step machinery itself: a look order arms two ticks of
     * head rotation and a walk order arms the navigation, and both only take effect inside
     * {@code serverAiStep} (look control and navigation tick there) — exactly the code M2b gates.
     * A frozen mob must ignore both orders; the same mob must turn its head on its own turn.
     * Head rotation (not walking) is the thaw signal because gametest plots sit millions of
     * blocks out, where float precision breaks vanilla pathfinding — angle math is unaffected.
     */
    public static void frozenMobHoldsStillAndActsOnItsTurn(GameTestHelper helper) {
        prepare(helper, FREEZE_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHuskWithAi(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        requireCurrent(helper, player, player);
        Vec3[] baseline = new Vec3[1];
        float[] headYaw = new float[1];
        helper.startSequence()
                // The triggering hit knocks the husk into a short airborne arc; the hold-still
                // baseline is only meaningful once that momentum has fully settled.
                .thenWaitUntil(() -> {
                    if (!husk.onGround() || husk.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                        helper.fail("waiting for the knockback of the triggering hit to settle");
                    }
                })
                .thenExecuteAfter(2, () -> {
                    requireFrozen(helper, husk, "a mob off its turn");
                    baseline[0] = husk.position();
                    headYaw[0] = husk.getYHeadRot();
                    orderAboutFace(husk);
                })
                .thenExecuteAfter(30, () -> {
                    double drift = husk.position().distanceTo(baseline[0]);
                    if (drift > 0.05) {
                        helper.fail("a frozen mob must not move under its own power but drifted " + drift + " blocks");
                    }
                    float turned = Math.abs(Mth.degreesDifference(headYaw[0], husk.getYHeadRot()));
                    if (turned > 2.0f) {
                        helper.fail("a frozen mob must ignore look orders but turned " + turned + " degrees");
                    }
                    turnOrder(helper, player).endTurn();
                })
                .thenExecuteAfter(2, () -> {
                    if (AiFreeze.isFrozen(husk.getUUID())) {
                        helper.fail("a mob must thaw on its own turn");
                    }
                    headYaw[0] = husk.getYHeadRot();
                    orderAboutFace(husk);
                })
                // Sampled two ticks after the order: the look plays out for exactly two armed
                // ticks and then vanilla re-aligns the head to the body, so a later sample would
                // see the head back at its starting angle.
                .thenExecuteAfter(2, () -> {
                    float turned = Math.abs(Mth.degreesDifference(headYaw[0], husk.getYHeadRot()));
                    if (turned < 8.0f) {
                        helper.fail(
                                "a thawed mob must act on its turn but its head only turned " + turned + " degrees");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    /** Orders a look (and a walk, for the frozen phase) directly behind the mob's current heading. */
    private static void orderAboutFace(Husk husk) {
        double yaw = Math.toRadians(husk.getYHeadRot());
        Vec3 behind = husk.position().add(Math.sin(yaw) * 10, husk.getEyeHeight(), -Math.cos(yaw) * 10);
        husk.getLookControl().setLookAt(behind.x, behind.y, behind.z);
        husk.getNavigation().moveTo(behind.x, husk.getY(), behind.z, 1.0);
    }

    public static void everyExitPathThawsFrozenState(GameTestHelper helper) {
        prepare(helper, FREEZE_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk dies = ScenarioSupport.spawnHuskWithAi(helper, 2, 2);
        Husk flees = ScenarioSupport.spawnHuskWithAi(helper, 4, 2);
        Husk vanishes = ScenarioSupport.spawnHuskWithAi(helper, 2, 4);
        Husk survivor = ScenarioSupport.spawnHuskWithAi(helper, 4, 4);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1, 1, 1, 1), dies, flees, vanishes, survivor);
        Vec3 outside = helper.absoluteVec(new Vec3(1, 1, 12));
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireFrozen(helper, dies, "every husk after formation");
                    requireFrozen(helper, flees, "every husk after formation");
                    requireFrozen(helper, vanishes, "every husk after formation");
                    requireFrozen(helper, survivor, "every husk after formation");
                    dies.kill();
                })
                .thenExecuteAfter(2, () -> {
                    requireThawed(helper, dies, "death");
                    flees.teleportTo(outside.x, outside.y, outside.z);
                })
                .thenExecuteAfter(2, () -> {
                    requireThawed(helper, flees, "fleeing beyond the leave radius");
                    vanishes.discard();
                })
                .thenExecuteAfter(2, () -> {
                    requireThawed(helper, vanishes, "removal from the world (unload/dimension change)");
                    requireFrozen(helper, survivor, "a husk still in the running encounter");
                    player.discard();
                })
                .thenExecuteAfter(2, () -> {
                    if (EncounterManager.encounterContaining(helper.getLevel(), survivor.getUUID())
                            .isPresent()) {
                        helper.fail("the encounter must end once no player remains");
                    }
                    requireThawed(helper, survivor, "encounter end");
                    Set<UUID> leaked = AiFreeze.frozenUuids();
                    if (!leaked.isEmpty()) {
                        helper.fail("no frozen UUID may remain after all encounters end, but found " + leaked);
                    }
                    cleanUp(flees, survivor);
                })
                .thenSucceed();
    }

    public static void frozenMobStillTakesDamage(GameTestHelper helper) {
        prepare(helper, FREEZE_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHuskWithAi(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        float[] before = new float[1];
        helper.startSequence()
                .thenExecuteAfter(15, () -> {
                    requireFrozen(helper, husk, "the husk off its turn");
                    before[0] = husk.getHealth();
                    husk.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0f);
                })
                .thenExecuteAfter(2, () -> {
                    if (husk.getHealth() >= before[0]) {
                        helper.fail("a frozen mob must still take damage normally");
                    }
                    requireFrozen(helper, husk, "the husk after being hit off-turn");
                    if (EncounterManager.encounterContaining(helper.getLevel(), husk.getUUID())
                            .isEmpty()) {
                        helper.fail("being hit must not remove the frozen mob from the encounter");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void excludedTypeIsNeverFrozenButKeepsItsTurnSlot(GameTestHelper helper) {
        prepare(helper, new TurnConfig(true, 1000, 1000, 20.0, 12, true, false, false, Set.of("minecraft:husk")));
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHuskWithAi(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    if (AiFreeze.isFrozen(husk.getUUID())) {
                        helper.fail("an excluded entity type must never be frozen");
                    }
                    requireCurrent(helper, player, player);
                    boolean inOrder = turnOrder(helper, player).order().stream()
                            .map(InitiativeEntry::participant)
                            .anyMatch(husk.getUUID()::equals);
                    if (!inOrder) {
                        helper.fail("an excluded entity must still hold its slot in the turn order");
                    }
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void frozenMobStillTicksStatusEffects(GameTestHelper helper) {
        prepare(helper, FREEZE_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHuskWithAi(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireFrozen(helper, husk, "the husk off its turn");
                    // Fire resistance, not regeneration: undead cannot receive healing effects.
                    husk.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100));
                })
                .thenExecuteAfter(20, () -> {
                    MobEffectInstance effect = husk.getEffect(MobEffects.FIRE_RESISTANCE);
                    if (effect == null) {
                        helper.fail("the potion effect must still be present on the frozen mob");
                    }
                    if (effect.getDuration() >= 100) {
                        helper.fail("a potion effect must keep ticking down on a frozen mob (the entity tick "
                                + "is not skipped) but duration was " + effect.getDuration());
                    }
                    requireFrozen(helper, husk, "the husk through the whole observation");
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    public static void freezeFlagOffThawsAndFreezesNothing(GameTestHelper helper) {
        prepare(helper, FREEZE_ON);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHuskWithAi(helper, 3, 3);
        ScenarioSupport.formEncounter(helper, player, List.of(20, 1), husk);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireFrozen(helper, husk, "the husk while the freeze flag is on");
                    InitiativeConfig.overrideTurnsForTesting(
                            new TurnConfig(true, 1000, 1000, 20.0, 12, false, false, false, Set.of()));
                })
                .thenExecuteAfter(2, () -> {
                    if (AiFreeze.isFrozen(husk.getUUID())) {
                        helper.fail("turning the freeze flag off must thaw every frozen mob");
                    }
                    if (!AiFreeze.frozenUuids().isEmpty()) {
                        helper.fail("no frozen state may linger once the freeze flag is off");
                    }
                    requireCurrent(helper, player, player);
                    cleanUp(player, husk);
                })
                .thenSucceed();
    }

    private static void prepare(GameTestHelper helper, TurnConfig turns) {
        InitiativeConfig.overrideEncountersForTesting(TIGHT_BUBBLE);
        InitiativeConfig.overrideTurnsForTesting(turns);
        InitiativeConfig.overrideActionsForTesting(ScenarioSupport.ACTIONS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        ScenarioSupport.discardLeftoverHostilesNearby(helper);
    }

    private static TurnOrder turnOrder(GameTestHelper helper, Player player) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), player.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail("the encounter must form before its turn order can be inspected");
        }
        return encounter.turnOrder();
    }

    private static void requireCurrent(GameTestHelper helper, Player player, LivingEntity expected) {
        UUID current = turnOrder(helper, player).currentTurn().orElse(null);
        if (!expected.getUUID().equals(current)) {
            helper.fail("the current turn must belong to " + expected.getUUID() + " but was " + current);
        }
    }

    private static void requireFrozen(GameTestHelper helper, LivingEntity entity, String what) {
        if (!AiFreeze.isFrozen(entity.getUUID())) {
            helper.fail(what + " must be frozen");
        }
    }

    private static void requireThawed(GameTestHelper helper, LivingEntity entity, String exitPath) {
        if (AiFreeze.isFrozen(entity.getUUID())) {
            helper.fail(exitPath + " must thaw the mob");
        }
    }

    private static void cleanUp(LivingEntity... entities) {
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
