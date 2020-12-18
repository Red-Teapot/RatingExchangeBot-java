package me.redteapot.rebot.commands;

import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.annotations.Permissions;
import me.redteapot.rebot.frontend.arguments.Identifier;

@BotCommand(name = "help", permissions = Permissions.GENERAL)
public class HelpCommand extends Command {
    @OrderedArgument(order = 0, type = Identifier.class, optional = true)
    public String name;

    @Override
    public void execute() {
        context.respond("This should be a help command. The name is: " + name);
    }
}
