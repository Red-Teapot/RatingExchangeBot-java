package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.frontend.MessageReader;
import me.redteapot.rebot.frontend.exceptions.ReaderException;
import me.redteapot.rebot.frontend.exceptions.UnexpectedCharException;

import java.time.Duration;

public class DurationArg implements ArgumentParser<Duration> {
    @Override
    public Duration parse(MessageReader reader) throws ReaderException {
        Duration result = Duration.ZERO;

        while (true) {
            if (!reader.canRead() || Character.isWhitespace(reader.peek())) {
                break;
            }

            final int number;
            try {
                number = Integer.parseInt(reader.read(Chars::isAsciiDigit));
            } catch (NumberFormatException e) {
                break;
            }

            char specifier = reader.read();

            switch (specifier) {
                case 'd':
                    result = result.plusDays(number);
                    break;
                case 'h':
                    result = result.plusHours(number);
                    break;
                case 'm':
                    result = result.plusMinutes(number);
                    break;
                case 's':
                    result = result.plusSeconds(number);
                    break;
                default:
                    throw new UnexpectedCharException(reader.getMessage(), reader.getPosition() - 1, "one of: `d`, `h`, `m`, `s`");
            }
        }

        return result;
    }
}
