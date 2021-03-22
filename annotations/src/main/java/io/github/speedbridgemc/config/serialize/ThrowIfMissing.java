package io.github.speedbridgemc.config.serialize;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that, should a field be missing, an exception should be thrown by the serializer.<p>
 * This will cause the entire load operation to fail, falling back to loading default values.<p>
 * If a type is marked with this method, all of its fields will "inherit" the annotation and its values.
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.SOURCE)
public @interface ThrowIfMissing {
    /**
     * The message to be used for the exception. This is suffixed onto the field name.
     * @return exception message
     */
    @NotNull String @NotNull [] message() default { };
}
