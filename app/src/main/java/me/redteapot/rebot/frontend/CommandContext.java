package me.redteapot.rebot.frontend;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.Getter;
import me.redteapot.rebot.AssignmentScheduler;
import me.redteapot.rebot.Config;
import me.redteapot.rebot.Markdown;

import static me.redteapot.rebot.formatting.Formatter.format;

public class CommandContext {
    @Getter
    private final GatewayDiscordClient client;
    @Getter
    private final MessageCreateEvent event;
    @Getter
    private final Message message;
    @Getter
    private final MessageChannel channel;
    @Getter
    private final AssignmentScheduler scheduler;
    @Getter
    private final CommandDispatcher dispatcher;
    @Getter
    private final Config config;

    public CommandContext(GatewayDiscordClient client, MessageCreateEvent event, AssignmentScheduler scheduler, CommandDispatcher dispatcher, Config config) {
        this.client = client;
        this.event = event;
        this.message = event.getMessage();
        this.channel = event.getMessage().getChannel().block();
        this.scheduler = scheduler;
        this.dispatcher = dispatcher;
        this.config = config;
    }

    public void respond(String response, Object... args) {
        String formatted = format(response, args);
        String fixedResponse = message.getAuthor()
            .map(user -> user.getMention() + " " + formatted)
            .orElse(response);

        channel.createMessage(spec -> {
            // TODO Add message reference
            spec.setContent(fixedResponse);
        }).block();
    }

    public void respond(Markdown response) {
        respond(response.toString());
    }
}
