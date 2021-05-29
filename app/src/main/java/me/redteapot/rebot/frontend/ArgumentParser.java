package me.redteapot.rebot.frontend;

import me.redteapot.rebot.reading.MessageReader;
import me.redteapot.rebot.reading.ReaderException;

public interface ArgumentParser<T> {
    T parse(MessageReader reader) throws ReaderException;
}
