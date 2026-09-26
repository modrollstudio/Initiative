package studio.modroll.initiative.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.GsonHelper;

/**
 * Reads and range-checks one config key. Throwing is how a reader reports a value it cannot use;
 * {@link ConfigSection} turns that into a fallback to the built-in default for that key alone.
 */
@FunctionalInterface
interface ConfigValue<T> {

    ConfigValue<Boolean> BOOLEAN = GsonHelper::getAsBoolean;
    ConfigValue<Integer> INTEGER = GsonHelper::getAsInt;
    ConfigValue<Integer> NON_NEGATIVE_INTEGER = atLeast(0);
    ConfigValue<Integer> POSITIVE_INTEGER = atLeast(1);
    ConfigValue<Double> FINITE_DOUBLE = ConfigNumbers::finiteDouble;
    ConfigValue<Double> NON_NEGATIVE_DOUBLE = (json, key) -> {
        double value = ConfigNumbers.finiteDouble(json, key);
        if (value < 0) {
            throw new JsonSyntaxException(key + " must not be negative but was " + value);
        }
        return value;
    };
    ConfigValue<Double> POSITIVE_DOUBLE = (json, key) -> {
        double value = ConfigNumbers.finiteDouble(json, key);
        if (value <= 0) {
            throw new JsonSyntaxException(key + " must be positive but was " + value);
        }
        return value;
    };

    T read(JsonObject json, String key);

    static ConfigValue<Integer> atLeast(int minimum) {
        return (json, key) -> {
            int value = GsonHelper.getAsInt(json, key);
            if (value < minimum) {
                throw new JsonSyntaxException(key + " must be at least " + minimum + " but was " + value);
            }
            return value;
        };
    }
}
