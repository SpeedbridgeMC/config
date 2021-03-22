package io.github.speedbridgemc.config.serialize;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies <em>aliases</em> for a field.<p>
 * If the field's name isn't found, the serializer will instead try to load a value from one of these aliases.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface SerializedAliases {
    /**
     * The field's aliases.
     * @return field aliases
     */
    @NotNull String @NotNull [] value();
}
