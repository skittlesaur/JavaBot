package net.discordjug.javabot.systems.user_commands.format_code;

import net.dv8tion.jda.api.entities.Message;

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
	 * passes the bot's own messages to {@code onResolved}, or runs {@code onError} if it can't be
	 * safely resolved.
	 *
	 * @param triggerMessage the last message of the block (carries the component)
	 * @param total          the number of messages in the block
	 * @param onResolved     receives the bot's messages that make up the block
	 * @param onError        runs if the block can't be safely resolved
	 */
	static void resolveBefore(Message triggerMessage, int total, Consumer<List<Message>> onResolved, Runnable onError) {
		if (total <= 1) {
			verify(List.of(triggerMessage), total, onResolved, onError);
			return;
		}
		triggerMessage.getChannel().getHistoryBefore(triggerMessage.getIdLong(), total - 1).queue(history -> {
			List<Message> block = new ArrayList<>(history.getRetrievedHistory());
			block.add(triggerMessage);
			verify(block, total, onResolved, onError);
		});
	}

	/**
	 * Resolves the block of {@code total} messages sent after {@code anchorMessage} and passes the
	 * bot's own messages to {@code onResolved}, or runs {@code onError} if it can't be safely resolved.
	 *
	 * @param anchorMessage the message just before the block (carries the component)
	 * @param total         the number of messages in the block
	 * @param onResolved    receives the bot's messages that make up the block
	 * @param onError       runs if the block can't be safely resolved
	 */
	static void resolveAfter(Message anchorMessage, int total, Consumer<List<Message>> onResolved, Runnable onError) {
		anchorMessage.getChannel().getHistoryAfter(anchorMessage.getIdLong(), total).queue(history ->
				verify(history.getRetrievedHistory(), total, onResolved, onError));
	}

	private static void verify(List<Message> messages, int total, Consumer<List<Message>> onResolved, Runnable onError) {
		List<Message> own = onlyOwn(messages);
		boolean allCodeBlocks = own.stream().allMatch(message -> message.getContentRaw().startsWith("```"));
		if (own.size() != total || !allCodeBlocks) {
			onError.run();
			return;
		}
		onResolved.accept(own);
	}

	private static List<Message> onlyOwn(List<Message> messages) {
		return messages.stream()
				.filter(message -> message.getAuthor().getIdLong() == message.getJDA().getSelfUser().getIdLong())
				.toList();
	}
}
