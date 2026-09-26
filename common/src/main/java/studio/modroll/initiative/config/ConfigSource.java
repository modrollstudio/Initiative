package studio.modroll.initiative.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.GsonHelper;

/**
 * A config file paired with the built-in defaults it is read against. Whatever the file leaves out,
 * gets wrong, or says about a setting this build does not have is recorded as an issue and the
 * default stands in for that key alone, so a file written against an older Initiative keeps every
 * value it does specify instead of being discarded whole.
 */
public final class ConfigSource {

    static final String FORMAT_VERSION = "format_version";

    private static final String DEFAULTS_RESOURCE = "/initiative-default-config.json";

    private final JsonObject values;
    private final JsonObject defaults;
    private final List<ConfigIssue> issues = new ArrayList<>();

    private ConfigSource(JsonObject values, JsonObject defaults) {
        this.values = values;
        this.defaults = defaults;
    }

    public static ConfigSource of(JsonObject values) {
        ConfigSource source =
                new ConfigSource(values, JsonParser.parseString(defaultsJson()).getAsJsonObject());
        source.checkFormatVersion();
        source.reportUnknownKeys(values, source.defaults, "");
        return source;
    }

    static String defaultsJson() {
        try (InputStream in = ConfigSource.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("bundled default config resource missing");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read bundled default config", e);
        }
    }

    ConfigSection section(String name) {
        JsonObject sectionDefaults = defaults.getAsJsonObject(name);
        if (sectionDefaults == null) {
            throw new IllegalStateException("bundled default config has no " + name + " group");
        }
        JsonElement present = values.get(name);
        if (present == null) {
            report(ConfigIssue.missing(name));
            return ConfigSection.defaulted(this, name, sectionDefaults);
        }
        if (!present.isJsonObject()) {
            report(ConfigIssue.invalid(name, "not a JSON object"));
            return ConfigSection.defaulted(this, name, sectionDefaults);
        }
        return ConfigSection.of(this, name, present.getAsJsonObject(), sectionDefaults);
    }

    List<ConfigIssue> issues() {
        return List.copyOf(issues);
    }

    void report(ConfigIssue issue) {
        issues.add(issue);
    }

    /** Absent on every file written before the version existed, which is exactly the file to keep. */
    private void checkFormatVersion() {
        if (!values.has(FORMAT_VERSION)) {
            return;
        }
        int declared;
        try {
            declared = GsonHelper.getAsInt(values, FORMAT_VERSION);
        } catch (JsonParseException e) {
            report(ConfigIssue.invalid(FORMAT_VERSION, e.getMessage()));
            return;
        }
        int supported = GsonHelper.getAsInt(defaults, FORMAT_VERSION);
        if (declared > supported) {
            report(ConfigIssue.newerFormat(declared, supported));
        }
    }

    private void reportUnknownKeys(JsonObject present, JsonObject known, String prefix) {
        for (Map.Entry<String, JsonElement> entry : present.entrySet()) {
            JsonElement counterpart = known.get(entry.getKey());
            if (counterpart == null) {
                report(ConfigIssue.unknown(prefix + entry.getKey()));
            } else if (counterpart.isJsonObject() && entry.getValue().isJsonObject()) {
                reportUnknownKeys(
                        entry.getValue().getAsJsonObject(),
                        counterpart.getAsJsonObject(),
                        prefix + entry.getKey() + ".");
            }
        }
    }
}
