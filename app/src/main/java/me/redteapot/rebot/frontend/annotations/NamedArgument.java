package me.redteapot.rebot.frontend.annotations;

import me.redteapot.rebot.frontend.ArgumentParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An argument that is specified using a key-value pair, separated via
 * an equals sign, e.g. {@code argument=123}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NamedArgument {
    /**
     * The name of this argument. Must be an ASCII identifier, i.e. consist
     * only of English letters, digits and underscores.
     */
    String name();

    /**
     * The parser class used for this argument value.
     */
    Class<? extends ArgumentParser<?>> type();

    /**
     * Specifies if this argument is optional and can be omitted.
     */
    boolean optional() default false;
}
