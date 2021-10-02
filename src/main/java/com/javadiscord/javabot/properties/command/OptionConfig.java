package com.javadiscord.javabot.properties.command;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

/**
 * Simple DTO representing an option that can be given to a Discord slash
 * command or subcommand.
 */
@Data
public class OptionConfig {
	private String name;
	private String description;
	private String type;
	private boolean required;
	private ChoiceConfig[] choices;

	@Data
	public static class ChoiceConfig {
		private String name;
		private long value;
	}

	public OptionData toData() {
		var data = new OptionData(OptionType.valueOf(this.type.toUpperCase()), this.name, this.description, this.required);
		if (this.choices != null && this.choices.length > 0) {
			for (var c : this.choices) {
				data.addChoices(new Command.Choice(c.getName(), c.getValue()));
			}
		}
		return data;
	}

	@Override
	public String toString() {
		return "OptionConfig{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", type='" + type + '\'' +
			", required=" + required +
			'}';
	}

	public static OptionConfig fromData(OptionData data) {
		OptionConfig c = new OptionConfig();
		c.setName(data.getName());
		c.setDescription(data.getDescription());
		c.setType(data.getType().name());
		c.setRequired(data.isRequired());
		c.choices = null;
		if (!data.getChoices().isEmpty()) {
			c.choices = new ChoiceConfig[data.getChoices().size()];
			for (int i = 0; i < data.getChoices().size(); i++) {
				var choice = data.getChoices().get(i);
				c.choices[i] = new ChoiceConfig();
				c.choices[i].setName(choice.getName());
				c.choices[i].setValue(choice.getAsLong());
			}
		}
		return c;
	}
}
