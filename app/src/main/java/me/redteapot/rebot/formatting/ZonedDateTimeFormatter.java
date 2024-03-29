package me.redteapot.rebot.formatting;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class ZonedDateTimeFormatter implements TypeFormatter<ZonedDateTime> {
    private final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendLiteral('/')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('/')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral(' ')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .appendLiteral(' ')
        .appendZoneId()
        .toFormatter();

    @Override
    public String format(ZonedDateTime object) {
        return formatter.format(object);
    }
}
