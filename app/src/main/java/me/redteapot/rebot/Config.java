package me.redteapot.rebot;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Config {
    private Discord discord;

    private Database database;

    private String prefix = "";

    private List<String> owners = new ArrayList<>();

    @Data
    public static class Discord {
        private String token;
    }

    @Data
    public static class Database {
        private String file = "rebot.db";
    }
}
