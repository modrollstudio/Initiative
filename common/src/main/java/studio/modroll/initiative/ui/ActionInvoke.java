package studio.modroll.initiative.ui;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import studio.modroll.initiative.action.ActionFeedback;
import studio.modroll.initiative.api.ActionRegistry;
import studio.modroll.initiative.api.ActionRequest;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;

/**
 * The server end of a button click. It resolves what the click pointed at and hands it to
 * {@link ActionRegistry#invoke}, which is the same call the command makes: the UI is a second front
 * end, never a second set of rules. With the UI off, clicks are refused and only commands remain.
 */
public final class ActionInvoke {

    private ActionInvoke() {}

    public static ActionResult perform(ServerLevel level, Player player, ActionInvokePayload payload) {
        if (!ActionUiSync.enabled()) {
            return ActionResult.rejected(ActionStatus.INACTIVE);
        }
        ActionResult result = ActionRegistry.invoke(level, player, payload.actionId(), request(level, payload));
        ActionFeedback.send(player, ActionRegistry.get(payload.actionId()).orElse(null), result);
        return result;
    }

    /** The wire carries three raw doubles, so a crafted packet can point an action at nowhere. */
    static boolean isUsablePosition(Vec3 position) {
        return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
    }

    private static ActionRequest request(ServerLevel level, ActionInvokePayload payload) {
        if (payload.targetEntityId() != ActionInvokePayload.NO_TARGET) {
            return level.getEntity(payload.targetEntityId()) instanceof LivingEntity target
                    ? ActionRequest.of(target)
                    : ActionRequest.none();
        }
        return payload.position()
                .filter(ActionInvoke::isUsablePosition)
                .map(ActionRequest::at)
                .orElseGet(ActionRequest::none);
    }
}
