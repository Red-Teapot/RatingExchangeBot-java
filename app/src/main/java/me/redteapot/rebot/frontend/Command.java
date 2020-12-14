package me.redteapot.rebot.frontend;

public abstract class Command {
    protected CommandContext context;

    public abstract void execute();
}
