package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.frontend.MessageReader;
import me.redteapot.rebot.frontend.MessageReader.ReaderUnexpectedCharException;

public class Identifier implements ArgumentParser<String> {
    @Override
    public String parse(MessageReader reader) throws ReaderUnexpectedCharException {
        String result = reader.read(Chars::isAsciiIdentifier);

        if (result.isBlank()) {
            throw new ReaderUnexpectedCharException(reader.getMessage(), reader.getPosition());
        }

        return result;
    }
}
