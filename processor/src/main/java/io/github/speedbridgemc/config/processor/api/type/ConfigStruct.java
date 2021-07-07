package io.github.speedbridgemc.config.processor.api.type;

import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface ConfigStruct extends ConfigType {
    /**
     * Gets the instantiation strategy of this struct type.
     * @return instantiation strategy
     */
    @NotNull StructInstantiationStrategy instantiationStrategy();
    /**
     * Gets the nested properties of this struct type.
     * @return type properties
     */
    @NotNull List<? extends ConfigProperty> properties();
    /**
     * Gets the nested property with the specified name in this struct type, if it exists.
     * @param name property name
     * @return property, or {@code Optional.empty()} if this struct doesn't contain a property with that name
     */
    default @NotNull Optional<ConfigProperty> property(@NotNull String name) {
        for (ConfigProperty prop : properties()) {
            if (name.equals(prop.name()))
                return Optional.of(prop);
        }
        return Optional.empty();
    }
}
