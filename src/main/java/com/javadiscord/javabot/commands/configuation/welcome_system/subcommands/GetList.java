package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.events.UserJoin;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.io.ByteArrayInputStream;

public class GetList implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        event.deferReply().queue();
        Database db = new Database();

        String status;
        if (db.getConfigBoolean(event.getGuild(), "welcome_system.welcome_status")) status = "enabled";
        else status = "disabled";

        var eb = new EmbedBuilder()
                .setTitle("Welcome System Configuration")
                .setColor(Constants.GRAY)

                .addField("Image", "Width, Height: `" + db.getConfigString(event.getGuild(), "welcome_system.image.imgW") +
                        "`, `" + db.getConfigString(event.getGuild(), "welcome_system.image.imgH") +
                        "`\n[Overlay](" + db.getConfigString(event.getGuild(), "welcome_system.image.overlayURL") +
                        "), [Background](" + db.getConfigString(event.getGuild(), "welcome_system.image.bgURL") + ")", false)

                .addField("Color", "Primary Color: `#" + Integer.toHexString(db.getConfigInt(event.getGuild(), "welcome_system.image.primCol")) +
                        "`\nSecondary Color: `#" + Integer.toHexString(db.getConfigInt(event.getGuild(), "welcome_system.image.secCol")) + "`", true)

                .addField("Avatar Image", "Width, Height: `" + db.getConfigInt(event.getGuild(), "welcome_system.image.avatar.avW") +
                        "`,`" + db.getConfigInt(event.getGuild(), "welcome_system.image.avatar.avH") +
                        "`\nX, Y: `" + db.getConfigInt(event.getGuild(), "welcome_system.image.avatar.avX") +
                        "`, `" + db.getConfigInt(event.getGuild(), "welcome_system.image.avatar.avY") + "`", true)

                .addField("Messages", "Join: `" + db.getConfigString(event.getGuild(), "welcome_system.join_msg") +
                        "`\nLeave: `" + db.getConfigString(event.getGuild(), "welcome_system.leave_msg") + "`", false)

                .addField("Channel", db.getConfigChannelAsMention(event.getGuild(), "welcome_system.welcome_cid"), true)
                .addField("Status", "``" + status + "``", true);

        try {
            event.getHook().editOriginalEmbeds(eb.build()).addFile(new ByteArrayInputStream(new UserJoin().generateImage(event, false, false)), event.getMember().getId() + ".png").queue();
        } catch (Exception e) { e.printStackTrace(); }
    }
}