package me.redteapot.rebot.frontend;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.*;
import me.redteapot.rebot.commands.CreateExchangeCommand;
import me.redteapot.rebot.commands.HelpCommand;
import me.redteapot.rebot.commands.StopCommand;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.NamedArgument;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.annotations.Permissions;
import me.redteapot.rebot.frontend.arguments.Identifier;
import me.redteapot.rebot.frontend.arguments.UserMention;
import me.redteapot.rebot.frontend.exceptions.InvalidArgumentNameException;
import me.redteapot.rebot.frontend.exceptions.NamedArgumentsNotProvidedException;
import me.redteapot.rebot.frontend.exceptions.ReaderException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static me.redteapot.rebot.Checks.*;

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
        register(CreateExchangeCommand.class);

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

        log.debug("Got a command: '{}', {}", message, reader.getPosition());

        reader.skip(Character::isWhitespace);

        String commandName = reader.read(Chars::isAsciiIdentifier);
        if (commandName.isBlank()) {
            Markdown markdown = new Markdown("Unknown command or wrong syntax:");
            markdown.code(Strings.comment(reader.getMessage(),
                reader.getPosition(),
                reader.getMessage().length(),
                10));
            context.respond(markdown);
            return;
        }

        if (!commands.containsKey(commandName)) {
            context.respond("Unknown command: `{}`.", commandName);
            return;
        }

        reader.skip(Character::isWhitespace);

        try {
            execute(reader, context, commands.get(commandName));
        } catch (CommandException e) {
            Markdown response = new Markdown();
            response.line("There was an error while executing your command:");
            response.concat(e.describe());
            context.respond(response);
        } catch (Exception e) {
            log.error("Exception during command execution", e);
            context.respond("Sorry, there was an internal error while executing the command.");
        }
    }

    private void execute(MessageReader reader,
                         CommandContext context,
                         CommandInfo commandInfo) throws Exception {
        if (!isUserAllowedToRun(commandInfo, context)) {
            context.respond("You are not allowed to run this command.");
            return;
        }

        Command command = commandInfo.getClazz().getConstructor().newInstance();
        command.context = context;

        fillOrderedArguments(command, commandInfo, reader);
        reader.skip(Character::isWhitespace);
        fillNamedArguments(command, commandInfo, reader);
        reader.skip(Character::isWhitespace);

        if (reader.canRead()) {
            Markdown markdown = new Markdown();
            markdown.code(Strings.comment(reader.getMessage(),
                "There are unexpected characters at the end of the command.",
                reader.getPosition(), reader.getMessage().length(), 10));
            context.respond(markdown);
            return;
        }

        command.execute();
    }

    private boolean isUserAllowedToRun(CommandInfo commandInfo, CommandContext context) {
        Member author = context.getMessage().getAuthorAsMember().block();
        ensure(author != null, "Message author is null");

        switch (commandInfo.getPermissions()) {
            case GENERAL:
                return true;
            case SERVER_ADMIN:
                // TODO Maybe check a configured Discord role
                PermissionSet authorPermissions = author.getBasePermissions().block();
                ensure(authorPermissions != null, "Author permissions is null");
                return authorPermissions.contains(Permission.ADMINISTRATOR);
            case BOT_OWNER:
                return config.getOwners().contains(author.getTag());
            default:
                return unreachable("Not all permissions are checked");
        }
    }

    private void fillOrderedArguments(Command command,
                                      CommandInfo commandInfo,
                                      MessageReader reader) throws Exception {
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
        }
    }

    private void fillNamedArguments(Command command,
                                    CommandInfo commandInfo,
                                    MessageReader reader) throws Exception {
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
                throw new InvalidArgumentNameException(name);
            }

            reader.skip(Character::isWhitespace);
            reader.expect('=');
            reader.skip(Character::isWhitespace);
            NamedArgumentInfo info = commandInfo.getNamedArguments().get(name);
            @SuppressWarnings("rawtypes")
            ArgumentParser parser = info.getParser().getConstructor().newInstance();

            info.getField().set(command, parser.parse(reader));
            unfilledNamedArguments.remove(name);

            reader.skip(Character::isWhitespace);
        }

        Set<String> unfilledRequiredArguments = unfilledNamedArguments
            .stream()
            .filter(arg -> !commandInfo.getNamedArguments().get(arg).optional)
            .collect(Collectors.toSet());

        if (!unfilledRequiredArguments.isEmpty()) {
            throw new NamedArgumentsNotProvidedException(unfilledRequiredArguments);
        }
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
        require(Arrays.stream(command.getDeclaredFields())
                .filter(f -> f.getAnnotation(OrderedArgument.class) != null || f.getAnnotation(NamedArgument.class) != null)
                .allMatch(f -> Modifier.isPublic(f.getModifiers())),
            "Command argument fields must be all public");

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
        private final Permissions permissions;

        private CommandInfo(Class<? extends Command> clazz) {
            this.clazz = clazz;
            this.permissions = clazz.getAnnotation(BotCommand.class).permissions();

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
