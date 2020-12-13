package me.redteapot.rebot.frontend;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.Config;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CommandDispatcher {
    private final Config config;
    private final GatewayDiscordClient client;
    private final Map<String, Command> commands = new HashMap<>();
    private final Snowflake selfID;

    public CommandDispatcher(Config config, GatewayDiscordClient client) {
        this.config = config;
        this.client = client;
        this.selfID = client.getSelfId();

        log.debug("Created command dispatcher");

        client.on(MessageCreateEvent.class).subscribe(this::onMessage);
    }

    private void onMessage(MessageCreateEvent evt) {
        MessageParser parser = new MessageParser(evt.getMessage());
        parser.skipWhitespace();

        if (!checkPrefix(parser)) {
            return;
        }

        parser.skipWhitespace();

        String commandName = parser.readUnquotedString();

        log.debug("Got a command (probably): '{}', the name is '{}'", evt.getMessage().getContent(), commandName);

        if (commandName.equals("stop")) {
            client.logout().block();
        }
    }

    private boolean checkPrefix(MessageParser parser) {
        if (config.getPrefix().isBlank()) {
            try {
                Snowflake firstMention = parser.readUserMention();
                return firstMention.equals(selfID);
            } catch (MessageReader.ReaderException e) {
                return false;
            }
        } else {
            return parser.getReader().optional(config.getPrefix());
        }
    }
}
