package me.redteapot.rebot.frontend;

import java.util.function.Predicate;

/**
 * A reader class for Discord messages.
 */
@SuppressWarnings("unused")
public class MessageReader {
    private final String message;
    private int position = 0;

    /**
     * Constructs a new reader for given message.
     *
     * @param message The message to read.
     */
    public MessageReader(String message) {
        this.message = message;
    }

    /**
     * Checks whether the reader can read {@code offset} chars forwards
     * from the current position.
     *
     * @param offset The offset.
     * @return True if it can, false otherwise.
     */
    public boolean canRead(int offset) {
        return position + offset < message.length();
    }

    /**
     * Checks whether the reader can read one char.
     *
     * @return True if it can, false otherwise.
     */
    public boolean canRead() {
        return canRead(0);
    }

    /**
     * Peeks one char without advancing the current reader position.
     *
     * @return The char at current position.
     * @throws ReaderOutOfBoundsException If the reader can't read the char.
     */
    public char peek() throws ReaderOutOfBoundsException {
        assertCanRead();
        return message.charAt(position);
    }

    /**
     * Skips {@code count} chars without any checks.
     *
     * @param count The number of chars to skip.
     */
    public void skip(int count) {
        position += count;
    }

    /**
     * Skips one char without any checks.
     */
    public void skip() {
        skip(1);
    }

    /**
     * Skips chars while {@code skipPredicate} is true and the reader can read.
     *
     * @param skipPredicate The predicate that should return true if the char
     *                      should be skipped.
     */
    public void skip(Predicate<Character> skipPredicate) {
        try {
            while (canRead() && skipPredicate.test(peek())) {
                skip();
            }
        } catch (ReaderException e) {
            // This should never happen
            throw new IllegalStateException();
        }
    }

    /**
     * Reads one char and advances the current position by 1.
     *
     * @return The char at current position.
     * @throws ReaderOutOfBoundsException If the reader can't read the char.
     */
    public char read() throws ReaderOutOfBoundsException {
        assertCanRead();
        return message.charAt(position++);
    }

    /**
     * Reads {@code count} chars and advances the current position accordingly.
     *
     * @param count The number of chars to read.
     * @return The read string.
     * @throws ReaderOutOfBoundsException If the reader can't read the string.
     */
    public String read(int count) throws ReaderOutOfBoundsException {
        assertCanRead(count - 1);
        final int start = position;
        position += count;
        return message.substring(start, position);
    }

    /**
     * Reads chars while {@code skipPredicate} is true and the reader can read.
     *
     * @param readPredicate The predicate that should return true if the char
     *                      should be read.
     * @return The read string.
     */
    public String read(Predicate<Character> readPredicate) {
        try {
            final int start = position;
            while (canRead() && readPredicate.test(peek())) {
                skip();
            }
            return message.substring(start, position);
        } catch (ReaderException e) {
            // This should never happen
            throw new IllegalStateException();
        }
    }

    /**
     * Checks that the next char is {@code c}
     * and advances the reader position if it is.
     *
     * @param c The expected char.
     * @throws ReaderException If can't read or the next char is not {@code c}.
     */
    public void expect(char c) throws ReaderException {
        assertCanRead();
        if (peek() != c) {
            throw new ReaderUnexpectedCharException();
        }
        skip();
    }

    /**
     * Checks that the string from the current position matches {@code s}
     * and advances the reader position if it does.
     *
     * @param s The expected string.
     * @throws ReaderException If can't read or the next string is not {@code s}.
     */
    public void expect(String s) throws ReaderException {
        assertCanRead(s.length());
        if (!read(s.length()).equals(s)) {
            throw new ReaderUnexpectedStringException();
        }
        skip(s.length());
    }

    /**
     * Checks that the next character matches {@code predicate}
     * and advances the reader position if it does.
     *
     * @param predicate The predicate to check the expected char.
     * @throws ReaderException If can't read or the char doesn't match {@code predicate}.
     */
    public void expect(Predicate<Character> predicate) throws ReaderException {
        assertCanRead();
        if (!predicate.test(peek())) {
            throw new ReaderUnexpectedCharException();
        }
        skip();
    }

    /**
     * Checks that the next char is {@code c} and advances the reader
     * position if it is.
     *
     * @param c The expected char.
     * @return True if matches, false otherwise.
     */
    public boolean optional(char c) {
        try {
            expect(c);
            return true;
        } catch (ReaderException e) {
            return false;
        }
    }

    /**
     * Checks that the string from the current position matches {@code s}
     * and advances the reader position if it does.
     *
     * @param s The expected string.
     * @return True if matches, false otherwise.
     */
    public boolean optional(String s) {
        try {
            expect(s);
            return true;
        } catch (ReaderException e) {
            return false;
        }
    }

    /**
     * Checks that the next character matches {@code predicate}
     * and advances the reader position if it does.
     *
     * @param predicate The predicate to check the expected char.
     * @return True if matches, false otherwise.
     */
    public boolean optional(Predicate<Character> predicate) {
        try {
            expect(predicate);
            return true;
        } catch (ReaderException e) {
            return false;
        }
    }

    /**
     * Asserts that the reader can read the char at given {@code offset}.
     *
     * @throws ReaderOutOfBoundsException If the reader can't read the char.
     */
    private void assertCanRead(int offset) throws ReaderOutOfBoundsException {
        if (!canRead(offset)) {
            throw new ReaderOutOfBoundsException();
        }
    }

    /**
     * Asserts that the reader can read one char.
     *
     * @throws ReaderOutOfBoundsException If the reader can't read the char.
     */
    private void assertCanRead() throws ReaderOutOfBoundsException {
        if (!canRead()) {
            throw new ReaderOutOfBoundsException();
        }
    }

    /**
     * A general {@link MessageReader} exception.
     */
    public static class ReaderException extends Exception {
    }

    /**
     * A {@link MessageReader} exception thrown when trying to read chars
     * out of the message bounds.
     */
    public static class ReaderOutOfBoundsException extends ReaderException {
    }

    /**
     * A {@link MessageReader} exception thrown if the reader stumbles upon
     * an unexpected char.
     */
    public static class ReaderUnexpectedCharException extends ReaderException {
    }

    /**
     * A {@link MessageReader} exception thrown if the reader stumbles upon
     * an unexpected string.
     */
    public static class ReaderUnexpectedStringException extends ReaderException {
    }
}
