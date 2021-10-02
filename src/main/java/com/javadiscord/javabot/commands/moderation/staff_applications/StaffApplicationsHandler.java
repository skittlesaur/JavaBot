package com.javadiscord.javabot.commands.moderation.staff_applications;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.commands.Responses;

public class StaffApplicationsHandler extends DelegatingCommandHandler {
	public StaffApplicationsHandler() {
		addSubcommand("list", event -> Responses.warning(event, "Not implemented."));
		addSubcommand("handle", event -> Responses.warning(event, "Not implemented."));
	}
}
