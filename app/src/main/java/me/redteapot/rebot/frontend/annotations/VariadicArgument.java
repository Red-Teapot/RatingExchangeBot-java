package me.redteapot.rebot.frontend.annotations;

import me.redteapot.rebot.frontend.ArgumentParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VariadicArgument {

    Class<? extends ArgumentParser<?>> type();

}
