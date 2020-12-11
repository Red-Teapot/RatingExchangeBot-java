package me.redteapot.rebot;

import com.moandjiezana.toml.Toml;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class REBot {
    public static void main(String[] args) {
        if(args.length != 1) {
            log.error("No config file provided!");
            return;
        }

        Config config = new Toml().read(new File(args[0])).to(Config.class);

        final GatewayDiscordClient client = DiscordClientBuilder
            .create(config.getDiscord().getToken()).build().login().block();
        assert client != null;
        log.info("Connected");

        // TODO Run the bot

        client.onDisconnect().block();
        log.info("Stopped");
    }
}
