package me.redteapot.rebot.frontend;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.Chars;
import me.redteapot.rebot.Config;
import me.redteapot.rebot.commands.HelpCommand;
import me.redteapot.rebot.commands.StopCommand;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.arguments.UserMention;

import java.util.HashMap;
import java.util.Map;

import static me.redteapot.rebot.Checks.require;

@Slf4j
public class CommandDispatcher {
    private final Config config;
    private final GatewayDiscordClient client;
    private final Map<String, Class<? extends Command>> commands = new HashMap<>();
    private final Snowflake selfID;

    public CommandDispatcher(Config config, GatewayDiscordClient client) {
        this.config = config;
        this.client = client;
        this.selfID = client.getSelfId();

        register(HelpCommand.class);
        register(StopCommand.class);

        client.on(MessageCreateEvent.class).subscribe(this::onMessage);

        log.debug("Created command dispatcher");
    }

    private void onMessage(MessageCreateEvent evt) {
        if (evt.getMember().filter(m -> m.getId().equals(selfID)).isPresent()) {
            return;
        }

        String message = evt.getMessage().getContent();
        MessageReader reader = new MessageReader(message);
        CommandContext context = new CommandContext(client, evt);
        reader.skip(Character::isWhitespace);

        if (!checkPrefix(reader)) {
            return;
        }

        log.debug("Got a command: '{}'", evt.getMessage().getContent());

        reader.skip(Character::isWhitespace);

        String commandName = reader.read(Chars::isAsciiIdentifier);
        if (commandName.isBlank()) {
            context.respond("I guess there should be a command, but there is none. Ignoring.");
            return;
        }

        if (!commands.containsKey(commandName)) {
            context.respond("Unknown command, please fix it and try again.");
            return;
        }

        reader.skip(Character::isWhitespace);

        try {
            execute(reader, context, commands.get(commandName));
        } catch (Exception e) {
            log.error("Exception during command execution", e);
            context.respond("Sorry, there was an internal error while executing the command.");
        }
    }

    private <T extends Command> void execute(MessageReader reader,
                                             CommandContext context,
                                             Class<T> commandClass) throws Exception {
        T command = commandClass.getConstructor().newInstance();
        command.context = context;

        // TODO Parse arguments

        command.execute();
    }

    private void register(Class<? extends Command> command) {
        BotCommand commandAnnotation = command.getAnnotation(BotCommand.class);
        require(commandAnnotation != null,
            "{} must be annotated with @BotCommand to be registered",
            command);
        String name = commandAnnotation.name();
        require(!name.isEmpty(), "Command name is empty for {}", command);
        require(name.chars().allMatch(c -> Chars.isAsciiIdentifier((char) c)),
            "Invalid command name: {} for {}", name, command);
        require(!commands.containsKey(name),
            "Duplicate command name: {} for {}", name, command);
        commands.put(name, command);
        log.debug("Registered '{}' command with class {}", name, command);
    }

    private boolean checkPrefix(MessageReader reader) {
        if (config.getPrefix().isBlank()) {
            try {
                Snowflake firstMention = new UserMention().parse(reader);
                return firstMention.equals(selfID);
            } catch (Exception e) {
                return false;
            }
        } else {
            return reader.optional(config.getPrefix());
        }
    }
}
