package com.javadiscord.javabot.jam.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.jam.dao.JamRepository;
import com.javadiscord.javabot.jam.model.Jam;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Shows some basic information about the current Java Jam.
 */
@RequiredArgsConstructor
public class JamInfoSubcommand implements SlashCommandHandler {
	@Override
	public void handle(SlashCommandEvent event) {
		event.deferReply().queue();

		Jam jam;
		try {
			jam = this.fetchJam(event);
		} catch (Throwable t) {
			event.getHook().sendMessage(t.getMessage()).queue();
			return;
		}
		if (jam == null) {
			event.getHook().sendMessage("No Jam was found.").queue();
			return;
		}

		event.getJDA().retrieveUserById(jam.getStartedBy()).queue(user -> {
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("Jam Information")
					.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
					.addField("Id", Long.toString(jam.getId()), true)
					.addField("Name", jam.getFullName(), true)
					.addField("Created at", jam.getCreatedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy 'at' kk:mm:ss 'UTC'")), true)
					.addField("Started by", user.getAsTag(), true)
					.addField("Starts at", jam.getStartsAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy")), true)
					.addField("Current phase", jam.getCurrentPhase(), true);

			event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
		});
	}

	private Jam fetchJam(SlashCommandEvent event) {
		Jam jam;
		try {
			Connection con = Bot.dataSource.getConnection();
			JamRepository jamRepository = new JamRepository(con);
			OptionMapping idOption = event.getOption("id");
			if (idOption == null) {
				if (event.getGuild() == null) {
					throw new RuntimeException("Cannot find active Jam without Guild context.");
				}
				jam = jamRepository.getActiveJam(event.getGuild().getIdLong());
			} else {
				jam = jamRepository.getJam(idOption.getAsLong());
			}
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Error occurred while fetching the Jam info.", e);
		}
		return jam;
	}
}