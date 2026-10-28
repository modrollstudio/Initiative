package studio.modroll.initiative.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.modroll.critfall.api.CombatSuppression;
import studio.modroll.critfall.api.RollService;
import studio.modroll.initiative.action.ActionEconomy;
import studio.modroll.initiative.action.ActionEconomy.AttackStatus;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.ActionUiConfig;
import studio.modroll.initiative.config.EncounterConfig;
import studio.modroll.initiative.config.HudConfig;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.config.RollAnimationConfig;
import studio.modroll.initiative.config.TurnConfig;
import studio.modroll.initiative.encounter.Encounter;
import studio.modroll.initiative.encounter.EncounterManager;
import studio.modroll.initiative.hud.TurnOrderSync;
import studio.modroll.initiative.roll.RollAnimation;
import studio.modroll.initiative.roll.RollAnimationSync;
import studio.modroll.initiative.turn.AiFreeze;
import studio.modroll.initiative.turn.InitiativeEntry;
import studio.modroll.initiative.turn.SkippedTurns;
import studio.modroll.initiative.ui.ActionUiSync;

/**
 * Joining a running fight from outside, and leaving one by changing dimension.
 *
 * <p>An attack on a participant joins its attacker on that very hit, and is its turn for the round:
 * the order passes over its slot later that round with a line to the table, and from the next round
 * it acts as usual. The joining hit is that turn's attack, rolled through Critfall like any other, so
 * it can miss. Each joiner attacks a tick after the fight formed: a hit in the forming tick is part of
 * the fight starting, not an attack from outside it. With checks off, initiative bonuses come from
 * movement speed — player +2, husk and zombie +4, skeleton +5 — so a scripted natural 10 seats every
 * joiner between the player (22) and the husk (5). The joining hit's own d20 is scripted after it: a
 * natural 2, which misses every target here, or a natural 20.
 */
public final class JoinScenarios {

    private static final EncounterConfig BUBBLE =
            new EncounterConfig(true, true, true, false, true, true, true, true, 1.0, 30.0);
    /** The player stands inside the leave radius; the attackers below stand past it. */
    private static final EncounterConfig NEAR_EDGE =
            new EncounterConfig(true, true, true, false, true, true, true, true, 1.0, 3.0);

    private static final TurnConfig TURNS = new TurnConfig(true, 1000, 1000, 20.0, 12, true, false, false, Set.of());
    private static final ActionConfig ACTIONS = new ActionConfig(
            true, 6.0, true, true, true, 4.0, false, true, true, true, true, true, true, true, 3.0, 0, 0, true, true,
            3.0, 1.0, 0, 0, true, 8.0, true, 1.0, 0, 0, true, 24.0, 64.0);

    private static final List<Integer> PLAYER_FIRST = List.of(20, 1);
    private static final int JOINER_MIDDLE = 10;
    private static final int MISS = 2;
    private static final int HIT = 20;
    /** Focus-fire join order is first attacker, target, second attacker — so the first player acts. */
    private static final List<Integer> ALLY_LAST = List.of(20, 5, 1);

    private static final int TURN_TICKS = 10;

    private static final NoticeSender NOTICES = new NoticeSender();
    private static final MultiplayerScenarios.RollSender ROLLS = new MultiplayerScenarios.RollSender();
    private static final RollAnimationConfig SHOWN = new RollAnimationConfig(true, 10, 10, 2, true, true);

    private JoinScenarios() {}

    static final class NoticeSender implements SkippedTurns.Sender {
        record Sent(UUID player, Component line) {}

        final List<Sent> sent = new ArrayList<>();

        @Override
        public void send(Player player, Component line) {
            sent.add(new Sent(player.getUUID(), line));
        }
    }

    public static void aZombieJoinsOnItsHitAndHitsNoMore(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Zombie zombie = spawnHostile(helper, EntityType.ZOMBIE, 0, 1, player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    opening(MISS, () -> melee(helper, zombie, player));
                    requireJoinedOnTheHit(helper, encounter, player, zombie, husk);
                })
                .thenExecuteAfter(1, () -> {
                    requireHeldOffTurn(helper, zombie);
                    float before = player.getHealth();
                    melee(helper, zombie, player);
                    if (player.getHealth() < before) {
                        helper.fail("a zombie that joined on its hit must land no further real-time hit");
                    }
                })
                .thenExecute(() -> requireSkippedThisRound(helper, encounter, player, zombie, husk))
                .thenExecuteAfter(1, () -> requireNoticeNaming(helper, player, zombie))
                .thenExecute(() -> startJoinersNextTurn(helper, encounter, player, husk, zombie))
                .thenExecuteAfter(TURN_TICKS, () -> requireMobActedOnItsTurn(helper, encounter, husk))
                .thenExecute(() -> cleanUp(player, husk, zombie))
                .thenSucceed();
    }

    public static void aPlayerJoinsOnTheirHitAndHitsNoMore(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Player outsider = ScenarioSupport.spawnPlayer(helper, 0, 2);
        ServerLevel level = helper.getLevel();
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    if (!ActionEconomy.leavesToVanilla(
                            ActionEconomy.attack(level, outsider, husk).status())) {
                        helper.fail("an outsider's first swing must be left to vanilla, which is what joins them");
                    }
                    opening(MISS, () -> swing(helper, outsider, husk));
                    requireJoinedOnTheHit(helper, encounter, player, outsider, husk);
                })
                .thenExecuteAfter(1, () -> {
                    AttackStatus second =
                            ActionEconomy.attack(level, outsider, husk).status();
                    if (ActionEconomy.leavesToVanilla(second)) {
                        helper.fail("a player who joined on their hit must land no further real-time hit");
                    }
                    ScenarioSupport.requireAttack(
                            helper, second, AttackStatus.NOT_YOUR_TURN, "the joiner's second swing this round");
                })
                .thenExecute(() -> requireSkippedThisRound(helper, encounter, player, outsider, husk))
                .thenExecuteAfter(1, () -> requireNoticeNaming(helper, player, outsider))
                .thenExecute(() -> startJoinersNextTurn(helper, encounter, player, husk, outsider))
                .thenExecute(() -> {
                    ScenarioSupport.forceCountedDice(true);
                    try {
                        ScenarioSupport.requireAttack(
                                helper,
                                ActionEconomy.attack(level, outsider, husk).status(),
                                AttackStatus.PERFORMED,
                                "the joiner's attack on its turn the round after");
                    } finally {
                        RollService.resetRoller();
                    }
                    cleanUp(player, husk, outsider);
                })
                .thenSucceed();
    }

    public static void aSkeletonJoinsOnItsShotAndShootsNoMore(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Skeleton skeleton = spawnHostile(helper, EntityType.SKELETON, 4, 4, player);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    opening(MISS, () -> shoot(helper, skeleton, player));
                    requireJoinedOnTheHit(helper, encounter, player, skeleton, husk);
                })
                .thenExecuteAfter(1, () -> {
                    requireHeldOffTurn(helper, skeleton);
                    float before = player.getHealth();
                    shoot(helper, skeleton, player);
                    if (player.getHealth() < before) {
                        helper.fail("a skeleton that joined on its shot must land no further real-time arrow");
                    }
                })
                .thenExecute(() -> requireSkippedThisRound(helper, encounter, player, skeleton, husk))
                .thenExecuteAfter(1, () -> requireNoticeNaming(helper, player, skeleton))
                .thenExecute(() -> startJoinersNextTurn(helper, encounter, player, husk, skeleton))
                .thenExecuteAfter(TURN_TICKS, () -> requireMobActedOnItsTurn(helper, encounter, husk))
                .thenExecute(() -> cleanUp(player, husk, skeleton))
                .thenSucceed();
    }

    /**
     * The cause of the extra real-time hits: an attacker standing past the leave radius was dropped
     * by the sweep on the very tick its hit joined it, so its next hit joined it all over again.
     */
    public static void anAttackerFromPastTheLeaveRadiusStaysInTheFight(GameTestHelper helper) {
        prepare(helper, NEAR_EDGE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Zombie zombie = spawnHostile(helper, EntityType.ZOMBIE, 1, 5, player);
        Player outsider = ScenarioSupport.spawnPlayer(helper, 1, 6);
        if (encounter.isWithin(zombie.position(), 3.0) || encounter.isWithin(outsider.position(), 3.0)) {
            helper.fail("the scenario needs both attackers past the leave radius");
        }
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    opening(MISS, () -> melee(helper, zombie, player));
                    opening(MISS, () -> swing(helper, outsider, husk));
                })
                .thenExecuteAfter(3, () -> {
                    if (!encounter.contains(zombie.getUUID())) {
                        helper.fail("a mob that attacked its way in must not be swept out for where it stands");
                    }
                    if (!encounter.contains(outsider.getUUID())) {
                        helper.fail("a player who attacked their way in must not be swept out for where they stand");
                    }
                    cleanUp(player, husk, zombie, outsider);
                })
                .thenSucceed();
    }

    /** The opening swing is the zombie's turn attack: rolled through Critfall, and a miss deals nothing. */
    public static void aZombiesOpeningSwingRollsAndCanMiss(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Zombie zombie = spawnHostile(helper, EntityType.ZOMBIE, 0, 1, player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    requireRolledMiss(helper, encounter, zombie, player, () -> melee(helper, zombie, player));
                    cleanUp(player, husk, zombie);
                })
                .thenSucceed();
    }

    public static void aSkeletonsOpeningArrowRollsAndCanMiss(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Skeleton skeleton = spawnHostile(helper, EntityType.SKELETON, 4, 4, player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    requireRolledMiss(helper, encounter, skeleton, player, () -> shoot(helper, skeleton, player));
                    cleanUp(player, husk, skeleton);
                })
                .thenSucceed();
    }

    public static void aPlayersOpeningSwingRollsAndCanMiss(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Player outsider = ScenarioSupport.spawnPlayer(helper, 0, 2);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    requireRolledMiss(helper, encounter, outsider, husk, () -> swing(helper, outsider, husk));
                    cleanUp(player, husk, outsider);
                })
                .thenSucceed();
    }

    public static void aPlayersOpeningArrowRollsAndCanMiss(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        // Past the husk's melee reach: a ranged attack with a hostile in reach rolls with disadvantage.
        Player outsider = ScenarioSupport.spawnPlayer(helper, 0, 8);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    requireRolledMiss(helper, encounter, outsider, husk, () -> shoot(helper, outsider, husk));
                    cleanUp(player, husk, outsider);
                })
                .thenSucceed();
    }

    /**
     * The loaders put Critfall's damage listener ahead of Initiative's, so a real hurt joins through
     * Critfall's combat interaction and Initiative only rolls. When Initiative's listener runs first
     * instead, it must join the attacker itself and roll the same attack: this calls it first.
     */
    public static void anOpeningAttackRollsWhenInitiativesListenerRunsFirst(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Zombie zombie = spawnHostile(helper, EntityType.ZOMBIE, 0, 1, player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    boolean[] canceled = new boolean[1];
                    requireRolledMiss(
                            helper,
                            encounter,
                            zombie,
                            player,
                            () -> canceled[0] = ActionEconomy.interceptVanillaDamage(
                                    player, helper.getLevel().damageSources().mobAttack(zombie)));
                    if (!canceled[0]) {
                        helper.fail("the vanilla hit must be canceled once it has been rolled");
                    }
                    cleanUp(player, husk, zombie);
                })
                .thenSucceed();
    }

    /** The other side of the same roll: a natural 20 lands Critfall's damage, not the vanilla 1. */
    public static void anOpeningSwingThatHitsDealsRolledDamage(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        Player player = ScenarioSupport.spawnPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, player, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, player);
        Zombie zombie = spawnHostile(helper, EntityType.ZOMBIE, 0, 1, player);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    float before = player.getHealth();
                    ScenarioSupport.ScriptedDice dice = opening(HIT, () -> melee(helper, zombie, player));
                    if (!encounter.contains(zombie.getUUID())) {
                        helper.fail("the opening attack must still join its attacker");
                    }
                    if (dice.d20Draws != 2) {
                        helper.fail("the opening attack must roll a d20 of its own after initiative, but "
                                + dice.d20Draws + " were drawn");
                    }
                    if (before - player.getHealth() <= 1.0f) {
                        helper.fail("a critical opening hit must deal Critfall's rolled damage, but dealt "
                                + (before - player.getHealth()));
                    }
                    cleanUp(player, husk, zombie);
                })
                .thenSucceed();
    }

    /** Leaving by changing dimension is the disconnect exit: at once, turn passed on, bar cleared. */
    public static void aPlayerWhoChangesDimensionLeavesAtOnce(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        MultiplayerScenarios.HudSender hud = new MultiplayerScenarios.HudSender();
        ActionUiScenarios.CapturingSender ui = new ActionUiScenarios.CapturingSender();
        TurnOrderSync.overrideSenderForTesting(hud);
        ActionUiSync.overrideSenderForTesting(ui);
        ServerPlayer traveller = spawnServerPlayer(helper, 1, 1);
        Player stays = ScenarioSupport.spawnPlayer(helper, 1, 2);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formFocusFire(helper, ALLY_LAST, traveller, stays, husk);
        Encounter encounter = encounterOf(helper, traveller);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    requireCurrent(helper, encounter, traveller, "the traveller must hold the turn as it leaves");
                    if (!ui.lastFor(helper, traveller).yourTurn()) {
                        helper.fail("the traveller's bar must be up before it leaves");
                    }
                    toTheNether(helper, traveller);
                    requireGone(helper, encounter, traveller);
                    requireCurrent(helper, encounter, husk, "the turn must pass on from a player who left");
                    requireCleared(helper, hud, ui, traveller);
                })
                .thenExecuteAfter(1, () -> {
                    if (!EncounterManager.encounters(helper.getLevel()).contains(encounter)
                            || !encounter.contains(stays.getUUID())
                            || !encounter.contains(husk.getUUID())) {
                        helper.fail("the fight must carry on for whoever is left");
                    }
                    clearSenders();
                    helper.getLevel().getServer().getPlayerList().remove(traveller);
                    cleanUp(stays, husk);
                })
                .thenSucceed();
    }

    public static void theLastPlayerChangingDimensionEndsTheFight(GameTestHelper helper) {
        prepare(helper, BUBBLE);
        MultiplayerScenarios.HudSender hud = new MultiplayerScenarios.HudSender();
        ActionUiScenarios.CapturingSender ui = new ActionUiScenarios.CapturingSender();
        TurnOrderSync.overrideSenderForTesting(hud);
        ActionUiSync.overrideSenderForTesting(ui);
        ServerPlayer traveller = spawnServerPlayer(helper, 1, 1);
        Husk husk = ScenarioSupport.spawnHusk(helper, 2, 2);
        ScenarioSupport.formEncounter(helper, traveller, PLAYER_FIRST, husk);
        Encounter encounter = encounterOf(helper, traveller);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    toTheNether(helper, traveller);
                    requireGone(helper, encounter, traveller);
                    requireCleared(helper, hud, ui, traveller);
                })
                .thenExecuteAfter(1, () -> {
                    if (EncounterManager.encounters(helper.getLevel()).contains(encounter)) {
                        helper.fail("the fight must end once its last player has left the dimension");
                    }
                    if (RollService.isSuppressed(husk) || AiFreeze.isFrozen(husk.getUUID())) {
                        helper.fail("the ended fight must release its mob");
                    }
                    clearSenders();
                    helper.getLevel().getServer().getPlayerList().remove(traveller);
                    cleanUp(husk);
                })
                .thenSucceed();
    }

    private static void requireJoinedOnTheHit(
            GameTestHelper helper, Encounter encounter, Player player, LivingEntity joiner, Husk husk) {
        if (!encounter.contains(joiner.getUUID())) {
            helper.fail("an attack on a participant must join its attacker on that very hit");
        }
        List<UUID> order = encounter.turnOrder().order().stream()
                .map(InitiativeEntry::participant)
                .toList();
        if (!order.equals(List.of(player.getUUID(), joiner.getUUID(), husk.getUUID()))) {
            helper.fail("the joiner must be seated between the player and the husk, but the order read " + order);
        }
        requireCurrent(helper, encounter, player, "joining must not take the acting player's turn");
    }

    private static void requireHeldOffTurn(GameTestHelper helper, Mob joiner) {
        if (!AiFreeze.isFrozen(joiner.getUUID()) || !RollService.isSuppressed(joiner)) {
            helper.fail("the joiner must be frozen and suppressed off its turn like any participant");
        }
    }

    /** The acting player ends their turn: the joiner's slot, next in the order, is passed over. */
    private static void requireSkippedThisRound(
            GameTestHelper helper, Encounter encounter, Player player, LivingEntity joiner, Husk husk) {
        if (!ActionEconomy.endTurn(helper.getLevel(), player)) {
            helper.fail("the acting player must be able to end their turn");
        }
        requireCurrent(helper, encounter, husk, "the joiner already acted this round, so its slot must be skipped");
        if (encounter.turnOrder().round() != 1) {
            helper.fail("skipping the joiner must not start a new round");
        }
    }

    private static void requireNoticeNaming(GameTestHelper helper, Player player, LivingEntity joiner) {
        for (NoticeSender.Sent sent : NOTICES.sent) {
            if (sent.player().equals(player.getUUID())
                    && sent.line().getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("initiative.turn.already_acted")
                    && contents.getArgs().length == 1
                    && contents.getArgs()[0] instanceof Component name
                    && name.getString().equals(joiner.getDisplayName().getString())) {
                return;
            }
        }
        helper.fail("the table must be told the joiner already acted, but was sent " + NOTICES.sent);
    }

    /** The husk and then the player end their turns: round two reaches the joiner. */
    private static void startJoinersNextTurn(
            GameTestHelper helper, Encounter encounter, Player player, Husk husk, LivingEntity joiner) {
        encounter.turnOrder().endTurn();
        requireCurrent(helper, encounter, player, "round two must open on the player");
        ActionEconomy.endTurn(helper.getLevel(), player);
        requireCurrent(helper, encounter, joiner, "from the next round on the joiner takes its turn");
        if (encounter.turnOrder().round() != 2) {
            helper.fail("the joiner's own turn must come in round two");
        }
        if (husk.isRemoved()) {
            helper.fail("the husk must still be in the fight");
        }
        ScenarioSupport.forceCountedDice(true);
    }

    /** A mob ends its turn once its action is spent, so the turn moving on means it attacked. */
    private static void requireMobActedOnItsTurn(GameTestHelper helper, Encounter encounter, Husk husk) {
        RollService.resetRoller();
        requireCurrent(helper, encounter, husk, "the joiner must attack on its own turn and pass it on");
    }

    private static void requireCurrent(GameTestHelper helper, Encounter encounter, LivingEntity expected, String what) {
        UUID current = encounter.turnOrder().currentTurn().orElse(null);
        if (!expected.getUUID().equals(current)) {
            helper.fail(what + " (current turn: " + current + ", expected " + expected.getUUID() + ")");
        }
    }

    private static void requireGone(GameTestHelper helper, Encounter encounter, Player player) {
        if (encounter.contains(player.getUUID())) {
            helper.fail("a player who changed dimension must leave the encounter at once");
        }
        if (CombatSuppression.suppressedUuids().contains(player.getUUID())) {
            helper.fail("a player who changed dimension must not stay suppressed");
        }
    }

    private static void requireCleared(
            GameTestHelper helper,
            MultiplayerScenarios.HudSender hud,
            ActionUiScenarios.CapturingSender ui,
            Player player) {
        if (!hud.lastFor(helper, player).isEmpty()) {
            helper.fail("the server must send the message that clears the traveller's turn strip");
        }
        if (ui.lastFor(helper, player).yourTurn()) {
            helper.fail("the server must send the message that clears the traveller's turn bar");
        }
    }

    private static void toTheNether(GameTestHelper helper, ServerPlayer player) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        if (nether == null) {
            helper.fail("the GameTest server must have a Nether to travel to");
        }
        player.teleportTo(nether, player.getX(), 100.0, player.getZ(), 0.0f, 0.0f);
        if (player.serverLevel() != nether) {
            helper.fail("the traveller must have arrived in the Nether");
        }
    }

    /**
     * An outsider's joining hit, with the d20s it draws scripted in order: its initiative, then the
     * attack roll it now is. Every other die rolls its highest face.
     */
    private static ScenarioSupport.ScriptedDice opening(int attackFace, Runnable hit) {
        ScenarioSupport.ScriptedDice dice = ScenarioSupport.forceD20Faces(JOINER_MIDDLE, attackFace);
        try {
            hit.run();
        } finally {
            RollService.resetRoller();
        }
        return dice;
    }

    private static void requireRolledMiss(
            GameTestHelper helper, Encounter encounter, LivingEntity attacker, LivingEntity victim, Runnable hit) {
        float before = victim.getHealth();
        int shown = ROLLS.sent.size();
        ScenarioSupport.ScriptedDice dice = opening(MISS, hit);
        if (!encounter.contains(attacker.getUUID())) {
            helper.fail("the opening attack must still join its attacker");
        }
        if (dice.d20Draws != 2) {
            helper.fail("the opening attack must roll a d20 of its own after initiative, but " + dice.d20Draws
                    + " were drawn");
        }
        if (victim.getHealth() < before) {
            helper.fail("a missed opening attack must deal no damage, but dealt " + (before - victim.getHealth()));
        }
        if (ROLLS.since(shown).stream().noneMatch(sent -> sent.animation().kind() == RollAnimation.Kind.ATTACK)) {
            helper.fail("the opening attack's roll must be shown like a turn attack's");
        }
    }

    private static void swing(GameTestHelper helper, Player attacker, LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().playerAttack(attacker), 1.0f);
    }

    private static void melee(GameTestHelper helper, Mob attacker, Player target) {
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1.0f);
    }

    private static void shoot(GameTestHelper helper, LivingEntity shooter, LivingEntity target) {
        Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        arrow.setPos(shooter.getX(), shooter.getY(), shooter.getZ());
        helper.getLevel().addFreshEntity(arrow);
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().arrow(arrow, shooter), 1.0f);
        arrow.discard();
    }

    /** No AI, so only the scenario and the turn move it; aimed at the player like the mob that hit them. */
    private static <T extends Mob> T spawnHostile(
            GameTestHelper helper, EntityType<T> type, int x, int z, Player target) {
        T mob = helper.spawn(type, new BlockPos(x, 1, z));
        mob.setNoAi(true);
        mob.setTarget(target);
        return mob;
    }

    private static ServerPlayer spawnServerPlayer(GameTestHelper helper, double x, double z) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(x, 1, z));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }

    private static Encounter encounterOf(GameTestHelper helper, LivingEntity entity) {
        Encounter encounter = EncounterManager.encounterContaining(helper.getLevel(), entity.getUUID())
                .orElse(null);
        if (encounter == null) {
            helper.fail(entity.getName().getString() + " must be in an encounter for the scenario to continue");
        }
        return encounter;
    }

    private static void prepare(GameTestHelper helper, EncounterConfig encounters) {
        RollService.resetRoller();
        InitiativeConfig.overrideEncountersForTesting(encounters);
        InitiativeConfig.overrideTurnsForTesting(TURNS);
        InitiativeConfig.overrideActionsForTesting(ACTIONS);
        InitiativeConfig.overrideChecksForTesting(ScenarioSupport.CHECKS_OFF);
        InitiativeConfig.overrideGrappleForTesting(ScenarioSupport.GRAPPLE_DEFAULT);
        InitiativeConfig.overrideCoverForTesting(ScenarioSupport.COVER_OFF);
        InitiativeConfig.overrideActionUiForTesting(new ActionUiConfig(true));
        InitiativeConfig.overrideHudForTesting(new HudConfig(true));
        InitiativeConfig.overrideRollAnimationForTesting(SHOWN);
        NOTICES.sent.clear();
        SkippedTurns.overrideSenderForTesting(NOTICES);
        RollAnimationSync.overrideSenderForTesting(ROLLS);
        ScenarioSupport.discardLeftoverMobsNearby(helper);
    }

    private static void clearSenders() {
        TurnOrderSync.clearSenderOverrideForTesting();
        ActionUiSync.clearSenderOverrideForTesting();
    }

    private static void cleanUp(LivingEntity... entities) {
        RollService.resetRoller();
        RollAnimationSync.clearSenderOverrideForTesting();
        SkippedTurns.clearSenderOverrideForTesting();
        for (LivingEntity entity : entities) {
            entity.discard();
        }
    }
}
