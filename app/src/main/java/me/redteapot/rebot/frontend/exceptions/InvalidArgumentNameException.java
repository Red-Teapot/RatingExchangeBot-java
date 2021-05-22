package me.redteapot.rebot.frontend.exceptions;

import me.redteapot.rebot.CommandException;
import me.redteapot.rebot.Markdown;

public class InvalidArgumentNameException extends CommandException {
    private final String name;

    public InvalidArgumentNameException(String name) {
        this.name = name;
    }

    @Override
    public Markdown describe() {
        return Markdown.md("Duplicate or invalid argument name: `{}`", name);
    }
}
