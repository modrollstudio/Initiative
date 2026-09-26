package studio.modroll.initiative.config;

import static studio.modroll.initiative.config.ConfigValue.BOOLEAN;
import static studio.modroll.initiative.config.ConfigValue.NON_NEGATIVE_INTEGER;
import static studio.modroll.initiative.config.ConfigValue.POSITIVE_INTEGER;

/**
 * The roll animation: how long the die tumbles, how long the settled result stays, how fast faces
 * flip, and whether the whole encounter watches the roll or only the one player in the exchange.
 */
public record RollAnimationConfig(
        boolean enabled,
        int tumbleTicks,
        int holdTicks,
        int faceChangeTicks,
        boolean holdReadout,
        boolean sharedVisibility) {

    public static RollAnimationConfig fromJson(ConfigSource source) {
        ConfigSection animation = source.section("roll_animation");
        return new RollAnimationConfig(
                animation.read("enabled", BOOLEAN),
                animation.read("tumble_ticks", NON_NEGATIVE_INTEGER),
                animation.read("hold_ticks", NON_NEGATIVE_INTEGER),
                animation.read("face_change_ticks", POSITIVE_INTEGER),
                animation.read("hold_readout", BOOLEAN),
                animation.read("shared_visibility", BOOLEAN));
    }

    public int totalTicks() {
        return tumbleTicks + holdTicks;
    }
}
