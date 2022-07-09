package me.redteapot.rebot.frontend.annotations;

import me.redteapot.rebot.frontend.ArgumentParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An argument that is specified via its order, separated from previous
 * arguments with spaces.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrderedArgument {
    /**
     * The order of this argument, starting from zero.
     */
    int order();

    /**
     * The parser class used for this argument value.
     */
    Class<? extends ArgumentParser<?>> type();

    /**
     * Specifies if this argument is optional and can be omitted.
     */
    boolean optional() default false;
}
