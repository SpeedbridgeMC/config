package io.github.speedbridgemc.config.processor.api.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ConfigEnum extends ConfigType {
    /**
     * Gets the constants this enum type contains.
     * @return enum constant names
     */
    @NotNull List<? extends String> enumConstants();
}
