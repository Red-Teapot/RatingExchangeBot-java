package me.redteapot.rebot.frontend.exceptions;

import me.redteapot.rebot.CommandException;
import me.redteapot.rebot.Markdown;

import java.util.Set;
import java.util.stream.Collectors;

public class NamedArgumentsNotProvidedException extends CommandException {
    private final Set<String> argumentNames;

    public NamedArgumentsNotProvidedException(Set<String> argumentNames) {
        this.argumentNames = argumentNames;
    }

    @Override
    public Markdown describe() {
        Set<String> quotedNames = argumentNames.stream().map(s -> "`" + s + "`").collect(Collectors.toSet());
        return Markdown.md(
            "Required named arguments are not provided: {}",
            String.join(", ", quotedNames));
    }
}
