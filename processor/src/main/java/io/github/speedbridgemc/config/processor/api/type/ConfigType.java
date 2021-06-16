package io.github.speedbridgemc.config.processor.api.type;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;

/**
 * Represents a value type in a configuration file.
 */
public interface ConfigType {
    /**
     * Gets the kind of this type.
     * @return type kind
     */
    @NotNull ConfigTypeKind kind();
    /**
     * Gets the name of this type.
     * @return name
     */
    @NotNull String name();
    /**
     * Gets this type as a {@link TypeMirror}.
     * @return type as mirror
     */
    @NotNull TypeMirror asMirror();
    /**
     * Checks if this type can hold {@literal null}.
     * @return {@literal true} if nullable, {@literal false} otherwise
     */
    boolean isNullable();
}
