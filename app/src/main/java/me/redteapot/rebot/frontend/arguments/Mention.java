package me.redteapot.rebot.frontend.arguments;

import discord4j.common.util.Snowflake;
import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.frontend.MessageReader;
import me.redteapot.rebot.frontend.MessageReader.ReaderException;

public abstract class Mention implements ArgumentParser<Snowflake> {
    private final PrefixCheck prefixCheck;

    protected Mention(PrefixCheck prefixCheck) {
        this.prefixCheck = prefixCheck;
    }

    @Override
    public Snowflake parse(MessageReader reader) throws ReaderException {
        reader.expect('<');

        prefixCheck.check(reader);

        final long id = Long.parseLong(reader.read(Chars::isAsciiDigit));

        reader.expect('>');

        return Snowflake.of(id);
    }

    @FunctionalInterface
    protected interface PrefixCheck {
        void check(MessageReader reader) throws ReaderException;
    }
}
