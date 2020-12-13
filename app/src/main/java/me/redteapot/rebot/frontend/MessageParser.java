package me.redteapot.rebot.frontend;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import lombok.Getter;
import me.redteapot.rebot.frontend.MessageReader.ReaderException;

@SuppressWarnings("unused")
public class MessageParser {
    @Getter
    private final MessageReader reader;

    public MessageParser(Message message) {
        reader = new MessageReader(message);
    }

    public void skipWhitespace() {
        reader.skip(Character::isWhitespace);
    }

    public String readUnquotedString() {
        return reader.read(c -> !Character.isWhitespace(c));
    }

    public String readQuotedString() throws ReaderException {
        char quote = reader.peek();
        reader.expect(MessageParser::isQuoteChar);

        final StringBuilder result = new StringBuilder();
        boolean escape = false;
        while (reader.canRead()) {
            final char c = reader.read();

            if (c == '\\') {
                if (escape) {
                    result.append('\\');
                } else {
                    escape = true;
                }
                continue;
            }

            if (c == quote) {
                if (escape) {
                    result.append(quote);
                } else {
                    return result.toString();
                }
                continue;
            }

            result.append(c);
        }

        // FIXME
        throw new IllegalStateException();
    }

    public String readString() throws ReaderException {
        if (isQuoteChar(reader.peek())) {
            return readQuotedString();
        } else {
            return readUnquotedString();
        }
    }

    private Snowflake readSnowflake(PrefixCheck prefixCheck) throws ReaderException {
        reader.expect('<');

        prefixCheck.check();

        final long id = Long.parseLong(reader.read(Character::isDigit));

        reader.expect('>');

        return Snowflake.of(id);
    }

    public Snowflake readUserMention() throws ReaderException {
        return readSnowflake(() -> {
            reader.expect('@'); // For users
            reader.optional('!'); // For users with nicknames (optional)
        });
    }

    public Snowflake readChannelMention() throws ReaderException {
        return readSnowflake(() -> reader.expect('#'));
    }

    public Snowflake readRoleMention() throws ReaderException {
        return readSnowflake(() -> reader.expect("@&"));
    }

    @FunctionalInterface
    private interface PrefixCheck {
        void check() throws ReaderException;
    }

    private static boolean isQuoteChar(char c) {
        return c == '"' || c == '\'';
    }
}
