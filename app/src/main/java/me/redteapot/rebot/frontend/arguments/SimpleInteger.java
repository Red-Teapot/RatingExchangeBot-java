package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.frontend.MessageReader;
import me.redteapot.rebot.frontend.MessageReader.ReaderException;
import me.redteapot.rebot.frontend.MessageReader.ReaderUnexpectedCharException;

public class SimpleInteger implements ArgumentParser<Integer> {
    @Override
    public Integer parse(MessageReader reader) throws ReaderException {
        final boolean negative = reader.optional('-');
        final String string = reader.read(Chars::isAsciiDigit);
        if (string.isEmpty()) {
            throw new ReaderUnexpectedCharException(reader.getMessage(), reader.getPosition());
        }
        int result = Integer.parseInt(string);
        if (negative) {
            result *= -1;
        }
        return result;
    }
}
