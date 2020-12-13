package me.redteapot.rebot.commands;

import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.CommandContext;
import me.redteapot.rebot.frontend.annotations.BotCommand;

@BotCommand("stop")
public class StopCommand implements Command {
    @Override
    public void execute(CommandContext context) {
        context.respond("Stopping.");
        context.getClient().logout();
    }
}
