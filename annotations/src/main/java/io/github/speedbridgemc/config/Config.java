package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Config {
    @NotNull String name();
    @NotNull @JavaClass String handlerInterface();
    @NotNull Component @NotNull [] components();
    @NotNull String @NotNull [] handlerName() default { };
    @NotNull @JavaClass String @NotNull [] nonNullAnnotation() default { };
    @NotNull @JavaClass String @NotNull [] nullableAnnotation() default { };
}
