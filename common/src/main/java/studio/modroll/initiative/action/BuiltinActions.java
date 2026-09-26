package studio.modroll.initiative.action;

import net.minecraft.resources.ResourceLocation;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionTargeting;
import studio.modroll.initiative.api.ActionTargeting.Side;
import studio.modroll.initiative.config.ActionConfig;
import studio.modroll.initiative.config.InitiativeConfig;

/**
 * Initiative's own actions, registered through the same public API a third-party mod uses. Nothing
 * here is privileged: the registry and the invocation pipeline cannot tell these apart from any
 * other registration.
 */
public final class BuiltinActions {

    public static final ResourceLocation ATTACK = id("attack");
    public static final ResourceLocation DASH = id("dash");
    public static final ResourceLocation DISENGAGE = id("disengage");
    public static final ResourceLocation DODGE = id("dodge");
    public static final ResourceLocation HELP = id("help");
    public static final ResourceLocation HIDE = id("hide");
    public static final ResourceLocation SHOVE = id("shove");
    public static final ResourceLocation BLINK = id("blink");
    public static final ResourceLocation REEL = id("reel");
    public static final ResourceLocation GRAPPLE = id("grapple");
    public static final ResourceLocation ESCAPE = id("escape");
    public static final ResourceLocation END_TURN = id("end_turn");
    public static final ResourceLocation OPPORTUNITY_ATTACK = id("opportunity_attack");

    private BuiltinActions() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Initiative.MOD_ID, path);
    }

    private static ActionConfig config() {
        return InitiativeConfig.actions();
    }

    public static void registerAll() {
        ActionRegistry.register(Action.builder(ATTACK)
                // The attack is the one action whose cost is a config decision: with
                // single_attack_per_turn off it is free and limited only by the turn itself.
                .cost(() -> config().singleAttackPerTurn() ? ActionCost.ACTION : ActionCost.FREE)
                .targeting(ActionTargeting.entity(Side.ENEMY, () -> config().attackReachBlocks()))
                .effect(ActionEconomy::performAttack)
                .build());
        ActionRegistry.register(Action.builder(DASH)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.self())
                .enabled(() -> config().dash())
                .messages("initiative.command.dash.done", null)
                .effect(ActionEconomy::performDash)
                .build());
        ActionRegistry.register(Action.builder(DISENGAGE)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.self())
                .enabled(() -> config().disengage())
                .messages("initiative.command.disengage.done", null)
                .effect(ActionEconomy::performDisengage)
                .build());
        ActionRegistry.register(Action.builder(DODGE)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.self())
                .enabled(() -> config().dodge())
                .messages("initiative.command.dodge.done", null)
                .effect(ActionEconomy::performDodge)
                .build());
        ActionRegistry.register(Action.builder(HELP)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.entity(Side.ALLY))
                .enabled(() -> config().help())
                .messages("initiative.command.help.done", null)
                .effect(ActionEconomy::performHelp)
                .build());
        ActionRegistry.register(Action.builder(HIDE)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.self())
                .enabled(() -> config().hide())
                .messages("initiative.command.hide.done", "initiative.command.hide.seen")
                .effect(ActionEconomy::performHide)
                .build());
        ActionRegistry.register(Action.builder(SHOVE)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.entity(Side.ENEMY, () -> config().shoveReachBlocks()))
                .enabled(() -> config().shove())
                .messages("initiative.command.shove.done", "initiative.command.shove.resisted")
                .effect(NativeActions::performShove)
                .build());
        ActionRegistry.register(Action.builder(BLINK)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.none())
                .enabled(() -> config().enderPearlBlink())
                .messages("initiative.command.blink.done", null)
                .effect(NativeActions::performBlink)
                .build());
        ActionRegistry.register(Action.builder(REEL)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.entity(Side.ENEMY))
                .enabled(() -> config().fishingRodReel())
                .messages("initiative.command.reel.done", "initiative.command.reel.resisted")
                .effect(NativeActions::performReel)
                .build());
        ActionRegistry.register(Action.builder(GRAPPLE)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.entity(
                        Side.ENEMY, () -> InitiativeConfig.grapple().reachBlocks()))
                .enabled(() -> InitiativeConfig.grapple().enabled())
                .messages("initiative.command.grapple.done", "initiative.command.grapple.resisted")
                .effect(NativeActions::performGrapple)
                .build());
        ActionRegistry.register(Action.builder(ESCAPE)
                .cost(ActionCost.ACTION)
                .targeting(ActionTargeting.self())
                .enabled(() -> InitiativeConfig.grapple().escape())
                .availableTo(Grapples::isGrappled)
                .messages("initiative.command.escape.done", "initiative.command.escape.held")
                .effect(NativeActions::performEscape)
                .build());
        ActionRegistry.register(Action.builder(END_TURN)
                .cost(ActionCost.FREE)
                .targeting(ActionTargeting.none())
                .commandLiteral("endturn")
                .messages("initiative.command.end_turn.done", null)
                .effect(ActionEconomy::performEndTurn)
                .build());
        ActionRegistry.register(Action.builder(OPPORTUNITY_ATTACK)
                .cost(ActionCost.REACTION)
                .targeting(ActionTargeting.entity(Side.ENEMY))
                .enabled(() -> config().opportunityAttack())
                .effect(Reactions::performOpportunityAttack)
                .build());
    }
}
