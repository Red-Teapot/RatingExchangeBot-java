package me.redteapot.rebot;

import org.slf4j.helpers.MessageFormatter;

public class Strings {
    public static String format(String fmt, Object... args) {
        return MessageFormatter.format(fmt, args).getMessage();
    }
}
