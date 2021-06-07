package me.redteapot.rebot.assigner;

import discord4j.common.util.Snowflake;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URL;
import java.util.List;

@Data
@AllArgsConstructor
public class Member {
    private Snowflake id;
    private List<URL> playedGames;

    public boolean isDummy() {
        return id == null;
    }
}
