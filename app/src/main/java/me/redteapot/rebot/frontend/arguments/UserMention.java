package me.redteapot.rebot.frontend.arguments;

public class UserMention extends Mention {
    public UserMention() {
        super(reader -> {
            reader.expect('@');
            reader.optional('!');
        });
    }
}
