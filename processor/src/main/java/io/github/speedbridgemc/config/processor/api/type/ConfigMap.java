package io.github.speedbridgemc.config.processor.api.type;

import org.jetbrains.annotations.NotNull;

public interface ConfigMap extends ConfigType {
    /**
     * Gets the key type of this map type.
     * @return key type
     */
    @NotNull ConfigType keyType();
    /**
     * Gets the value type of this map type.
     * @return value type
     */
    @NotNull ConfigType valueType();
}
