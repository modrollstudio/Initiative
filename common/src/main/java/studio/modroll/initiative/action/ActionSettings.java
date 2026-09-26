package studio.modroll.initiative.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import studio.modroll.initiative.Initiative;

/**
 * Per-action tunables supplied by datapacks at {@code data/<namespace>/initiative/actions/<path>.json}.
 * Actions define their behavior in code; this is where their numbers and their toggle come from when
 * they are not part of Initiative's own config file.
 */
public final class ActionSettings {

    private static volatile Map<ResourceLocation, JsonObject> settings = Map.of();

    private ActionSettings() {}

    public static Optional<JsonObject> get(ResourceLocation id) {
        return Optional.ofNullable(settings.get(id));
    }

    /**
     * An action's toggle is read on every gate check and every UI rebuild — once per tick while its
     * owner is acting — so a datapack that writes something other than a boolean must not be able to
     * throw from there. A bad value logs and leaves the action at the fallback its author chose.
     */
    public static boolean enabled(ResourceLocation id, boolean fallback) {
        return get(id).map(json -> enabledOrFallback(id, json, fallback)).orElse(fallback);
    }

    private static boolean enabledOrFallback(ResourceLocation id, JsonObject json, boolean fallback) {
        JsonElement value = json.get("enabled");
        if (value == null) {
            return fallback;
        }
        if (!GsonHelper.isBooleanValue(value)) {
            Initiative.LOG.warn("Action settings for {} have a non-boolean \"enabled\"; leaving it {}", id, fallback);
            return fallback;
        }
        return value.getAsBoolean();
    }

    public static void replaceAll(Map<ResourceLocation, JsonObject> loaded) {
        settings = Map.copyOf(loaded);
    }

    /** Test-scope only, mirroring Critfall's {@code RollService.setRoller} convention. */
    public static void clearForTesting() {
        settings = Map.of();
    }
}
