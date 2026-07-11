package net.discordjug.javabot.systems.user_commands.format_code;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import org.jspecify.annotations.NonNull;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import xyz.dynxsty.dih4jda.interactions.components.StringSelectMenuHandler;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * Handles the interactive components on formatted code blocks: the delete-all button and the
 * language-selection dropdown. Both act on every message of a (possibly multi-message) block,
 * which is resolved via {@link LinkedMessages}.
 */
@AutoDetectableComponentHandler(FormatCodeInteractionHandler.COMPONENT_ID)
@RequiredArgsConstructor
public class FormatCodeInteractionHandler implements ButtonHandler, StringSelectMenuHandler {
	static final String COMPONENT_ID = "format-code";
	private final BotConfig botConfig;

	/**
	 * Builds the delete-all button placed on the last message of a code block.
	 *
	 * @param requesterID 		the id of the user allowed to delete the block
	 * @param total 			the number of messages making up the block
	 * @param firstMessageID	the id of first message to check update code block
	 * @return the delete-all button
	 */
	public static Button createDeleteAllButton(long requesterID, int total, long firstMessageID) {
		return Button.secondary(ComponentIdBuilder.build(COMPONENT_ID, requesterID, total,firstMessageID), "\uD83D\uDDD1\uFE0F");
	}

	@Override
	public void handleButton(ButtonInteractionEvent event, @NonNull Button button) {
		if (!isValid(event)) {
			return;
		}
		String[] id = ComponentIdBuilder.split(event.getComponentId());

		event.deferEdit().queue();

		LinkedMessages.resolveBefore(event.getMessage(), Integer.parseInt(id[2]), true,
				messages -> {
					if (messages.getLast().getIdLong() == Long.parseLong(id[3])) {
						event.getChannel().purgeMessages(messages);
					} else {
						Responses.error(event.getHook(), "The code block could not be deleted. The messages may have been deleted.").queue();
					}
				}, () -> Responses.error(event.getHook(), "Could not delete the code block").queue());
	}

	/**
	 * Builds the language-selection dropdown row for a code block.
	 *
	 * @param requesterId    the id of the user allowed to change the language
	 * @param total          the number of messages making up the block
	 * @param firstMessageID the id of first message to check update code block
	 * @return an action row containing the language dropdown
	 */
	public static ActionRow buildLanguageMenu(long requesterId, int total, long firstMessageID) {
		return ActionRow.of(languageMenu(ComponentIdBuilder.build(COMPONENT_ID, requesterId, total, firstMessageID)));
	}

	@Override
	public void handleStringSelectMenu(@NonNull StringSelectInteractionEvent event, @NonNull List<String> values) {
		if (!isValid(event)) {
			return;
		}

		String[] id = ComponentIdBuilder.split(event.getComponentId());
		Language language = Language.fromString(values.getFirst());

		event.deferEdit().queue();

		LinkedMessages.resolveBefore(event.getMessage(), Integer.parseInt(id[2]), false,
				messages -> {
					if (messages.getLast().getIdLong() == Long.parseLong(id[3])) {
						messages.forEach(message -> message.editMessage(withLanguage(message.getContentRaw(), language)).queue());
					} else {
						Responses.error(event.getHook(), "The code block could not be updated. The messages may have been deleted.").queue();
					}
				}, () -> Responses.error(event.getHook(), "Could not update the code block").queue());
	}

	private static StringSelectMenu languageMenu(String customId) {
		return StringSelectMenu.create(customId)
				.setPlaceholder("Change language")
				.addOptions(Arrays.stream(Language.values())
						.filter(language -> language != Language.UNKNOWN)
						.map(language -> SelectOption.of(language.getDisplayName(), language.name()))
						.toList())
				.build();
	}

	private boolean isValid(ComponentInteraction event) {
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		long requesterId = Long.parseLong(id[1]);

		Member member = event.getMember();
		if (member == null) {
			Responses.errorWithTitle(event, "Server Required", "This may only be used inside a server.").queue();
			return false;
		}
		if (member.getIdLong() != requesterId || !Checks.hasStaffRole(botConfig, member)) {
			Responses.errorWithTitle(event, "Access Denied", "You are not authorized to perform this action.").queue();
			return false;
		}

		return true;
	}

	/**
	 * Re-wraps a code-block message in a different language by swapping the tag on its opening fence,
	 * leaving the code itself untouched.
	 *
	 * @param content  the raw message content, expected to start with a fenced code block
	 * @param language the language to switch to
	 * @return the message content with its opening fence set to {@code language}
	 */
	private static String withLanguage(String content, Language language) {
		int firstLineEnd = content.indexOf('\n');
		return firstLineEnd < 0
				? content
				: "```" + language.getDiscordName() + content.substring(firstLineEnd);
	}
}
