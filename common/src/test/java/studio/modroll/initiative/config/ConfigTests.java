package studio.modroll.initiative.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.List;

final class ConfigTests {

    private ConfigTests() {}

    static ConfigSource source(String body) {
        return ConfigSource.of(JsonParser.parseString(body).getAsJsonObject());
    }

    static void assertReported(ConfigSource source, ConfigIssue.Kind kind, String key) {
        assertTrue(
                source.issues().stream()
                        .anyMatch(issue -> issue.kind() == kind && issue.key().equals(key)),
                () -> "expected a " + kind + " issue for '" + key + "' but got " + source.issues());
    }

    static void assertNothingReported(ConfigSource source) {
        assertEquals(List.of(), source.issues());
    }
}
