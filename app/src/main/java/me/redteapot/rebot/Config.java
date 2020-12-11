package me.redteapot.rebot;

import lombok.Data;

@Data
public class Config {
    private Discord discord;

    @Data
    public static class Discord {
        private String token;
    }
}
