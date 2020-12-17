package me.redteapot.rebot.frontend;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.Getter;
import me.redteapot.rebot.Markdown;
import me.redteapot.rebot.Strings;

public class CommandContext {
    @Getter
    private final GatewayDiscordClient client;
    @Getter
    private final MessageCreateEvent event;
    @Getter
    private final Message message;
    @Getter
    private final MessageChannel channel;

    public CommandContext(GatewayDiscordClient client, MessageCreateEvent event) {
        this.client = client;
        this.event = event;
        this.message = event.getMessage();
        this.channel = event.getMessage().getChannel().block();
    }

    public void respond(String response, Object... args) {
        String formatted = Strings.format(response, args);
        String fixedResponse = message.getAuthor()
            .map(user -> user.getMention() + " " + formatted)
            .orElse(response);

        channel.createMessage(fixedResponse).block();
    }

    public void respond(Markdown response) {
        respond(response.toString());
    }
}
