package me.redteapot.rebot;

import com.moandjiezana.toml.Toml;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.frontend.CommandDispatcher;

import java.io.File;

import static me.redteapot.rebot.Checks.ensure;
import static me.redteapot.rebot.Checks.require;

@Slf4j
public class REBot {
    public static void main(String[] args) {
        require(args.length == 1, "No config file provided");

        Config config = new Toml().read(new File(args[0])).to(Config.class);
        ensure(config != null, "Config object is null");

        Database.init(config.getDatabase());

        log.debug("Bot prefix: '{}'", config.getPrefix());
        log.debug("Bot owners: {}", config.getOwners());

        final GatewayDiscordClient client = DiscordClientBuilder
            .create(config.getDiscord().getToken()).build().login().block();
        ensure(client != null, "Discord client is null");
        log.info("Connected");

        final AssignmentScheduler scheduler = new AssignmentScheduler(client, config);
        final CommandDispatcher commandDispatcher = new CommandDispatcher(config, client, scheduler);

        client.onDisconnect().block();
        scheduler.close();
        Database.close();
        log.info("Stopped");
    }
}
