package me.redteapot.rebot.frontend;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.Assertion;
import me.redteapot.rebot.Chars;
import me.redteapot.rebot.Config;
import me.redteapot.rebot.commands.HelpCommand;
import me.redteapot.rebot.commands.StopCommand;
import me.redteapot.rebot.frontend.annotations.BotCommand;

import java.util.HashMap;
import java.util.Map;

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

        MessageParser parser = new MessageParser(evt.getMessage());
        CommandContext context = new CommandContext(client, evt);
        parser.skipWhitespace();

        if (!checkPrefix(parser)) {
            return;
        }

        log.debug("Got a command: '{}'", evt.getMessage().getContent());

        parser.skipWhitespace();

        String commandName = parser.readUnquotedString(Chars::isAsciiIdentifier);
        if (commandName.isBlank()) {
            context.respond("I guess there should be a command, but there is none. Ignoring.");
            return;
        }

        if (!commands.containsKey(commandName)) {
            context.respond("Unknown command, please fix it and try again.");
            return;
        }

        // TODO
    }

    private void register(Class<? extends Command> cmd) {
        BotCommand cmdAnnotation = cmd.getAnnotation(BotCommand.class);
        Assertion.isTrue(cmdAnnotation != null, "No @BotCommand annotation on a bot command");
        final String name = cmdAnnotation.value();
        Assertion.isTrue(!name.isEmpty(), "Empty bot command name");
        Assertion.isTrue(name.chars().allMatch(c -> Chars.isAsciiIdentifier((char) c)), "Invalid bot command name: {}", name);
        commands.put(name, cmd);
    }

    private boolean checkPrefix(MessageParser parser) {
        if (config.getPrefix().isBlank()) {
            try {
                Snowflake firstMention = parser.readUserMention();
                return firstMention.equals(selfID);
            } catch (Exception e) {
                return false;
            }
        } else {
            return parser.getReader().optional(config.getPrefix());
        }
    }
}
