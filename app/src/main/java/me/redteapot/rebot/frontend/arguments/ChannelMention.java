package me.redteapot.rebot.frontend.arguments;

public class ChannelMention extends Mention {
    public ChannelMention() {
        super(reader -> reader.expect('#'));
    }
}
