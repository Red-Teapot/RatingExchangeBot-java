package me.redteapot.rebot.commands;

import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;

@BotCommand(name = "stop")
public class StopCommand extends Command {
    @Override
    public void execute() {
        context.respond("Stopping.");
        context.getClient().logout().block();
    }
}
