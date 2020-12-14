package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.frontend.MessageReader;

public class Identifier implements ArgumentParser<String> {
    @Override
    public String parse(MessageReader reader) {
        return reader.read(Chars::isAsciiIdentifier);
    }
}
