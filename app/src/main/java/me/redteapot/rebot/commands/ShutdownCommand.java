package me.redteapot.rebot.commands;

import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.Permissions;

@BotCommand(name = "shutdown", permissions = Permissions.BOT_OWNER, allowedInDM = true)
public class ShutdownCommand extends Command {
    @Override
    public void execute() {
        context.respond("Shutting down.");
        context.getScheduler().close();
        context.getClient().logout().block();
        Database.close();
    }
}
