package me.redteapot.rebot;

/**
 * A base class for all exceptions that might occur during
 * command execution.
 */
public abstract class CommandException extends Exception {
    /**
     * Returns a formatted Markdown description of the exception.
     */
    public abstract Markdown describe();
}
