package me.redteapot.rebot.commands;

import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.arguments.Identifier;

@BotCommand(name = "help")
public class HelpCommand extends Command {
    @OrderedArgument(order = 0, type = Identifier.class, optional = false)
    public String name;

    @Override
    public void execute() {
        context.respond("This should be a help command. The name is: " + name);
    }
}
