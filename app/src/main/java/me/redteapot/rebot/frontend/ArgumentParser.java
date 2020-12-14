package me.redteapot.rebot.frontend;

import me.redteapot.rebot.frontend.MessageReader.ReaderException;

public interface ArgumentParser<T> {
    T parse(MessageReader reader) throws ReaderException;
}
