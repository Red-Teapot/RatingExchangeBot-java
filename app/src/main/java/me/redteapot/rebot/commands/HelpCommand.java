package me.redteapot.rebot.commands;

import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.arguments.Identifier;

@BotCommand(name = "help")
public class HelpCommand extends Command {
    @OrderedArgument(type = Identifier.class, optional = true)
    private String name;

    @Override
    public void execute() {
        context.respond("This should be a help command. The name is: " + name);
    }
}
