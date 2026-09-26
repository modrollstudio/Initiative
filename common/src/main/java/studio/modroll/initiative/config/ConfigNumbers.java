package studio.modroll.initiative.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.GsonHelper;

/**
 * The one way this package reads a config double. A literal too large for a double — {@code 1e400},
 * an extra row of zeroes — parses to infinity, and infinity satisfies every one-sided range check
 * the records apply afterwards, so it would reach the game as a radius or a budget that no distance
 * ever exceeds. Finiteness is settled here, before any range check runs.
 */
final class ConfigNumbers {

    private ConfigNumbers() {}

    static double finiteDouble(JsonObject json, String key) {
        double value = GsonHelper.getAsDouble(json, key);
        if (!Double.isFinite(value)) {
            throw new JsonSyntaxException(key + " must be a finite number but was " + value);
        }
        return value;
    }
}
