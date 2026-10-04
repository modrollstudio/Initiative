package studio.modroll.initiative.checks;

import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Skill;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.dice.RollResult;

/**
 * Every call Initiative makes into Checks. Call it only once {@link ChecksIntegration#active} is
 * true: this class names Checks types, so loading it without Checks installed fails. Checks rolls
 * every die through Critfall, so these rolls script and animate like any other.
 */
public final class ChecksBridge {

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
    public static ContestResult athleticsContest(LivingEntity attacker, LivingEntity defender) {
        return ChecksApi.contest(attacker, skill(ATHLETICS), defender, athleticsOrAcrobatics(defender));
    }

    /** Escape: the escaper's Athletics or Acrobatics, whichever is higher, against the grappler's Athletics. */
    public static ContestResult escapeContest(LivingEntity escaper, LivingEntity grappler) {
        return ChecksApi.contest(escaper, athleticsOrAcrobatics(escaper), grappler, skill(ATHLETICS));
    }

    public static RollResult stealthCheck(LivingEntity hider) {
        return ChecksApi.roll(hider, skill(STEALTH));
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

    private static Skill skill(ResourceLocation id) {
        return ChecksApi.skill(id).orElseThrow();
    }

    private static ResourceLocation skillId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ChecksIntegration.MOD_ID, path);
    }
}
