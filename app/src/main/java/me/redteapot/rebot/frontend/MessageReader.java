package me.redteapot.rebot.frontend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.redteapot.rebot.Markdown;
import me.redteapot.rebot.Strings;

import java.util.function.Predicate;

import static me.redteapot.rebot.Checks.require;
import static me.redteapot.rebot.Checks.unreachable;
import static me.redteapot.rebot.Markdown.md;
import static me.redteapot.rebot.Strings.format;

/**
 * A reader class for Discord messages.
 */
@SuppressWarnings("unused")
public class MessageReader {
    @Getter
    private final String message;
    @Getter
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
     * @throws UnexpectedEndOfMessageException If the reader can't read the char.
     */
    public char peek() throws UnexpectedEndOfMessageException {
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
            unreachable("MessageReader.skip(predicate) got a ReaderException: {}", e);
        }
    }

    /**
     * Reads one char and advances the current position by 1.
     *
     * @return The char at current position.
     * @throws UnexpectedEndOfMessageException If the reader can't read the char.
     */
    public char read() throws UnexpectedEndOfMessageException {
        assertCanRead();
        return message.charAt(position++);
    }

    /**
     * Reads {@code count} chars and advances the current position accordingly.
     *
     * @param count The number of chars to read.
     * @return The read string.
     * @throws UnexpectedEndOfMessageException If the reader can't read the string.
     */
    public String read(int count) throws UnexpectedEndOfMessageException {
        assertCanRead(count - 1);
        final int start = position;
        position += count;
        return message.substring(start, position);
    }

    /**
     * Reads chars while {@code skipPredicate} is true and the reader can read.
     * The returned string might be empty.
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
            return unreachable("MessageReader.read(predicate) got a ReaderException: {}", e);
        }
    }

    /**
     * Reads chars while {@code skipPredicate} is true and the reader can read.
     * Throws a {@link ReaderException} if no characters matching the
     * predicate can be read.
     *
     * @param readPredicate The predicate that should return true if the char
     *                      should be read.
     * @param comment       The comment to show if unexpected character occurs.
     * @return The read string.
     */
    public String readNonEmpty(Predicate<Character> readPredicate, String comment) throws ReaderException {
        String result = read(readPredicate);

        if (result.isEmpty()) {
            if (canRead()) {
                throw new ReaderUnexpectedCharException(getMessage(), getPosition(), comment);
            } else {
                throw new UnexpectedEndOfMessageException(getMessage(), getPosition());
            }
        }

        return result;
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
            throw new ReaderUnexpectedCharException(message, position, c);
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
            throw new ReaderUnexpectedStringException(message, position, s);
        }
    }

    /**
     * Checks that the next character matches {@code predicate}
     * and advances the reader position if it does.
     *
     * @param predicate The predicate to check the expected char.
     * @throws ReaderException If can't read or the char doesn't match {@code predicate}.
     */
    public void expect(Predicate<Character> predicate, String comment) throws ReaderException {
        assertCanRead();
        if (!predicate.test(peek())) {
            throw new ReaderUnexpectedCharException(message, position, comment);
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
        int pos = getPosition();
        try {
            expect(s);
            return true;
        } catch (ReaderException e) {
            rewind(pos);
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
            expect(predicate, "");
            return true;
        } catch (ReaderException e) {
            return false;
        }
    }

    /**
     * Changes current position. The new position
     * must be less than or equal to the current position.
     *
     * @param position The new position.
     */
    public void rewind(int position) {
        require(position <= this.position, "New position must be less than or equal to the current one");
        this.position = position;
    }

    /**
     * Asserts that the reader can read the char at given {@code offset}.
     *
     * @throws UnexpectedEndOfMessageException If the reader can't read the char.
     */
    private void assertCanRead(int offset) throws UnexpectedEndOfMessageException {
        if (!canRead(offset)) {
            throw new UnexpectedEndOfMessageException(message, position + offset);
        }
    }

    /**
     * Asserts that the reader can read one char.
     *
     * @throws UnexpectedEndOfMessageException If the reader can't read the char.
     */
    private void assertCanRead() throws UnexpectedEndOfMessageException {
        if (!canRead()) {
            throw new UnexpectedEndOfMessageException(message, position);
        }
    }

    /**
     * A general {@link MessageReader} exception.
     */
    @EqualsAndHashCode(callSuper = false)
    @Data
    @AllArgsConstructor
    public static class ReaderException extends Exception {
        protected final String source;
        protected final int position;

        public Markdown markdown() {
            return md("Syntax error.");
        }
    }

    /**
     * A {@link MessageReader} exception thrown if the reader stumbles upon
     * an unexpected char.
     */
    public static class ReaderUnexpectedCharException extends ReaderException {
        protected final Character expected;
        protected final String comment;

        public ReaderUnexpectedCharException(String source, int position, Character expected) {
            super(source, position);
            this.expected = expected;
            this.comment = null;
        }

        public ReaderUnexpectedCharException(String source, int position, String comment) {
            super(source, position);
            this.expected = null;
            this.comment = comment;
        }

        @Override
        public Markdown markdown() {
            int realPos = position + 1;
            char realChar = source.charAt(position);

            String error;
            if (expected == null) {
                error = format("Unexpected char: `{}`, {} expected.", realChar, comment);
            } else {
                error = format("Unexpected char: `{}`, `{}` expected.", realChar, expected);
            }

            Markdown markdown = new Markdown();
            markdown.code(Strings.comment(source, error, position, 10));
            return markdown;
        }
    }

    /**
     * A {@link MessageReader} exception thrown if the reader stumbles upon
     * an unexpected string.
     */
    public static class ReaderUnexpectedStringException extends ReaderException {
        protected final String expected;

        public ReaderUnexpectedStringException(String source, int position, String expected) {
            super(source, position);
            this.expected = expected;
        }

        @Override
        public Markdown markdown() {
            int expectedEnd = position + expected.length();
            String realString = Strings.softSubstring(source, position, expectedEnd);

            Markdown message = new Markdown();
            String error = format("Unexpected string: `{}`, `{}` expected.", realString, expected);
            message.code(Strings.comment(source, error, position, expectedEnd, 10));

            return message;
        }
    }

    /**
     * A {@link MessageReader} exception thrown if the reader reaches
     * the end of the message unexpectedly.
     */
    public static class UnexpectedEndOfMessageException extends ReaderException {
        public UnexpectedEndOfMessageException(String source, int position) {
            super(source, position);
        }

        @Override
        public Markdown markdown() {
            return md("Unexpected end of message.");
        }
    }
}
