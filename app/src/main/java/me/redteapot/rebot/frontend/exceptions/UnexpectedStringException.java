package me.redteapot.rebot.frontend.exceptions;

import me.redteapot.rebot.Markdown;
import me.redteapot.rebot.Strings;
import me.redteapot.rebot.frontend.MessageReader;

import static me.redteapot.rebot.Strings.format;

/**
 * A {@link MessageReader} exception thrown if the reader stumbles upon
 * an unexpected string.
 */
public class UnexpectedStringException extends ReaderException {
    protected final String expected;
    protected final int length;

    public UnexpectedStringException(String source, int position, String expected) {
        super(source, position);
        this.expected = expected;
        this.length = expected.length();
    }

    public UnexpectedStringException(String source, int start, int end) {
        super(source, start);
        this.expected = null;
        this.length = end - start;
    }

    @Override
    public Markdown describe() {
        int expectedEnd = position + length;
        String realString = Strings.softSubstring(source, position, expectedEnd);

        Markdown message = new Markdown();
        String error;
        if (expected == null) {
            error = format("Unexpected string: `{}`.", realString);
        } else {
            error = format("Unexpected string: `{}`, `{}` expected.", realString, expected);
        }
        message.code(Strings.comment(source, error, position, expectedEnd, 10));

        return message;
    }
}
