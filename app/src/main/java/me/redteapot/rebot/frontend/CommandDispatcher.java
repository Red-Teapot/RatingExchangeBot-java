package me.redteapot.rebot.frontend;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.Chars;
import me.redteapot.rebot.Config;
import me.redteapot.rebot.Strings;
import me.redteapot.rebot.commands.HelpCommand;
import me.redteapot.rebot.commands.StopCommand;
import me.redteapot.rebot.frontend.MessageReader.ReaderException;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.NamedArgument;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.arguments.Identifier;
import me.redteapot.rebot.frontend.arguments.UserMention;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static me.redteapot.rebot.Checks.require;

@Slf4j
public class CommandDispatcher {
    private final Config config;
    private final GatewayDiscordClient client;
    private final Map<String, CommandInfo> commands = new HashMap<>();
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
        } catch (ReaderException e) {
            StringBuilder response = new StringBuilder();
            response.append("There was an error while reading your command:\n");
            response.append(e.markdown());
            context.respond(response.toString());
        } catch (Exception e) {
            log.error("Exception during command execution", e);
            context.respond("Sorry, there was an internal error while executing the command.");
        }
    }

    private void execute(MessageReader reader,
                         CommandContext context,
                         CommandInfo commandInfo) throws Exception {
        Command command = commandInfo.getClazz().getConstructor().newInstance();
        command.context = context;

        for (OrderedArgumentInfo info : commandInfo.getOrderedArguments()) {
            reader.skip(Character::isWhitespace);
            final int position = reader.getPosition();
            @SuppressWarnings("rawtypes")
            ArgumentParser parser = info.getParser().getConstructor().newInstance();
            try {
                info.getField().set(command, parser.parse(reader));
            } catch (ReaderException e) {
                if (info.isOptional()) {
                    reader.rewind(position);
                    break;
                } else {
                    throw e;
                }
            }
            reader.skip(Character::isWhitespace);
        }
        reader.skip(Character::isWhitespace);

        Set<String> unfilledNamedArguments = new HashSet<>(commandInfo.getNamedArguments().keySet());
        while (!unfilledNamedArguments.isEmpty()) {
            int position = reader.getPosition();
            String name;
            try {
                name = new Identifier().parse(reader);
            } catch (ReaderException e) {
                reader.rewind(position);
                break;
            }
            if (!unfilledNamedArguments.contains(name)) {
                context.respond("Duplicate/unknown argument name: " + name);
                return;
            }
            reader.expect('=');
            NamedArgumentInfo info = commandInfo.getNamedArguments().get(name);
            position = reader.getPosition();
            @SuppressWarnings("rawtypes")
            ArgumentParser parser = info.getParser().getConstructor().newInstance();
            try {
                info.getField().set(command, parser.parse(reader));
                unfilledNamedArguments.remove(name);
            } catch (ReaderException e) {
                if (info.isOptional()) {
                    reader.rewind(position);
                    break;
                } else {
                    throw e;
                }
            }
            reader.skip(Character::isWhitespace);
        }

        reader.skip(Character::isWhitespace);

        if (reader.canRead()) {
            StringBuilder response = new StringBuilder();
            response.append("There are unexpected characters at the end of the command. Please fix it.\n");
            response.append("```");
            response.append(Strings.comment(reader.getMessage(), "Here", reader.getPosition(), reader.getMessage().length(), 10));
            response.append("```");
            context.respond(response.toString());
            return;
        }

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

        commands.put(name, new CommandInfo(command));

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

    @Data
    private static class CommandInfo {
        private final Class<? extends Command> clazz;
        private final List<OrderedArgumentInfo> orderedArguments;
        private final Map<String, NamedArgumentInfo> namedArguments;

        private CommandInfo(Class<? extends Command> clazz) {
            this.clazz = clazz;

            this.orderedArguments = Arrays.stream(clazz.getFields())
                .filter(f -> f.getAnnotation(OrderedArgument.class) != null)
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(OrderedArgument.class).order()))
                .map(field -> {
                    OrderedArgument annotation = field.getAnnotation(OrderedArgument.class);

                    return new OrderedArgumentInfo(
                        annotation.order(),
                        annotation.type(),
                        annotation.optional(),
                        field);
                })
                .collect(Collectors.toList());

            boolean optional = false;
            for (OrderedArgumentInfo info : orderedArguments) {
                require(!optional || info.isOptional(),
                    "Non-optional arguments are not allowed following optional ones: {}", clazz);

                optional = optional || info.isOptional();
            }

            this.namedArguments = new HashMap<>();
            Arrays.stream(clazz.getFields())
                .filter(f -> f.getAnnotation(NamedArgument.class) != null)
                .forEach(field -> {
                    NamedArgument annotation = field.getAnnotation(NamedArgument.class);

                    require(annotation.name().chars().allMatch(c -> Chars.isAsciiIdentifier((char) c)),
                        "Invalid keyword argument name: {}", annotation.name());

                    namedArguments.put(annotation.name(), new NamedArgumentInfo(
                        annotation.name(),
                        annotation.type(),
                        annotation.optional(),
                        field
                    ));
                });
        }
    }

    @Data
    @AllArgsConstructor
    private static class OrderedArgumentInfo {
        private final int order;
        private final Class<? extends ArgumentParser> parser;
        private final boolean optional;
        private final Field field;
    }

    @Data
    @AllArgsConstructor
    private static class NamedArgumentInfo {
        private final String name;
        private final Class<? extends ArgumentParser> parser;
        private final boolean optional;
        private final Field field;
    }
}
