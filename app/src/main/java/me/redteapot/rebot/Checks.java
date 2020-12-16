package me.redteapot.rebot;

import static me.redteapot.rebot.Strings.format;

@SuppressWarnings("unused")
public class Checks {
    /**
     * Checks given condition and throws {@link IllegalArgumentException}
     * if it is false.
     *
     * @param condition The condition to check.
     * @param message   The message to show if the check fails.
     * @param args      Message formatting arguments.
     */
    public static void require(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(format(message, args));
        }
    }

    /**
     * Asserts that given condition is true.
     *
     * @param condition The condition to check.
     * @param message   The message to show if the check fails.
     * @param args      Message formatting arguments.
     */
    public static void assertion(boolean condition, String message, Object... args) {
        assert condition : format(message, args);
    }

    /**
     * Checks given condition and throws {@link IllegalStateException}
     * if it is false.
     *
     * @param condition The condition to check.
     * @param message   The message to show if the check fails.
     * @param args      Message formatting arguments.
     */
    public static void ensure(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalStateException(format(message, args));
        }
    }

    public static <T> T unreachable(String message, Object... args) {
        throw new IllegalStateException(format(message, args));
    }
}
