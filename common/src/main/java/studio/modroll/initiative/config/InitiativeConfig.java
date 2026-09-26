package studio.modroll.initiative.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import studio.modroll.initiative.Initiative;

/**
 * The whole of Initiative's configuration, parsed once at mod construction and read from anywhere
 * afterwards. Each group is an immutable record swapped in as a unit, so a reader never sees a
 * half-applied file. The {@code override*ForTesting} methods are test-scope only, mirroring
 * Critfall's {@code RollService.setRoller} convention.
 */
public final class InitiativeConfig {

    private static final String FILE_NAME = "initiative.json";

    private static volatile EncounterConfig encounters;
    private static volatile TurnConfig turns;
    private static volatile ActionConfig actions;
    private static volatile CoverConfig cover;
    private static volatile GrappleConfig grapple;
    private static volatile HudConfig hud;
    private static volatile ActionUiConfig actionUi;
    private static volatile RollAnimationConfig rollAnimation;

    private InitiativeConfig() {}

    public static void load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(configDir);
                Files.writeString(file, ConfigSource.defaultsJson());
            }
            parse(Files.readString(file));
        } catch (IOException | JsonParseException e) {
            Initiative.LOG.error("Failed to load {}; using built-in defaults", file, e);
            parse(ConfigSource.defaultsJson());
        }
    }

    public static EncounterConfig encounters() {
        return loaded(encounters);
    }

    public static TurnConfig turns() {
        return loaded(turns);
    }

    public static ActionConfig actions() {
        return loaded(actions);
    }

    public static CoverConfig cover() {
        return loaded(cover);
    }

    public static GrappleConfig grapple() {
        return loaded(grapple);
    }

    public static HudConfig hud() {
        return loaded(hud);
    }

    public static ActionUiConfig actionUi() {
        return loaded(actionUi);
    }

    public static RollAnimationConfig rollAnimation() {
        return loaded(rollAnimation);
    }

    public static void overrideEncountersForTesting(EncounterConfig config) {
        encounters = config;
    }

    public static void overrideTurnsForTesting(TurnConfig config) {
        turns = config;
    }

    public static void overrideActionsForTesting(ActionConfig config) {
        actions = config;
    }

    public static void overrideCoverForTesting(CoverConfig config) {
        cover = config;
    }

    public static void overrideGrappleForTesting(GrappleConfig config) {
        grapple = config;
    }

    public static void overrideHudForTesting(HudConfig config) {
        hud = config;
    }

    public static void overrideActionUiForTesting(ActionUiConfig config) {
        actionUi = config;
    }

    public static void overrideRollAnimationForTesting(RollAnimationConfig config) {
        rollAnimation = config;
    }

    private static <T> T loaded(T group) {
        if (group == null) {
            throw new IllegalStateException("Initiative config accessed before load");
        }
        return group;
    }

    private static void parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new JsonSyntaxException("config root must be a JSON object");
        }
        ConfigSource source = ConfigSource.of(root.getAsJsonObject());
        encounters = EncounterConfig.fromJson(source);
        turns = TurnConfig.fromJson(source);
        actions = ActionConfig.fromJson(source);
        cover = CoverConfig.fromJson(source);
        grapple = GrappleConfig.fromJson(source);
        hud = HudConfig.fromJson(source);
        actionUi = ActionUiConfig.fromJson(source);
        rollAnimation = RollAnimationConfig.fromJson(source);
        report(source.issues());
    }

    /** The file is never rewritten, so this log is the only trace of what the load had to change. */
    private static void report(List<ConfigIssue> issues) {
        for (ConfigIssue issue : issues) {
            if (issue.kind() == ConfigIssue.Kind.INVALID) {
                Initiative.LOG.error("{}: {}", FILE_NAME, issue.message());
            } else {
                Initiative.LOG.warn("{}: {}", FILE_NAME, issue.message());
            }
        }
    }
}
