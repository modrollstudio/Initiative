package studio.modroll.initiative.action;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import studio.modroll.initiative.Initiative;

/** Reads {@code data/<namespace>/initiative/actions/<path>.json} into {@link ActionSettings} on every reload. */
public class ActionSettingsLoader extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, "actions");

    private static final String DIRECTORY = "initiative/actions";

    public ActionSettingsLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loaded, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonObject> settings = new HashMap<>();
        loaded.forEach((id, element) -> {
            if (element.isJsonObject()) {
                settings.put(id, element.getAsJsonObject());
            } else {
                Initiative.LOG.warn("Ignoring action settings {}: root must be a JSON object", id);
            }
        });
        ActionSettings.replaceAll(settings);
    }
}
