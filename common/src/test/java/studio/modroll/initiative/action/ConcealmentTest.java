package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.Encounter;

class ConcealmentTest {

    private static final ActionConfig SUPPRESSING = actions(true, true);
    private static final ActionConfig TOGGLE_OFF = actions(true, false);
    private static final ActionConfig HIDE_OFF = actions(false, true);

    private final UUID hider = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @AfterEach
    void reset() {
        Concealment.clear();
        InitiativeConfig.overrideActionsForTesting(null);
    }

    @Test
    void concealMarksOnlyThatParticipant() {
        Concealment.conceal(hider);
        assertTrue(Concealment.isConcealed(hider));
        assertFalse(Concealment.isConcealed(other));
    }

    @Test
    void revealRemovesTheMark() {
        Concealment.conceal(hider);
        Concealment.reveal(hider);
        assertFalse(Concealment.isConcealed(hider));
        assertEquals(Set.of(), Concealment.concealedUuids());
    }

    @Test
    void revealingAnUnknownUuidIsANoOp() {
        Concealment.reveal(hider);
        assertEquals(Set.of(), Concealment.concealedUuids());
    }

    @Test
    void clearEmptiesEverything() {
        Concealment.conceal(hider);
        Concealment.conceal(other);
        Concealment.clear();
        assertEquals(Set.of(), Concealment.concealedUuids());
    }

    @Test
    void theGateRefusesAConcealedTarget() {
        InitiativeConfig.overrideActionsForTesting(SUPPRESSING);
        Concealment.conceal(hider);
        assertTrue(Concealment.suppressesTarget(hider));
        assertFalse(Concealment.suppressesTarget(other));
    }

    /** Off must restore ordinary targeting on the next call, not on the next reconcile. */
    @Test
    void theGateStandsDownWithTheToggleOff() {
        Concealment.conceal(hider);
        InitiativeConfig.overrideActionsForTesting(TOGGLE_OFF);
        assertFalse(Concealment.suppressesTarget(hider));
        InitiativeConfig.overrideActionsForTesting(SUPPRESSING);
        assertTrue(Concealment.suppressesTarget(hider));
    }

    @Test
    void theGateStandsDownWithHideItselfOff() {
        InitiativeConfig.overrideActionsForTesting(HIDE_OFF);
        Concealment.conceal(hider);
        assertFalse(Concealment.suppressesTarget(hider));
    }

    @Test
    void aHiderIsHiddenOnlyWhileTargetingIsSuppressed() {
        Encounter encounter = fight();
        encounter.flags().setHidden(hider);
        InitiativeConfig.overrideActionsForTesting(SUPPRESSING);
        assertTrue(Concealment.hides(encounter, hider));
        assertFalse(Concealment.hides(encounter, other));
        InitiativeConfig.overrideActionsForTesting(TOGGLE_OFF);
        assertFalse(Concealment.hides(encounter, hider));
    }

    @Test
    void theFightIsLostOnlyWhenEveryPlayerHides() {
        InitiativeConfig.overrideActionsForTesting(SUPPRESSING);
        Encounter encounter = fight();
        UUID secondPlayer = UUID.randomUUID();
        encounter.add(secondPlayer, Encounter.Side.PLAYER, 0L);
        encounter.flags().setHidden(hider);
        assertFalse(Concealment.everyPlayerHidden(encounter));
        encounter.flags().setHidden(secondPlayer);
        assertTrue(Concealment.everyPlayerHidden(encounter));
    }

    /** A hiding mob is not what ends a fight: only the players' side going unseen does. */
    @Test
    void aHidingMobDoesNotCount() {
        InitiativeConfig.overrideActionsForTesting(SUPPRESSING);
        Encounter encounter = fight();
        encounter.flags().setHidden(other);
        assertFalse(Concealment.everyPlayerHidden(encounter));
    }

    @Test
    void aFightWithNoPlayersIsNotLostToHiding() {
        InitiativeConfig.overrideActionsForTesting(SUPPRESSING);
        Encounter encounter = new Encounter(Vec3.ZERO);
        encounter.add(other, Encounter.Side.HOSTILE, 0L);
        assertFalse(Concealment.everyPlayerHidden(encounter));
    }

    /** The hider is a player and {@code other} a hostile, the shape of every fight here. */
    private Encounter fight() {
        Encounter encounter = new Encounter(Vec3.ZERO);
        encounter.add(hider, Encounter.Side.PLAYER, 0L);
        encounter.add(other, Encounter.Side.HOSTILE, 0L);
        return encounter;
    }

    /**
     * The gate is read from any mob's targeting decision server-wide while the encounter reconcile
     * writes, the same exposure {@code AiFreeze} guards against.
     */
    @Test
    void readsAreSafeWhileTheSetIsBeingMutated() throws InterruptedException {
        InitiativeConfig.overrideActionsForTesting(SUPPRESSING);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 100_000 && done.getCount() > 0; i++) {
                UUID id = UUID.randomUUID();
                Concealment.conceal(id);
                Concealment.reveal(id);
            }
            done.countDown();
        });
        Thread reader = new Thread(() -> {
            try {
                while (done.getCount() > 0) {
                    Concealment.concealedUuids();
                    Concealment.suppressesTarget(hider);
                }
            } catch (Throwable t) {
                failure.set(t);
                done.countDown();
            }
        });
        writer.start();
        reader.start();
        writer.join();
        reader.join();
        assertNull(failure.get());
    }

    private static ActionConfig actions(boolean hide, boolean suppressesTargeting) {
        return new ActionConfig(
                true,
                6.0,
                true,
                true,
                true,
                4.0,
                true,
                true,
                true,
                true,
                true,
                true,
                hide,
                true,
                3.0,
                0,
                0,
                suppressesTargeting,
                true,
                3.0,
                1.0,
                0,
                0,
                true,
                8.0,
                true,
                1.0,
                0,
                0,
                true,
                24.0,
                64.0);
    }
}
