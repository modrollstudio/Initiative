package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EncounterFlagsTest {

    @Test
    void freshFlagsAreAllClear() {
        EncounterFlags flags = new EncounterFlags();
        UUID p = UUID.randomUUID();
        assertFalse(flags.isDodging(p));
        assertFalse(flags.isDisengaged(p));
        assertFalse(flags.hasHelpAdvantage(p));
    }

    @Test
    void dodgingPersistsUntilTheDodgersOwnTurnStarts() {
        EncounterFlags flags = new EncounterFlags();
        UUID dodger = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        flags.setDodging(dodger);
        assertTrue(flags.isDodging(dodger));
        flags.beginTurn(other);
        assertTrue(flags.isDodging(dodger));
        flags.beginTurn(dodger);
        assertFalse(flags.isDodging(dodger));
    }

    @Test
    void disengagedPersistsUntilTheParticipantsOwnTurnStarts() {
        EncounterFlags flags = new EncounterFlags();
        UUID p = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        flags.setDisengaged(p);
        assertTrue(flags.isDisengaged(p));
        flags.beginTurn(other);
        assertTrue(flags.isDisengaged(p));
        flags.beginTurn(p);
        assertFalse(flags.isDisengaged(p));
    }

    @Test
    void helpAdvantageIsConsumedOnce() {
        EncounterFlags flags = new EncounterFlags();
        UUID ally = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        flags.grantHelpAdvantage(ally, helper);
        assertTrue(flags.hasHelpAdvantage(ally));
        assertTrue(flags.consumeHelpAdvantage(ally));
        assertFalse(flags.hasHelpAdvantage(ally));
        assertFalse(flags.consumeHelpAdvantage(ally));
    }

    @Test
    void helpAdvantageExpiresAtTheHelpersNextTurnButNotTheAllys() {
        EncounterFlags flags = new EncounterFlags();
        UUID ally = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        flags.grantHelpAdvantage(ally, helper);
        flags.beginTurn(ally);
        assertTrue(flags.hasHelpAdvantage(ally));
        flags.beginTurn(helper);
        assertFalse(flags.hasHelpAdvantage(ally));
    }

    @Test
    void removePurgesTheParticipantAsDodgerAndAsBeneficiary() {
        EncounterFlags flags = new EncounterFlags();
        UUID p = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        flags.setDodging(p);
        flags.setDisengaged(p);
        flags.grantHelpAdvantage(p, helper);
        flags.remove(p);
        assertFalse(flags.isDodging(p));
        assertFalse(flags.isDisengaged(p));
        assertFalse(flags.hasHelpAdvantage(p));
    }

    @Test
    void removePurgesGrantsWhereTheParticipantWasTheHelper() {
        EncounterFlags flags = new EncounterFlags();
        UUID ally = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        flags.grantHelpAdvantage(ally, helper);
        flags.remove(helper);
        assertFalse(flags.hasHelpAdvantage(ally));
    }

    @Test
    void everyoneStartsWithTheirReaction() {
        EncounterFlags flags = new EncounterFlags();
        assertTrue(flags.hasReaction(UUID.randomUUID()));
    }

    @Test
    void useReactionSpendsItUntilTheOwnersNextTurn() {
        EncounterFlags flags = new EncounterFlags();
        UUID reactor = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        flags.useReaction(reactor);
        assertFalse(flags.hasReaction(reactor));
        flags.beginTurn(other);
        assertFalse(flags.hasReaction(reactor));
        flags.beginTurn(reactor);
        assertTrue(flags.hasReaction(reactor));
    }

    @Test
    void removeRestoresAndClearsTheReactionState() {
        EncounterFlags flags = new EncounterFlags();
        UUID reactor = UUID.randomUUID();
        flags.useReaction(reactor);
        flags.remove(reactor);
        assertTrue(flags.hasReaction(reactor));
    }

    @Test
    void hiddenGrantsAdvantageOnceThenBreaks() {
        EncounterFlags flags = new EncounterFlags();
        UUID hider = UUID.randomUUID();
        flags.setHidden(hider);
        assertTrue(flags.isHidden(hider));
        assertTrue(flags.consumeHidden(hider));
        assertFalse(flags.isHidden(hider));
        assertFalse(flags.consumeHidden(hider));
    }

    @Test
    void hiddenPersistsAcrossTurnsAndClearsOnBreakOrRemoval() {
        EncounterFlags flags = new EncounterFlags();
        UUID hider = UUID.randomUUID();
        flags.setHidden(hider);
        flags.beginTurn(UUID.randomUUID());
        assertTrue(flags.isHidden(hider));
        flags.beginTurn(hider);
        assertTrue(flags.isHidden(hider));
        flags.breakHidden(hider);
        assertFalse(flags.isHidden(hider));
        flags.setHidden(hider);
        flags.remove(hider);
        assertFalse(flags.isHidden(hider));
    }

    @Test
    void aStealthHideKeepsItsTotalWhileHidden() {
        EncounterFlags flags = new EncounterFlags();
        UUID hider = UUID.randomUUID();
        flags.setHidden(hider, 17);
        assertTrue(flags.isHidden(hider));
        assertEquals(Map.of(hider, 17), flags.hiddenStealth());
        flags.beginTurn(hider);
        assertEquals(Map.of(hider, 17), flags.hiddenStealth());
    }

    @Test
    void everyWayOutOfHidingDropsTheStealthTotal() {
        EncounterFlags flags = new EncounterFlags();
        UUID hider = UUID.randomUUID();
        flags.setHidden(hider, 17);
        flags.consumeHidden(hider);
        assertTrue(flags.hiddenStealth().isEmpty());
        flags.setHidden(hider, 17);
        flags.breakHidden(hider);
        assertTrue(flags.hiddenStealth().isEmpty());
    }

    /** A flat Hide has no Stealth total, so it must not inherit one from an earlier Checks Hide. */
    @Test
    void aFlatHideReplacesAStoredStealthTotal() {
        EncounterFlags flags = new EncounterFlags();
        UUID hider = UUID.randomUUID();
        flags.setHidden(hider, 17);
        flags.setHidden(hider);
        assertTrue(flags.isHidden(hider));
        assertTrue(flags.hiddenStealth().isEmpty());
    }

    /**
     * The leak check for per-participant state: every flag this class can hold is raised, in both
     * directions where a flag names two participants, and removal must leave nothing behind. A field
     * added to EncounterFlags without a matching line in remove() fails here.
     */
    @Test
    void removeClearsEveryFlagInBothDirections() {
        EncounterFlags flags = new EncounterFlags();
        UUID leaver = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        flags.setDodging(leaver);
        flags.setDisengaged(leaver);
        flags.setHidden(leaver, 17);
        flags.useReaction(leaver);
        flags.grantHelpAdvantage(leaver, other);
        flags.grantHelpAdvantage(other, leaver);
        flags.setGrappled(leaver, other);
        flags.setGrappled(other, leaver);

        flags.remove(leaver);

        assertFalse(flags.isDodging(leaver));
        assertFalse(flags.isDisengaged(leaver));
        assertFalse(flags.isHidden(leaver));
        assertTrue(flags.hiddenStealth().isEmpty());
        assertTrue(flags.hasReaction(leaver));
        assertFalse(flags.hasHelpAdvantage(leaver));
        assertFalse(flags.hasHelpAdvantage(other));
        assertFalse(flags.isGrappled(leaver));
        assertFalse(flags.isGrappled(other));
        assertTrue(flags.grapples().isEmpty());
    }
}
