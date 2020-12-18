package me.redteapot.rebot.commands;

import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.Permissions;

@BotCommand(name = "stop", permissions = Permissions.BOT_OWNER)
public class StopCommand extends Command {
    @Override
    public void execute() {
        context.respond("Stopping.");
        context.getClient().logout().block();
    }
}
