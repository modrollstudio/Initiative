package studio.modroll.initiative;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.modroll.critfall.api.event.CritfallEvents;
import studio.modroll.initiative.action.BuiltinActions;
import studio.modroll.initiative.config.InitiativeConfig;
import studio.modroll.initiative.encounter.EncounterManager;

/** Loader-agnostic entrypoint. Each loader module calls {@link #init} once at mod construction. */
public final class Initiative {

    public static final String MOD_ID = "initiative";
    public static final String MOD_NAME = "Critfall: Initiative";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Initiative() {}

    public static void init(Path configDir) {
        InitiativeConfig.load(configDir);
        BuiltinActions.registerAll();
        // Fires before all of Critfall's gating (even for hits it will miss or cancel) and never
        // for RollService.performAttack damage, so Initiative's own driven attacks cannot
        // re-trigger detection.
        CritfallEvents.onCombatInteraction(
                event -> EncounterManager.onCombat(event.attacker(), event.target(), event.delivery()));
        LOG.info("{} initialized", MOD_NAME);
    }
}
