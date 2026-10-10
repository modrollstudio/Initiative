package studio.modroll.initiative.checks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.initiative.config.ChecksConfig;
import studio.modroll.initiative.config.InitiativeConfig;

/**
 * Checks is not on the unit-test classpath, so any path here that loaded {@link ChecksBridge} would
 * throw {@code NoClassDefFoundError} instead of answering.
 */
class ChecksIntegrationTest {

    @AfterEach
    void forgetChecks() {
        ChecksIntegration.setPresent(false);
    }

    @Test
    void withoutChecksInstalledItIsInactiveWithoutTouchingChecks() {
        InitiativeConfig.overrideChecksForTesting(new ChecksConfig(true));
        ChecksIntegration.setPresent(false);
        assertFalse(ChecksIntegration.active());
    }

    @Test
    void switchedOffItIsInactiveWithoutTouchingChecks() {
        InitiativeConfig.overrideChecksForTesting(new ChecksConfig(false));
        ChecksIntegration.setPresent(true);
        assertFalse(ChecksIntegration.active());
    }

    @Test
    void installedAndOnItIsEnabledWithoutTouchingChecks() {
        InitiativeConfig.overrideChecksForTesting(new ChecksConfig(true));
        ChecksIntegration.setPresent(true);
        assertTrue(ChecksIntegration.enabled());
    }

    @Test
    void withoutChecksInstalledOrSwitchedOffItIsNotEnabled() {
        InitiativeConfig.overrideChecksForTesting(new ChecksConfig(true));
        ChecksIntegration.setPresent(false);
        assertFalse(ChecksIntegration.enabled());
        InitiativeConfig.overrideChecksForTesting(new ChecksConfig(false));
        ChecksIntegration.setPresent(true);
        assertFalse(ChecksIntegration.enabled());
    }
}
