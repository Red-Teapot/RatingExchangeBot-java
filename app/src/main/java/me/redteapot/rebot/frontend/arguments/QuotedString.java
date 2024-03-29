package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.reading.MessageReader;
import me.redteapot.rebot.reading.ReaderException;
import me.redteapot.rebot.reading.UnexpectedEndOfMessageException;

public class QuotedString implements ArgumentParser<String> {
    @Override
    public String parse(MessageReader reader) throws ReaderException {
        char quote = reader.peek();
        reader.expect(Chars::isQuote, "quote");

        final StringBuilder result = new StringBuilder();
        boolean escape = false;
        while (reader.canRead()) {
            final char c = reader.read();

            if (c == quote) {
                if (escape) {
                    result.append(quote);
                } else {
                    return result.toString();
                }
                continue;
            }

            if (c == '\\') {
                if (escape) {
                    result.append('\\');
                } else {
                    escape = true;
                }
                continue;
            } else {
                escape = false;
            }

            result.append(c);
        }

        throw new UnexpectedEndOfMessageException(reader.getMessage(), reader.getPosition());
    }
}
