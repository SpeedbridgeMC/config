package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Marks a class as a configuration POJO. The annotation processor will generate a handler implementation for it.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Config {
    /**
     * The name of the configuration.<br>
     * Components may use this string in any way they see fit.
     * @return config name
     */
    @NotNull String name();

    /**
     * The name of the handler interface that the processor should generate an implementation for.
     * @return handler interface class name
     */
    @NotNull @JavaClass String handlerInterface();

    /**
     * The {@link Component}s that should be added to the handler implementation.
     * @return components
     */
    @NotNull Component @NotNull [] components();

    /**
     * The name (and package) of the handler implementation.<p>
     * By default, this is the {@linkplain #handlerInterface() handler interface's name} suffixed with "Impl".
     * @return handler implementation class name
     */
    @NotNull String @NotNull [] handlerName() default { };

    /**
     * The name of the non-null annotation to use in the handler implementation.
     * @return non-null annotation class name
     */
    @NotNull @JavaClass String @NotNull [] nonNullAnnotation() default { };

    /**
     * The name of the nullable annotation to use in the handler implementation.
     * @return nullable annotation class name
     */
    @NotNull @JavaClass String @NotNull [] nullableAnnotation() default { };
}
