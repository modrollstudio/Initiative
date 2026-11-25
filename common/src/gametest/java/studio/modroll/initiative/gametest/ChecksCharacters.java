package studio.modroll.initiative.gametest;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterStore;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.PresetGrants;
import studio.modroll.checks.level.Levelling;

/**
 * Makes a player a character of a Checks class and level, the way Checks' own tests do; its API
 * only reads them. Names Checks types, so only load it in a run with Checks installed.
 */
final class ChecksCharacters {

    private ChecksCharacters() {}

    static void make(ServerPlayer player, String characterClass, int level) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, 10);
        }
        Optional<ResourceLocation> classId =
                Optional.of(ResourceLocation.fromNamespaceAndPath("checks", characterClass));
        CharacterBuild build = new CharacterBuild(
                CreationMethod.STANDARD_ARRAY,
                scores,
                Set.of(),
                new PresetChoices(Optional.empty(), Optional.empty(), classId, Optional.empty()),
                PresetGrants.NONE);
        CharacterStore.get(player.server).update(player.getUUID(), character -> character.withBuild(build));
        Levelling.setLevel(player, level);
    }
}
