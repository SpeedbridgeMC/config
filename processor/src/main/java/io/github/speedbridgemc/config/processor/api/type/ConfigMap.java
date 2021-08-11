package io.github.speedbridgemc.config.processor.api.type;

public interface ConfigMap extends ConfigType {
    /**
     * Gets the key type of this map type.
     * @return key type
     */
    ConfigType keyType();
    /**
     * Gets the value type of this map type.
     * @return value type
     */
    ConfigType valueType();
}
