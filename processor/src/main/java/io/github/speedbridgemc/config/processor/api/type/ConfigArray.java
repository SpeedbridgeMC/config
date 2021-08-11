package io.github.speedbridgemc.config.processor.api.type;

public interface ConfigArray extends ConfigType {
    /**
     * Gets the component type of this array type.
     * @return component type
     */
    ConfigType componentType();
}
