package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.reading.MessageReader;
import me.redteapot.rebot.reading.ReaderException;

public class Identifier implements ArgumentParser<String> {
    @Override
    public String parse(MessageReader reader) throws ReaderException {
        return reader.readNonEmpty(Chars::isAsciiIdentifier, "identifier");
    }
}
