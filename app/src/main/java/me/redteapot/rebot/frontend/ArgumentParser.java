package me.redteapot.rebot.frontend;

import me.redteapot.rebot.frontend.exceptions.ReaderException;

public interface ArgumentParser<T> {
    T parse(MessageReader reader) throws ReaderException;
}
