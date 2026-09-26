package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;
import static studio.modroll.initiative.config.ConfigValue.NON_NEGATIVE_DOUBLE;
import static studio.modroll.initiative.config.ConfigValue.NON_NEGATIVE_INTEGER;
import static studio.modroll.initiative.config.ConfigValue.POSITIVE_INTEGER;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * The two turn timeouts are deliberately different lengths: a player's is long enough to read the
 * bar and decide, while a mob's is only a stall-breaker for one that cannot act at all — a mob with
 * an available action already ends its turn the moment it spends it.
 */
public record TurnConfig(
        boolean enabled,
        int turnTimeoutTicks,
        int mobTurnTimeoutTicks,
        double initiativeBonusPerSpeed,
        int initiativeMaxBonus,
        boolean freezeEnabled,
        boolean actingMarkerEnabled,
        boolean restrictPlayersOffTurn,
        Set<String> noFreezeTypes) {

    public static TurnConfig fromJson(ConfigSource source) {
        ConfigSection turns = source.section("turns");
        return new TurnConfig(
                turns.read("enabled", BOOLEAN),
                turns.read("turn_timeout_ticks", POSITIVE_INTEGER),
                turns.read("mob_turn_timeout_ticks", POSITIVE_INTEGER),
                turns.read("initiative_bonus_per_speed", NON_NEGATIVE_DOUBLE),
                turns.read("initiative_max_bonus", NON_NEGATIVE_INTEGER),
                turns.read("freeze_enabled", BOOLEAN),
                turns.read("acting_marker_enabled", BOOLEAN),
                turns.read("restrict_players_off_turn", BOOLEAN),
                turns.read("no_freeze_types", TurnConfig::noFreezeTypes));
    }

    private static Set<String> noFreezeTypes(JsonObject turns, String key) {
        JsonArray array = GsonHelper.getAsJsonArray(turns, key);
        Set<String> types = new HashSet<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new JsonSyntaxException("no_freeze_types entries must be strings but found " + element);
            }
            String id = element.getAsString();
            if (ResourceLocation.tryParse(id) == null) {
                throw new JsonSyntaxException("no_freeze_types entry is not a valid entity type id: " + id);
            }
            types.add(id);
        }
        return Set.copyOf(types);
    }
}
