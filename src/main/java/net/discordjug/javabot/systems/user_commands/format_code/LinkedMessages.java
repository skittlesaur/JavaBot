package net.discordjug.javabot.systems.user_commands.format_code;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Helper for acting on a block of related messages sent as a group — such as a piece of code split
 * across several Discord messages. Given one message of the block and the total count, it resolves
 * the whole block (only the bot's own messages) so a single interaction can delete or edit it.
 */
public class LinkedMessages {
	private LinkedMessages(){}

	/**
	 * Resolves the block ending at {@code triggerMessage} (walking back {@code total} messages) and
	 * passes the bot's own messages among them to {@code onResolved}.
	 *
	 * @param channel        the channel the block is in
	 * @param triggerMessage the last message of the block (carries the component)
	 * @param total          the number of messages in the block
	 * @param onResolved      receives the bot's messages that make up the block
	 */
	static void resolve(MessageChannel channel, Message triggerMessage, int total, Consumer<List<Message>> onResolved) {
		if (total <= 1) {
			onResolved.accept(List.of(triggerMessage));
			return;
		}
		channel.getHistoryBefore(triggerMessage.getIdLong(), total - 1).queue(history -> {
			List<Message> block = new ArrayList<>(history.getRetrievedHistory());
			block.add(triggerMessage);
			onResolved.accept(onlyOwn(channel, block));
		});
	}

	/**
	 * Resolves the block of {@code total} messages sent after {@code anchorMessage} and passes the
	 * bot's own messages among them to {@code onResolved}.
	 *
	 * @param channel       the channel the block is in
	 * @param anchorMessage the message just before the block (carries the component)
	 * @param total         the number of messages in the block
	 * @param onResolved     receives the bot's messages that make up the block
	 */
	static void resolveForward(MessageChannel channel, Message anchorMessage, int total, Consumer<List<Message>> onResolved) {
		channel.getHistoryAfter(anchorMessage.getIdLong(), total).queue(history ->
				onResolved.accept(onlyOwn(channel, history.getRetrievedHistory())));
	}

	private static List<Message> onlyOwn(MessageChannel channel, List<Message> messages) {
		long selfId = channel.getJDA().getSelfUser().getIdLong();
		return messages.stream()
				.filter(message -> message.getAuthor().getIdLong() == selfId)
				.toList();
	}
}
