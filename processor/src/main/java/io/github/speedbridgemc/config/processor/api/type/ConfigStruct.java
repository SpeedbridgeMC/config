package io.github.speedbridgemc.config.processor.api.type;

import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;

import java.util.List;
import java.util.Optional;

public interface ConfigStruct extends ConfigType {
    /**
     * Gets the instantiation strategy of this struct type.
     * @return instantiation strategy
     */
    StructInstantiationStrategy instantiationStrategy();
    /**
     * Gets the nested properties of this struct type.
     * @return type properties
     */
    List<? extends ConfigProperty> properties();
    /**
     * Gets the nested property with the specified name in this struct type, if it exists.
     * @param name property name
     * @return property, or {@code Optional.empty()} if this struct doesn't contain a property with that name
     */
    default Optional<ConfigProperty> property(String name) {
        for (ConfigProperty prop : properties()) {
            if (name.equals(prop.name()))
                return Optional.of(prop);
        }
        return Optional.empty();
    }
}
