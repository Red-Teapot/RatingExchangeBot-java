package me.redteapot.rebot;

import lombok.Data;

@Data
public class Config {
    private Discord discord;

    private String prefix = "";

    @Data
    public static class Discord {
        private String token;
    }
}
