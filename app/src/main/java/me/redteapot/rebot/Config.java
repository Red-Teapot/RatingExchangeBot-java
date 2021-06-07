package me.redteapot.rebot;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static me.redteapot.rebot.Checks.ensure;

@Data
public class Config {
    private Discord discord;

    private String prefix = "";

    private String gameLinkRegex;
    private String gameLinkExample;

    private List<String> owners = new ArrayList<>();

    public void validate() {
        ensure(discord != null, "Discord config is null");
        ensure(gameLinkRegex != null, "No game link regex provided");
        ensure(gameLinkExample != null, "No game link example provided");
        Pattern gameLinkPattern = Pattern.compile(gameLinkRegex);
        ensure(gameLinkPattern.matcher(gameLinkExample).matches(), "Given game link example does not match the pattern");
    }

    @Data
    public static class Discord {
        private String token;
    }
}
