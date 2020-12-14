package me.redteapot.rebot;

import com.moandjiezana.toml.Toml;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.frontend.CommandDispatcher;

import java.io.File;

import static me.redteapot.rebot.Checks.ensure;

@Slf4j
public class REBot {
    public static void main(String[] args) {
        if (args.length != 1) {
            log.error("No config file provided!");
            return;
        }

        Config config = new Toml().read(new File(args[0])).to(Config.class);
        ensure(config != null, "Config object is null");

        log.debug("Bot prefix: '{}'", config.getPrefix());

        final GatewayDiscordClient client = DiscordClientBuilder
            .create(config.getDiscord().getToken()).build().login().block();
        ensure(client != null, "Discord client is null");
        log.info("Connected");

        final CommandDispatcher commandDispatcher = new CommandDispatcher(config, client);

        client.onDisconnect().block();
        log.info("Stopped");
    }
}
