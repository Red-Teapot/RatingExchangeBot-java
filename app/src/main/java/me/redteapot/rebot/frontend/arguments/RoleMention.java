package me.redteapot.rebot.frontend.arguments;

public class RoleMention extends Mention {
    public RoleMention() {
        super(reader -> reader.expect("@&"));
    }
}
