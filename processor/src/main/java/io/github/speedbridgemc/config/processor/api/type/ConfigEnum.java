package io.github.speedbridgemc.config.processor.api.type;

import java.util.List;

public interface ConfigEnum extends ConfigType {
    /**
     * Gets the constants this enum type contains.
     * @return enum constant names
     */
    List<? extends String> enumConstants();
}
