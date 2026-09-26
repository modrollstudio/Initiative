package studio.modroll.initiative.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ActionSettingsTest {

    private static final ResourceLocation TAUNT = ResourceLocation.fromNamespaceAndPath("examplemod", "taunt");
    private static final ResourceLocation ABSENT = ResourceLocation.fromNamespaceAndPath("examplemod", "absent");

    private static JsonObject json(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    @AfterEach
    void clear() {
        ActionSettings.clearForTesting();
    }

    @Test
    void storedSettingsAreReturnedById() {
        ActionSettings.replaceAll(Map.of(TAUNT, json("{\"reach_blocks\": 4.0}")));
        assertEquals(
                4.0, ActionSettings.get(TAUNT).orElseThrow().get("reach_blocks").getAsDouble());
    }

    @Test
    void unknownIdHasNoSettings() {
        assertTrue(ActionSettings.get(ABSENT).isEmpty());
    }

    @Test
    void enabledReadsTheField() {
        ActionSettings.replaceAll(Map.of(TAUNT, json("{\"enabled\": false}")));
        assertFalse(ActionSettings.enabled(TAUNT, true));
    }

    @Test
    void enabledFallsBackWithoutAFile() {
        assertTrue(ActionSettings.enabled(ABSENT, true));
        assertFalse(ActionSettings.enabled(ABSENT, false));
    }

    @Test
    void enabledFallsBackWithoutTheField() {
        ActionSettings.replaceAll(Map.of(TAUNT, json("{\"reach_blocks\": 4.0}")));
        assertTrue(ActionSettings.enabled(TAUNT, true));
    }

    @Test
    void enabledFallsBackWhenTheFieldIsNull() {
        ActionSettings.replaceAll(Map.of(TAUNT, json("{\"enabled\": null}")));
        assertTrue(ActionSettings.enabled(TAUNT, true));
        assertFalse(ActionSettings.enabled(TAUNT, false));
    }

    @Test
    void enabledFallsBackWhenTheFieldIsNotABoolean() {
        ActionSettings.replaceAll(Map.of(TAUNT, json("{\"enabled\": {\"oops\": 1}}")));
        assertTrue(ActionSettings.enabled(TAUNT, true));
    }

    @Test
    void reloadDropsThePreviousLoad() {
        ActionSettings.replaceAll(Map.of(TAUNT, json("{\"enabled\": false}")));
        ActionSettings.replaceAll(Map.of());
        assertTrue(ActionSettings.get(TAUNT).isEmpty());
    }
}
