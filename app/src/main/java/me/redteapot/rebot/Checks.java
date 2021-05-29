package me.redteapot.rebot;

import static me.redteapot.rebot.formatting.Formatter.format;

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

    /**
     * Marks a line that should be never executed. Throws an
     * {@link IllegalStateException} when ran.
     *
     * @param message The message to show.
     * @param args    Formatting arguments.
     */
    public static <T> T unreachable(String message, Object... args) {
        throw new IllegalStateException(format(message, args));
    }
}
