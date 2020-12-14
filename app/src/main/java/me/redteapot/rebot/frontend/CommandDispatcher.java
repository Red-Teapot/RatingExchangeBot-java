package me.redteapot.rebot.frontend;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.Chars;
import me.redteapot.rebot.Config;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CommandDispatcher {
    private final Config config;
    private final GatewayDiscordClient client;
    private final Map<String, Class<? extends Object>> commands = new HashMap<>();
    private final Snowflake selfID;

    public CommandDispatcher(Config config, GatewayDiscordClient client) {
        this.config = config;
        this.client = client;
        this.selfID = client.getSelfId();

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
        Parsers.skipWhitespace(reader);

        if (!checkPrefix(reader)) {
            return;
        }

        log.debug("Got a command: '{}'", evt.getMessage().getContent());

        Parsers.skipWhitespace(reader);

        String commandName = reader.read(Chars::isAsciiIdentifier);
        if (commandName.isBlank()) {
            context.respond("I guess there should be a command, but there is none. Ignoring.");
            return;
        }

        if (!commands.containsKey(commandName)) {
            context.respond("Unknown command, please fix it and try again.");
            return;
        }

        Parsers.skipWhitespace(reader);

        // TODO
    }

    private boolean checkPrefix(MessageReader reader) {
        if (config.getPrefix().isBlank()) {
            try {
                Snowflake firstMention = Parsers.readUserMention(reader);
                return firstMention.equals(selfID);
            } catch (Exception e) {
                return false;
            }
        } else {
            return reader.optional(config.getPrefix());
        }
    }
}
