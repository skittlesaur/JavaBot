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
	private LinkedMessages() {
	}

	/**
	 * Resolves a block of messages ending at {@code triggerMessage} and passes the
	 * bot's own messages to {@code onResolved}, ordered from newest to oldest.
	 * If {@code inclusive} is {@code true}, {@code triggerMessage} is included in
	 * the resolved block; otherwise, only the preceding {@code total} messages are
	 * considered. Runs {@code onError} if the block can't be safely resolved.
	 *
	 * @param triggerMessage the message marking the end of the block
	 * @param total          the number of messages to resolve
	 * @param inclusive      whether {@code triggerMessage} should be included in the resolved block
	 * @param onResolved     receives the bot's messages, ordered from newest to oldest
	 * @param onError        runs if the block can't be safely resolved
	 */
	static void resolveBefore(Message triggerMessage, int total, boolean inclusive,Consumer<List<Message>> onResolved, Runnable onError) {
		if (total <= 1 && inclusive) {
			verify(List.of(triggerMessage), total, onResolved, onError);
			return;
		}
		triggerMessage.getChannel().getHistoryBefore(triggerMessage.getIdLong(), inclusive?total-1 :total).queue(history -> {
			List<Message> block = new ArrayList<>(history.getRetrievedHistory());
			if (inclusive){
				block.addFirst(triggerMessage);
			}
			verify(block, total, onResolved, onError);
		});
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
