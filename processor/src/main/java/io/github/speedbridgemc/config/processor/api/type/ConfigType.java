package io.github.speedbridgemc.config.processor.api.type;

import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;

/**
 * Represents a value type in a configuration file.
 */
public interface ConfigType {
    static @NotNull ConfigStructBuilder structBuilder(@NotNull TypeMirror forMirror) {
        return new ConfigStructBuilder(forMirror);
    }

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

    // only for ENUM types
    /**
     * Gets the constants this enum type contains.<p>
     * If this type is not an enum, returns an empty list.
     * @return enum constant names
     */
    @NotNull List<? extends String> enumConstants();

    // only for ARRAY types
    /**
     * Gets the component type of this array type.
     * @return component type, or {@link Optional#empty()} if not an array
     */
    @NotNull Optional<ConfigType> componentType();

    // only for MAP types
    /**
     * Gets the key type of this map type.
     * @return key type, or {@link Optional#empty()} if not a map
     */
    @NotNull Optional<ConfigType> keyType();
    /**
     * Gets the value type of this map type.
     * @return value type, or {@link Optional#empty()} if not a map
     */
    @NotNull Optional<ConfigType> valueType();

    // only for STRUCT types
    /**
     * Gets the instantiation strategy of this struct type.
     * @return instantiation strategy, or {@link Optional#empty()} if not a struct
     */
    @NotNull Optional<StructInstantiationStrategy> instantiationStrategy();
    /**
     * Gets the nested properties of this struct type.<p>
     * If this type is not a struct, returns an empty list.
     * @return type properties
     */
    @NotNull List<? extends ConfigProperty> properties();
    /**
     * Gets the nested property with the specified name in this struct type, if it exists.
     * @param name property name
     * @return property, or {@literal Optional.empty()} if this struct doesn't contain a property with that name
     */
    default @NotNull Optional<ConfigProperty> property(@NotNull String name) {
        for (ConfigProperty prop : properties()) {
            if (name.equals(prop.name()))
                return Optional.of(prop);
        }
        return Optional.empty();
    }
}
