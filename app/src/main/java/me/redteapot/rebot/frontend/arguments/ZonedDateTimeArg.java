package me.redteapot.rebot.frontend.arguments;

import me.redteapot.rebot.Chars;
import me.redteapot.rebot.frontend.ArgumentParser;
import me.redteapot.rebot.reading.MessageReader;
import me.redteapot.rebot.reading.ReaderException;
import me.redteapot.rebot.reading.UnexpectedStringException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static me.redteapot.rebot.Checks.ensure;

public class ZonedDateTimeArg implements ArgumentParser<ZonedDateTime> {
    @Override
    public ZonedDateTime parse(MessageReader reader) throws ReaderException {
        int year;
        int month;
        int day;

        int start = reader.getPosition();
        try {
            String yearStr = reader.readNonEmpty(Chars::isAsciiDigit, "year");
            reader.expect('/');
            String monthStr = reader.readNonEmpty(Chars::isAsciiDigit, "month");
            reader.expect('/');
            String dayStr = reader.readNonEmpty(Chars::isAsciiDigit, "day");

            year = Integer.parseInt(yearStr);
            month = Integer.parseInt(monthStr);
            day = Integer.parseInt(dayStr);

            reader.skip(Character::isWhitespace);
        } catch (ReaderException | NumberFormatException e) {
            reader.rewind(start);
            year = -1;
            month = -1;
            day = -1;
        }

        int hour = 0;
        int minute = 0;
        int second = 0;
        try {
            start = reader.getPosition();
            reader.skip(Character::isWhitespace);

            hour = Integer.parseInt(reader.readNonEmpty(Chars::isAsciiDigit, "hour"));
            reader.expect(':');
            start = reader.getPosition();
            minute = Integer.parseInt(reader.readNonEmpty(Chars::isAsciiDigit, "minute"));

            if (reader.optional(':')) {
                start = reader.getPosition();
                second = Integer.parseInt(reader.readNonEmpty(Chars::isAsciiDigit, "second"));
            }
        } catch (NumberFormatException e) {
            throw new UnexpectedStringException(reader.getMessage(), start, reader.getPosition());
        }

        start = reader.getPosition();
        ZoneId timezone = ZoneId.of("UTC");
        try {
            reader.skip(Character::isWhitespace);
            String timezoneStr = reader.read(c -> Chars.isAsciiAlphabetic(c) || Chars.isAsciiDigit(c) || c == '+' || c == '-' || c == '/');
            timezone = ZoneId.of(timezoneStr);
        } catch (DateTimeException e) {
            reader.rewind(start);
        }

        ZonedDateTime result = ZonedDateTime.now(timezone).withNano(0);
        if (year >= 0) {
            ensure(month >= 0, "year is >= 0 but month is < 0");
            ensure(day >= 0, "year is >= 0 but day is < 0");

            result = result.withYear(year).withMonth(month).withDayOfMonth(day);
        }
        result = result.withHour(hour).withMinute(minute).withSecond(second);

        return result;
    }
}
