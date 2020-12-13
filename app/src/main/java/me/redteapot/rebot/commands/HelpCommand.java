package me.redteapot.rebot.commands;

import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.CommandContext;
import me.redteapot.rebot.frontend.annotations.BotCommand;

@BotCommand("help")
public class HelpCommand implements Command {
    @Override
    public void execute(CommandContext context) {
        context.respond("Help is not implemented yet :(");
    }
}
