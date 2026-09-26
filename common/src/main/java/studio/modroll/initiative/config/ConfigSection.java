package studio.modroll.initiative.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * One config group, read key by key against the built-in defaults. A key that is absent, or that its
 * reader refuses, falls back to its own default, so one bad line costs one setting instead of the
 * whole file.
 */
final class ConfigSection {

    private final ConfigSource source;
    private final String name;
    private final JsonObject values;
    private final JsonObject defaults;
    private final boolean wholeGroupDefaulted;

    private ConfigSection(
            ConfigSource source, String name, JsonObject values, JsonObject defaults, boolean wholeGroupDefaulted) {
        this.source = source;
        this.name = name;
        this.values = values;
        this.defaults = defaults;
        this.wholeGroupDefaulted = wholeGroupDefaulted;
    }

    static ConfigSection of(ConfigSource source, String name, JsonObject values, JsonObject defaults) {
        return new ConfigSection(source, name, values, defaults, false);
    }

    /** A group the file does not usably have: the group was reported once, its keys stay silent. */
    static ConfigSection defaulted(ConfigSource source, String name, JsonObject defaults) {
        return new ConfigSection(source, name, defaults, defaults, true);
    }

    <T> T read(String key, ConfigValue<T> value) {
        if (wholeGroupDefaulted) {
            return fromDefaults(key, value);
        }
        if (!values.has(key)) {
            source.report(ConfigIssue.missing(path(key)));
            return fromDefaults(key, value);
        }
        try {
            return value.read(values, key);
        } catch (JsonParseException e) {
            source.report(ConfigIssue.invalid(path(key), e.getMessage()));
            return fromDefaults(key, value);
        }
    }

    /** For a key whose validity depends on a sibling: only the built-in pair is known to agree. */
    <T> T revertToDefault(String key, ConfigValue<T> value, String reason) {
        source.report(ConfigIssue.invalid(path(key), reason));
        return fromDefaults(key, value);
    }

    private <T> T fromDefaults(String key, ConfigValue<T> value) {
        try {
            return value.read(defaults, key);
        } catch (JsonParseException e) {
            throw new IllegalStateException("bundled default config has no usable " + path(key), e);
        }
    }

    private String path(String key) {
        return name + "." + key;
    }
}
