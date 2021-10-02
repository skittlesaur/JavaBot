package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModerationConfig extends GuildConfigItem {
	/**
	 * The id of the channel where user-submitted reports are relayed.
	 */
	private long reportChannelId;

	/**
	 * The id of the channel for internal moderation log messages.
	 */
	private long logChannelId;

	/**
	 * The id of the channel where users can submit suggestions.
	 */
	private long suggestionChannelId;

	/**
	 * The id of the role that muted users have.
	 */
	private long muteRoleId;

	/**
	 * The id of the role that all staff have.
	 */
	private long staffRoleId;

	/**
	 * The id of the role that staff application reviewers have.
	 */
	private long staffApplicationReviewRoleId;

	/**
	 * The maximum number of messages to purge, at a time.
	 */
	private int purgeMaxMessageCount = 1000;

	public TextChannel getReportChannel() {
		return this.getGuild().getTextChannelById(this.reportChannelId);
	}

	public TextChannel getLogChannel() {
		return this.getGuild().getTextChannelById(this.logChannelId);
	}

	public TextChannel getSuggestionChannel() {
		return this.getGuild().getTextChannelById(this.suggestionChannelId);
	}

	public Role getMuteRole() {
		return this.getGuild().getRoleById(this.muteRoleId);
	}

	public Role getStaffRole() {
		return this.getGuild().getRoleById(this.staffRoleId);
	}

	public Role getStaffApplicationReviewRole() {
		return this.getGuild().getRoleById(this.staffApplicationReviewRoleId);
	}
}
