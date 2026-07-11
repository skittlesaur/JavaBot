package net.discordjug.javabot.systems.user_commands.format_code;

import net.discordjug.javabot.util.*;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Shared sending logic for the code-formatting commands. Replies with the full code as a
 * downloadable file, then posts it as one or more ordered code-block messages that each respect
 * Discord's 2000-character limit.
 */
class FormatCodeDispatcher {

	/**
	 * The maximum number of code-block messages to post inline; longer code results in an error.
	 */
	private static final int MAX_MESSAGES = 5;

	/**
	 * Acknowledges the interaction by replying with the full code as a file, then posts the code as
	 * ordered code-block messages. Replies with an error instead if there is nothing to format.
	 *
	 * @param code   the code to send
	 * @param event  the interaction to reply to
	 * @param target the original message the code came from, used for the channel and the
	 *               "View Original" / delete buttons
	 */
	public static void sendCode(Code code, @Nonnull CommandInteraction event, Message target) {
		if (code.getContent().isBlank()) {
			Responses.errorWithTitle(event.getHook(), "404 Code not found", "There is no code to format in that message.").queue();
			return;
		}

		List<String> messages = code.toDiscordMessages();

		MessageChannel channel = target.getChannel();

		if (messages.size() > MAX_MESSAGES) {
			Responses.errorWithTitle(event.getHook(), "Code out of Bounds", "The formatted result is too large to send. Please provide a smaller code snippet or use a paste service instead."
			).queue();
			return;
		}

		sendChunksInOrder(channel, messages, 0, target, event, 0L);
	}


	private static void sendChunksInOrder(MessageChannel channel, List<String> messages, int index, Message target, @Nonnull CommandInteraction event, long firstMessageID) {
		if (index >= messages.size()) {
			event.getHook().deleteOriginal().queue(_ ->
				event.getHook().sendMessage("Your message has been sent. If needed, you can change the language used for syntax highlighting below.")
						.setEphemeral(true)
						.setComponents(FormatCodeInteractionHandler.buildLanguageMenu(event.getUser().getIdLong(), messages.size(), firstMessageID))
						.queue()
			);
			return;
		}
		MessageCreateAction action = channel.sendMessage(messages.get(index))
				.setAllowedMentions(List.of());

		if (index == messages.size() - 1) {
			if(messages.size() >1) {
				action.setComponents(buildMultiMessageActionRow(target, event.getUser().getIdLong(), messages.size(), firstMessageID));
			} else {
				action.setComponents(buildActionRow(target,event.getUser().getIdLong()));
			}
		}

		action.queue(sent -> sendChunksInOrder(channel, messages, index + 1, target, event, index == 0 ? sent.getIdLong() : firstMessageID));
	}

	@Contract("_,_,_,_ -> new")
	static @NotNull ActionRow buildMultiMessageActionRow(@NotNull Message target, long requesterId, int total, long firstMessageID) {
		return ActionRow.of(
				FormatCodeInteractionHandler.createDeleteAllButton(requesterId, total, firstMessageID ),
				Button.link(target.getJumpUrl(), "View Original"));
	}

	@Contract("_,_-> new")
	static @NotNull ActionRow buildActionRow(@NotNull Message target, long requesterId) {
		return ActionRow.of(
				InteractionUtils.createDeleteButton(requesterId),
				Button.link(target.getJumpUrl(), "View Original"));
	}
}
