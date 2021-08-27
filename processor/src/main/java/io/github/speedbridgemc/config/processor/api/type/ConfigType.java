package io.github.speedbridgemc.config.processor.api.type;

import io.github.speedbridgemc.config.processor.api.util.Lazy;

import javax.lang.model.type.TypeMirror;

/**
 * Represents a value type in a configuration file.
 */
public interface ConfigType extends Lazy<ConfigType> {
    /**
     * Gets the kind of this type.
     * @return type kind
     */
    ConfigTypeKind kind();
    /**
     * Gets the name of this type.
     * @return name
     */
    String name();
    /**
     * Gets this type as a {@link TypeMirror}.
     * @return type as mirror
     */
    TypeMirror asMirror();

    /**
     * Returns {@code this}.
     *
     * @return this type instance
     */
    @Override
    default ConfigType get() {
        return this;
    }
}
