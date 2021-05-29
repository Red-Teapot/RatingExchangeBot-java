package me.redteapot.rebot.reading;

import me.redteapot.rebot.Markdown;

import static me.redteapot.rebot.Markdown.md;

/**
 * A {@link MessageReader} exception thrown if the reader reaches
 * the end of the message unexpectedly.
 */
public class UnexpectedEndOfMessageException extends ReaderException {
    public UnexpectedEndOfMessageException(String source, int position) {
        super(source, position);
    }

    @Override
    public Markdown describe() {
        return md("Unexpected end of message.");
    }
}
