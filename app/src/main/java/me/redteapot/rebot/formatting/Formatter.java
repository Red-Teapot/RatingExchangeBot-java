package me.redteapot.rebot.formatting;

import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.Chars;
import me.redteapot.rebot.reading.MessageReader;
import me.redteapot.rebot.reading.ReaderException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class Formatter {
    private static final Map<Class, TypeFormatter> formatters = new HashMap<>();

    static {
        formatters.put(Duration.class, new DurationFormatter());
        formatters.put(ZonedDateTime.class, new ZonedDateTimeFormatter());
    }

    private Formatter() {
    }

    public static String format(String message, Object... args) {
        MessageReader reader = new MessageReader(message);
        StringBuilder result = new StringBuilder();

        int currentArgIndex = 0;
        try {
            while (reader.canRead()) {
                switch (reader.peek()) {
                    case '{':
                        reader.skip();
                        String argIndexStr = reader.read(Chars::isAsciiDigit);
                        if (reader.optional('}')) {
                            if (argIndexStr.length() > 0) {
                                int argIndex = Integer.parseInt(argIndexStr);
                                result.append(formatArgument(args[argIndex]));
                            } else {
                                result.append(formatArgument(args[currentArgIndex++]));
                            }
                        } else {
                            result.append('{');
                            result.append(argIndexStr);
                        }
                        break;
                    case '\\':
                        result.append(readEscape(reader));
                        break;
                    default:
                        result.append(reader.read());
                        break;
                }
            }
        } catch (ReaderException e) {
            log.error("Exception while formatting a message", e);
        }

        return result.toString();
    }

    private static String readEscape(MessageReader reader) throws ReaderException {
        reader.expect('\\');
        if (!reader.canRead()) {
            return "\\";
        } else {
            return Character.toString(reader.read());
        }
    }

    private static String formatArgument(Object arg) {
        Class<?> clazz = arg.getClass();
        if (formatters.containsKey(clazz)) {
            return formatters.get(clazz).format(arg);
        } else {
            return arg.toString();
        }
    }
}
