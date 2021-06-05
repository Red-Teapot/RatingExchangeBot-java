package me.redteapot.rebot.frontend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotCommand {
    /**
     * A name used to call this command in Discord.
     * Must be an ASCII identifier, i.e. consist only of English letters,
     * digits and underscores.
     */
    String name();

    /**
     * Permission level required to run this command.
     *
     * @see Permissions
     */
    Permissions permissions();

    /**
     * Specifies whether or not the bot accepts this command in DM.
     */
    boolean allowedInDM() default false;
}
