package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.reading.MessageReader;
import me.redteapot.rebot.reading.ReaderException;

public class SimpleInteger implements ArgumentParser<Integer> {
    @Override
    public Integer parse(MessageReader reader) throws ReaderException {
        final boolean negative = reader.optional('-');
        final String string = reader.readNonEmpty(Chars::isAsciiDigit, "digit");
        int result = Integer.parseInt(string);
        if (negative) {
            result *= -1;
        }
        return result;
    }
}
