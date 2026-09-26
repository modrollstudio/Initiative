package studio.modroll.initiative.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.initiative.Initiative;
import studio.modroll.initiative.action.ActionFeedback;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionCost;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;
import studio.modroll.initiative.api.ActionTargeting;

/**
 * The action commands, built from {@link ActionRegistry} rather than hand-written: a registered
 * action gets its command for free, whoever registered it. The second front end beside the turn UI,
 * and the only one left when that UI is switched off.
 */
public final class InitiativeCommands {

    private static final String TARGET = "target";
    private static final String POSITION = "position";

    private InitiativeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("initiative");
        for (Action action : ActionRegistry.all()) {
            if (action.cost() == ActionCost.REACTION) {
                continue; // reactions are triggered, never typed
            }
            root.then(branch(action));
        }
        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> branch(Action action) {
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(literalFor(action));
        return switch (action.targeting()) {
            case ActionTargeting.Entity ignored ->
                literal.then(Commands.argument(TARGET, EntityArgument.entity())
                        .executes(context -> runOnEntity(context, action)));
            case ActionTargeting.Position ignored ->
                literal.then(Commands.argument(POSITION, Vec3Argument.vec3())
                        .executes(context ->
                                run(context, action, ActionRequest.at(Vec3Argument.getVec3(context, POSITION)))));
            case ActionTargeting.None ignored ->
                literal.executes(context -> run(context, action, ActionRequest.none()));
            case ActionTargeting.Self ignored ->
                literal.executes(context -> run(context, action, ActionRequest.none()));
        };
    }

    /** Own-namespace actions keep their bare path; anyone else's is namespaced to keep the tree unambiguous. */
    public static String literalFor(Action action) {
        if (action.commandLiteral() != null) {
            return action.commandLiteral();
        }
        ResourceLocation id = action.id();
        return Initiative.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.getNamespace() + ":" + id.getPath();
    }

    private static int runOnEntity(CommandContext<CommandSourceStack> context, Action action)
            throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, TARGET);
        if (!(target instanceof LivingEntity living)) {
            ActionFeedback.send(
                    context.getSource().getPlayerOrException(),
                    action,
                    ActionResult.rejected(ActionStatus.INVALID_TARGET));
            return 0;
        }
        return run(context, action, ActionRequest.of(living));
    }

    private static int run(CommandContext<CommandSourceStack> context, Action action, ActionRequest request)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ActionResult result = ActionRegistry.invoke(player.serverLevel(), player, action.id(), request);
        ActionFeedback.send(player, action, result);
        return result.isPerformed() ? 1 : 0;
    }
}
