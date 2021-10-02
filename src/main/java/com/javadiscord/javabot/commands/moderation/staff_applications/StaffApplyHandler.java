package com.javadiscord.javabot.commands.moderation.staff_applications;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class handles users' staff application submissions.
 */
public class StaffApplyHandler implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		if (event.getGuild() == null) return Responses.warning(event, "This command can only be used in guilds.");
		var nameOption = event.getOption("name");
		var ageOption = event.getOption("age");
		var emailOption = event.getOption("email");
		var timezoneOption = event.getOption("timezone");
		var positionOption = event.getOption("position");
		var extraRemarksOption = event.getOption("extra-remarks");

		if (nameOption == null || ageOption == null || emailOption == null || timezoneOption == null || positionOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}

		String name = nameOption.getAsString();
		int age = 0;
		long ageLong = ageOption.getAsLong();
		String email = emailOption.getAsString();
		String timezone = timezoneOption.getAsString();
		long positionChoice = positionOption.getAsLong();
		String extraRemarks = extraRemarksOption == null ? null : extraRemarksOption.getAsString();

		// Validate the data and send a warning response if there are any invalid fields.
		List<String> validationMessages = new ArrayList<>();
		if (name.isBlank() || name.length() > 127) validationMessages.add("Name should not be empty, and must be less than 127 characters.");
		if (ageLong >= 13 && ageLong <= 127) {
			age = (int) ageLong;
		} else {
			validationMessages.add("Age should be between 13 and 127.");
		}
		if (email.isBlank() || email.length() > 255) validationMessages.add("Email should not be empty, and must be less than 255 characters.");
		if (timezone.isBlank() || timezone.length() > 32) validationMessages.add("Timezone should not be empty, and must be less than 32 characters.");
		Role chosenRole = event.getGuild().getRoleById(positionChoice);
		if (chosenRole == null) validationMessages.add("Unknown role.");
		if (extraRemarks != null && extraRemarks.length() > 1024) validationMessages.add("Extra remarks are limited to 1024 characters.");

		if (!validationMessages.isEmpty()) {
			String messages = validationMessages.stream().map(s -> "- " + s).collect(Collectors.joining("\n"));
			return Responses.warning(event, "Invalid Application", "Your application is invalid because:\n" + messages);
		}

		// Save the application.
		try (var con = Bot.dataSource.getConnection()) {
			var stmt = con.prepareStatement("INSERT INTO staff_applications (user_id, name, age, email, timezone, role_id, extra_remarks) VALUES (?, ?, ?, ?, ?, ?, ?)");
			stmt.setLong(1, event.getUser().getIdLong());
			stmt.setString(2, name);
			stmt.setInt(3, age);
			stmt.setString(4, email);
			stmt.setString(5, timezone);
			stmt.setLong(6, chosenRole.getIdLong());
			if (extraRemarks == null) {
				stmt.setNull(7, Types.VARCHAR);
			} else {
				stmt.setString(7, extraRemarks);
			}
			stmt.executeUpdate();
			stmt.close();
			return Responses.success(event, "Application Submitted", "Your application has been submitted.");
		} catch (SQLException e) {
			e.printStackTrace();
			return Responses.error(event, "An error occurred: " + e.getMessage());
		}
	}
}
