package me.redteapot.rebot.frontend.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.redteapot.rebot.CommandException;
import me.redteapot.rebot.Markdown;
import me.redteapot.rebot.Strings;
import me.redteapot.rebot.frontend.MessageReader;

/**
 * A general {@link MessageReader} exception.
 */
@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
public class ReaderException extends CommandException {
    protected final String source;
    protected final int position;

    public Markdown describe() {
        Markdown description = new Markdown();
        description.code(Strings.comment(source, "Syntax error", position, 10));
        return description;
    }
}
