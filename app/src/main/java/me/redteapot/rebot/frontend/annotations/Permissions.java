package me.redteapot.rebot.frontend.annotations;

/**
 * Command permissions enum.
 */
public enum Permissions {
    /**
     * General - all users are allowed to run this command.
     */
    GENERAL,
    /**
     * Server admin - only server admins are allowed to run this command.
     */
    SERVER_ADMIN,
    /**
     * Bot owner - only bot owners (specified in the config file) are allowed
     * to run this command. This permission is independent from {@code SERVER_ADMIN},
     * e.g. bot owners are not allowed to run server admin commands.
     */
    BOT_OWNER
}
