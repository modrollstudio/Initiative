package studio.modroll.initiative.action;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import studio.modroll.initiative.api.Action;
import studio.modroll.initiative.api.ActionResult;
import studio.modroll.initiative.api.ActionStatus;

/**
 * What a player is told about an action they took. The command front end and the turn UI share it,
 * so clicking a button and typing its command read the same line.
 */
public final class ActionFeedback {

    private static final String GENERIC_SUCCESS = "initiative.command.action.done";
    private static final String GENERIC_FAILURE = "initiative.command.action.failed";

    private ActionFeedback() {}

    /**
     * Action lines go to the action bar, not chat: one transient line that the next one — or
     * Critfall's roll readout — replaces, instead of a running column of past actions.
     */
    public static void send(Player player, Action action, ActionResult result) {
        if (!result.isPerformed()) {
            player.displayClientMessage(
                    Component.translatable(rejectionKey(result.status())).withStyle(ChatFormatting.RED), true);
            return;
        }
        player.displayClientMessage(Component.translatable(outcomeKey(action, result.succeeded())), true);
    }

    public static String outcomeKey(Action action, boolean succeeded) {
        if (action == null) {
            return succeeded ? GENERIC_SUCCESS : GENERIC_FAILURE;
        }
        if (succeeded) {
            return action.successKey() != null ? action.successKey() : GENERIC_SUCCESS;
        }
        if (action.failureKey() != null) {
            return action.failureKey();
        }
        return action.successKey() != null ? action.successKey() : GENERIC_FAILURE;
    }

    public static String rejectionKey(ActionStatus status) {
        return switch (status) {
            case PERFORMED -> GENERIC_SUCCESS;
            case DISABLED -> "initiative.action.disabled";
            case NOT_IN_ENCOUNTER -> "initiative.action.not_in_encounter";
            case NOT_YOUR_TURN -> "initiative.action.not_your_turn";
            case NO_ACTION -> "initiative.action.no_action";
            case NO_BONUS_ACTION -> "initiative.action.no_bonus_action";
            case NO_REACTION -> "initiative.action.no_reaction";
            case NO_MOVEMENT -> "initiative.action.no_movement";
            case INVALID_TARGET -> "initiative.action.invalid_target";
            case OUT_OF_REACH -> "initiative.action.out_of_reach";
            case MISSING_ITEM -> "initiative.action.missing_item";
            case HANDS_FULL -> "initiative.action.hands_full";
            case TOTAL_COVER -> "initiative.action.total_cover";
            case INACTIVE -> "initiative.action.inactive";
        };
    }
}
