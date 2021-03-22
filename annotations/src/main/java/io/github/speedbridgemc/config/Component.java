package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link Config} handler implementation component.
 */
@Target({})
@Retention(RetentionPolicy.SOURCE)
public @interface Component {
    /**
     * The ID of the component to add.
     * @return component ID
     */
    @NotNull String value();

    /**
     * The parameters to pass to the component.<br>
     * Each element should define one parameter and its value.
     * @return component parameters
     */
    @NotNull String @NotNull [] params() default { };
}
