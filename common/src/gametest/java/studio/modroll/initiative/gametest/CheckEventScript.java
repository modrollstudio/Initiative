package studio.modroll.initiative.gametest;

import java.util.function.Consumer;
import studio.modroll.checks.api.BeforeCheckEvent;
import studio.modroll.checks.api.ChecksApi;

/**
 * Steers Checks rolls through its check event for one action. Checks keeps a listener for the
 * server's lifetime, so a single one is registered on first use and runs whatever script is current,
 * which is nothing outside these calls. This class names Checks types: only scenarios that need
 * Checks installed may touch it.
 */
final class CheckEventScript {

    private static final Consumer<BeforeCheckEvent> NOTHING = event -> {};

    private static volatile Consumer<BeforeCheckEvent> script = NOTHING;
    private static boolean registered;

    private CheckEventScript() {}

    /** Runs {@code action} with every check rolled inside it canceled. */
    static void canceling(Runnable action) {
        during(BeforeCheckEvent::cancel, action);
    }

    /** Runs {@code action} with every check rolled inside it granted advantage. */
    static void withAdvantage(Runnable action) {
        during(BeforeCheckEvent::grantAdvantage, action);
    }

    private static void during(Consumer<BeforeCheckEvent> each, Runnable action) {
        register();
        script = each;
        try {
            action.run();
        } finally {
            script = NOTHING;
        }
    }

    private static synchronized void register() {
        if (!registered) {
            ChecksApi.onBeforeCheck(event -> script.accept(event));
            registered = true;
        }
    }
}
