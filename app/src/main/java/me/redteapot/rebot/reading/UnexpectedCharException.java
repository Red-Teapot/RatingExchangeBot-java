package me.redteapot.rebot.reading;

import me.redteapot.rebot.Markdown;
import me.redteapot.rebot.Strings;

import static me.redteapot.rebot.formatting.Formatter.format;

/**
 * A {@link MessageReader} exception thrown if the reader stumbles upon
 * an unexpected char.
 */
public class UnexpectedCharException extends ReaderException {
    protected final Character expected;
    protected final String comment;

    public UnexpectedCharException(String source, int position, Character expected) {
        super(source, position);
        this.expected = expected;
        this.comment = null;
    }

    public UnexpectedCharException(String source, int position, String comment) {
        super(source, position);
        this.expected = null;
        this.comment = comment;
    }

    @Override
    public Markdown describe() {
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
