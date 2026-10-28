package studio.modroll.initiative.checks;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.ContestRoll;
import studio.modroll.checks.api.OpenRoll;
import studio.modroll.checks.api.Skill;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.dice.RollDetail;

/**
 * Every call Initiative makes into Checks. Call it only once {@link ChecksIntegration#active} is
 * true: this class names Checks types, so loading it without Checks installed fails. Checks rolls
 * every die through Critfall, so these rolls script and animate like any other. A check event
 * listener can cancel a roll, which then rolled nothing: these come back empty rather than as the
 * zero totals Checks reports for it.
 */
public final class ChecksBridge {

    /** A rolled check with no opponent: its d20s in the mode actually rolled, and its total. */
    public record Rolled(RollDetail roll, int total) {}

    private static final ResourceLocation ATHLETICS = skillId("athletics");
    private static final ResourceLocation ACROBATICS = skillId("acrobatics");
    private static final ResourceLocation STEALTH = skillId("stealth");
    private static final ResourceLocation PERCEPTION = skillId("perception");

    private ChecksBridge() {}

    /** A datapack can drop a standard skill; without one of these the flat rolls stand in. */
    static boolean skillsLoaded() {
        return Stream.of(ATHLETICS, ACROBATICS, STEALTH, PERCEPTION)
                .allMatch(id -> ChecksApi.skill(id).isPresent());
    }

    /** Shove and Grapple: the attacker's Athletics against the defender's Athletics or Acrobatics, whichever is higher. */
    public static Optional<ContestResult> athleticsContest(LivingEntity attacker, LivingEntity defender) {
        return rolled(ChecksApi.contest(attacker, skill(ATHLETICS), defender, athleticsOrAcrobatics(defender)));
    }

    /** Escape: the escaper's Athletics or Acrobatics, whichever is higher, against the grappler's Athletics. */
    public static Optional<ContestResult> escapeContest(LivingEntity escaper, LivingEntity grappler) {
        return rolled(ChecksApi.contest(escaper, athleticsOrAcrobatics(escaper), grappler, skill(ATHLETICS)));
    }

    public static Optional<Rolled> stealthCheck(LivingEntity hider) {
        OpenRoll stealth = ChecksApi.roll(hider, skill(STEALTH));
        if (stealth.canceled()) {
            return Optional.empty();
        }
        return Optional.of(new Rolled(
                RollDetail.of(stealth.mode(), stealth.result()),
                stealth.result().total()));
    }

    /** The modifier initiative adds: the entity's Dexterity. */
    public static int dexterityModifier(LivingEntity entity) {
        return ChecksApi.abilityModifier(entity, Ability.DEXTERITY);
    }

    public static int passivePerception(LivingEntity watcher) {
        return ChecksApi.passiveScore(watcher, skill(PERCEPTION));
    }

    private static Skill athleticsOrAcrobatics(LivingEntity entity) {
        Skill athletics = skill(ATHLETICS);
        Skill acrobatics = skill(ACROBATICS);
        return ChecksApi.skillModifier(entity, acrobatics) > ChecksApi.skillModifier(entity, athletics)
                ? acrobatics
                : athletics;
    }

    private static Optional<ContestResult> rolled(ContestRoll contest) {
        return contest.canceled() ? Optional.empty() : Optional.of(contest.result());
    }

    private static Skill skill(ResourceLocation id) {
        return ChecksApi.skill(id).orElseThrow();
    }

    private static ResourceLocation skillId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ChecksIntegration.MOD_ID, path);
    }
}
