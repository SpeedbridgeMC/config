package io.github.speedbridgemc.config.processor.api.type;

import org.jetbrains.annotations.NotNull;

public interface ConfigArray extends ConfigType {
    /**
     * Gets the component type of this array type.
     * @return component type
     */
    @NotNull ConfigType componentType();
}
