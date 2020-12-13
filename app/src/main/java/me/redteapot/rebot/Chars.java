package me.redteapot.rebot;

/**
 * Miscellaneous char util class.
 */
public class Chars {
    public static boolean isAsciiDigit(char c) {
        return "0123456789".indexOf(c) != -1;
    }

    public static boolean isAsciiAlphabetic(char c) {
        return "abcdefghijklmnopqrstuvwxyz".indexOf(Character.toLowerCase(c)) != -1;
    }

    public static boolean isQuote(char c) {
        return c == '\'' || c == '"';
    }

    public static boolean isAsciiIdentifier(char c) {
        return isAsciiDigit(c) || isAsciiAlphabetic(c) || c == '_';
    }
}
