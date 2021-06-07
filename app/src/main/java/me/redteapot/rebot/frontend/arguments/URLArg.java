package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.reading.MessageReader;
import me.redteapot.rebot.reading.ReaderException;
import me.redteapot.rebot.reading.UnexpectedStringException;

import java.net.MalformedURLException;
import java.net.URL;

public class URLArg implements ArgumentParser<URL> {
    @Override
    public URL parse(MessageReader reader) throws ReaderException {
        reader.skip(Character::isWhitespace);
        reader.optional('<');
        int start = reader.getPosition();
        String urlStr = reader.read(c -> !Character.isWhitespace(c) && c != '>');
        reader.optional('>');
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new UnexpectedStringException(reader.getMessage(), start, reader.getPosition());
        }
    }
}
