package me.redteapot.rebot;

import org.slf4j.helpers.MessageFormatter;

/**
 * Custom assertion methods.
 * Because standard {@code assert}s are weird sometimes.
 */
public class Assertion {
    public static void isTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    public static void isTrue(boolean condition, String error, Object... args) {
        if (!condition) {
            String errorFormatted = MessageFormatter.arrayFormat(error, args).getMessage();
            throw new AssertionError(errorFormatted);
        }
    }
}
