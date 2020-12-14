package me.redteapot.rebot.frontend;

import discord4j.common.util.Snowflake;
import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.MessageReader.ReaderException;

@SuppressWarnings("unused")
public class Parsers {
    public static void skipWhitespace(MessageReader reader) {
        reader.skip(Character::isWhitespace);
    }

    public static String readUnquotedString(MessageReader reader) {
        return reader.read(c -> !Character.isWhitespace(c));
    }

    public static String readQuotedString(MessageReader reader) throws ReaderException {
        char quote = reader.peek();
        reader.expect(Chars::isQuote);

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

        throw new UnexpectedEndOfMessageException();
    }

    public static String readString(MessageReader reader) throws ReaderException {
        if (Chars.isQuote(reader.peek())) {
            return readQuotedString(reader);
        } else {
            return readUnquotedString(reader);
        }
    }

    private static Snowflake readSnowflake(MessageReader reader, PrefixCheck prefixCheck) throws ReaderException {
        reader.expect('<');

        prefixCheck.check();

        final long id = Long.parseLong(reader.read(Chars::isAsciiDigit));

        reader.expect('>');

        return Snowflake.of(id);
    }

    public static Snowflake readUserMention(MessageReader reader) throws ReaderException {
        return readSnowflake(reader, () -> {
            reader.expect('@'); // For users
            reader.optional('!'); // For users with nicknames (optional)
        });
    }

    public static Snowflake readChannelMention(MessageReader reader) throws ReaderException {
        return readSnowflake(reader, () -> reader.expect('#'));
    }

    public static Snowflake readRoleMention(MessageReader reader) throws ReaderException {
        return readSnowflake(reader, () -> reader.expect("@&"));
    }

    public static class UnexpectedEndOfMessageException extends ReaderException {
    }

    @FunctionalInterface
    private interface PrefixCheck {
        void check() throws ReaderException;
    }
}
