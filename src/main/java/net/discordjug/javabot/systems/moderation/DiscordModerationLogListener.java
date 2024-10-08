package net.discordjug.javabot.systems.moderation;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.EnumSet;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.util.ExceptionLogger;
import net.dv8tion.jda.api.audit.ActionType;

import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Logs moderative actions that did not use bot commands.
 */
@RequiredArgsConstructor
public class DiscordModerationLogListener extends ListenerAdapter{
	
	private final ModerationService moderationService;

	@Override
	public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
		AuditLogEntry entry = event.getEntry();
		long targetUserId = entry.getTargetIdLong();
		long moderatorUserId = entry.getUserIdLong();
		if (moderatorUserId == event.getJDA().getSelfUser().getIdLong()) {
			return;
		}
		if (!EnumSet.of(ActionType.KICK, ActionType.BAN, ActionType.UNBAN, ActionType.MEMBER_UPDATE).contains(entry.getType())) {
			return;
		}
		event.getJDA().retrieveUserById(targetUserId).queue(targetUser -> {
			event.getGuild().retrieveMemberById(moderatorUserId).queue(moderator -> {
				String reason = entry.getReason();
				if (reason == null) {
					reason = "<no reason provided>";
				}
				switch(entry.getType()) {
				case KICK -> moderationService.sendKickGuildNotification(targetUser, reason, moderator);
				case BAN -> moderationService.sendBanGuildNotification(targetUser, reason, moderator);
				case UNBAN -> moderationService.sendUnbanGuildNotification(targetUser, reason, moderator);
				case MEMBER_UPDATE -> {
					if (entry.getChanges().containsKey("communication_disabled_until")) {
						AuditLogChange change = entry.getChangeByKey("communication_disabled_until");
						if (change.getNewValue()!=null) {
							ZonedDateTime timeoutRemovalTimestamp = java.time.ZonedDateTime.parse(change.getNewValue());
							moderationService.sendTimeoutGuildNotification(targetUser, reason, moderator, Duration.between(ZonedDateTime.now(), timeoutRemovalTimestamp));
						}else {
							moderationService.sendRemoveTimeoutGuildNotification(targetUser, reason, moderator);
						}
					}
				}
				default -> ExceptionLogger.capture(new IllegalStateException("Unexpected audit log entry: "+entry.getType()), getClass().getName());
				}
			}, e -> ExceptionLogger.capture(e, getClass().getName()));
		}, e -> ExceptionLogger.capture(e, getClass().getName()));
	}
}
