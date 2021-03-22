package io.github.speedbridgemc.config.serialize;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets a field's serialized name.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface SerializedName {
    /**
     * The field's serialized name.
     * @return field name
     */
    @NotNull String value();
}
