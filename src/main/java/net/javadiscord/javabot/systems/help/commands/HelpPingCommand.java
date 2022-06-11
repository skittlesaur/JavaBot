package net.javadiscord.javabot.systems.help.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.systems.help.HelpChannelManager;
import net.javadiscord.javabot.systems.help.model.ChannelReservation;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handler for the /help-ping command that allows users to occasionally ping
 * helpers.
 */
public class HelpPingCommand implements SlashCommand {
	private static final String WRONG_CHANNEL_MSG = "This command can only be used in **reserved help channels**.";
	private static final long CACHE_CLEANUP_DELAY = 60L;

	private final Map<Member, Long> lastPingTimes;

	/**
	 * Constructor that initializes a scheduled cache cleanup for this
	 * command's built-in {@link HelpPingCommand#lastPingTimes} cache.
	 */
	public HelpPingCommand() {
		lastPingTimes = new ConcurrentHashMap<>();
		Bot.asyncPool.scheduleWithFixedDelay(this::cleanTimeoutCache, CACHE_CLEANUP_DELAY, CACHE_CLEANUP_DELAY, TimeUnit.SECONDS);
	}

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		Guild guild = event.getGuild();
		if (guild == null) return Responses.warning(event, WRONG_CHANNEL_MSG);
		GuildConfig config = Bot.config.get(guild);
		var channelManager = new HelpChannelManager(config.getHelp());
		if (channelManager.isReserved(event.getTextChannel())) {
			Optional<ChannelReservation> optionalReservation = channelManager.getReservationForChannel(event.getTextChannel().getIdLong());
			if (optionalReservation.isEmpty()) {
				return Responses.warning(event, "Could not fetch the channel reservation.");
			}
			ChannelReservation reservation = optionalReservation.get();
			Member member = event.getMember();
			if (member == null) {
				return Responses.warning(event, "No member information was available for this event.");
			}
			if (isHelpPingForbiddenForMember(reservation, member, config)) {
				return Responses.warning(event, "Sorry, but only the person who reserved this channel, or staff and helpers, may use this command.");
			}
			if (isHelpPingTimeoutElapsed(member, config)) {
				lastPingTimes.put(event.getMember(), System.currentTimeMillis());
				Role role = channelManager.getConfig().getHelpPingRole();
				event.getChannel().sendMessage(role.getAsMention())
						.allowedMentions(EnumSet.of(Message.MentionType.ROLE))
						.setEmbeds(this.buildAuthorEmbed(event.getUser()))
						.queue();
				return event.replyFormat("Successfully pinged " + role.getAsMention()).setEphemeral(true);
			} else {
				return Responses.warning(event, "Sorry, but you can only use this command occasionally. Please try again later.");
			}
		} else {
			return Responses.warning(event, WRONG_CHANNEL_MSG);
		}
	}

	private MessageEmbed buildAuthorEmbed(User author) {
		return new EmbedBuilder()
				.setTitle("Requested by " + author.getAsTag())
				.build();
	}

	/**
	 * Determines if a user is forbidden from sending a help-ping command due
	 * to their status in the server.
	 * @param reservation The channel reservation for the channel they're
	 *                    trying to send the command in.
	 * @param member The member.
	 * @param config The guild config.
	 * @return True if the user is forbidden from sending the command.
	 */
	private boolean isHelpPingForbiddenForMember(ChannelReservation reservation, Member member, GuildConfig config) {
		Set<Role> allowedRoles = Set.of(config.getModeration().getStaffRole(), config.getHelp().getHelperRole());
		return !(
				reservation.getUserId() == member.getUser().getIdLong() ||
				member.getRoles().stream().anyMatch(allowedRoles::contains) ||
				member.isOwner()
		);
	}

	/**
	 * Determines if the user's timeout has elapsed (or doesn't exist), which
	 * implies that it's fine for the user to send the command.
	 * @param member The member.
	 * @param config The guild config.
	 * @return True if the user's timeout has elapsed or doesn't exist, or
	 * false if the user should NOT send the command because of their timeout.
	 */
	private boolean isHelpPingTimeoutElapsed(Member member, GuildConfig config) {
		Long lastPing = lastPingTimes.get(member);
		return lastPing == null ||
				lastPing + config.getHelp().getHelpPingTimeoutSeconds() * 1000L < System.currentTimeMillis();
	}

	/**
	 * Method that cleans out any entries from the list of last ping times if
	 * their timeout is no longer valid.
	 */
	private void cleanTimeoutCache() {
		// Find the list of members whose last ping time was old enough that they should be removed from the cache.
		var membersToRemove = lastPingTimes.entrySet().stream().filter(entry -> {
			var config = Bot.config.get(entry.getKey().getGuild()).getHelp();
			long timeoutMillis = config.getHelpPingTimeoutSeconds() * 1000L;
			return entry.getValue() + timeoutMillis < System.currentTimeMillis();
		}).map(Map.Entry::getKey).toList();
		// Remove each member from the map.
		for (var member : membersToRemove) {
			lastPingTimes.remove(member);
		}
	}
}
