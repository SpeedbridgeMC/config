package io.github.speedbridgemc.config.processor.api.property;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import io.github.speedbridgemc.config.processor.impl.property.ConfigPropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ConfigProperty {
    static @NotNull ConfigProperty field(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                         @NotNull String fieldName, boolean isFinal, boolean isOptional) {
        return new ConfigPropertyImpl.Field(type, name, ImmutableClassToInstanceMap.of(), !isFinal, isOptional, fieldName);
    }

    static @NotNull ConfigProperty field(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                         @NotNull String fieldName, boolean isOptional) {
        return field(type, name, fieldName, false, isOptional);
    }

    static @NotNull ConfigProperty accessors(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                             @NotNull String getterName, @NotNull String setterName, boolean isOptional) {
        return new ConfigPropertyImpl.Accessors(type, name, ImmutableClassToInstanceMap.of(), isOptional, getterName, setterName);
    }

    static @NotNull ConfigProperty getter(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                          @NotNull String getterName, boolean isOptional) {
        return new ConfigPropertyImpl.Accessors(type, name, ImmutableClassToInstanceMap.of(), isOptional, getterName);
    }

    static @NotNull ConfigPropertyBuilder fieldBuilder(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                                       @NotNull String fieldName, boolean isFinal) {
        return new ConfigPropertyBuilder(type, name, false, !isFinal, fieldName, "", "");
    }

    static @NotNull ConfigPropertyBuilder fieldBuilder(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                                       @NotNull String fieldName) {
        return fieldBuilder(type, name, fieldName, false);
    }

    static @NotNull ConfigPropertyBuilder accessorsBuilder(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                                           @NotNull String getterName, @NotNull String setterName) {
        return new ConfigPropertyBuilder(type, name, true, false, "", getterName, setterName);
    }

    static @NotNull ConfigPropertyBuilder getterBuilder(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                                        @NotNull String getterName) {
        return new ConfigPropertyBuilder(type, name, true, true, "", getterName, "");
    }

    @NotNull String name();
    @NotNull ConfigType type();
    boolean isOptional();
    @NotNull <E extends ConfigPropertyExtension> Optional<E> extension(@NotNull Class<E> type);

    @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String destination);
    /**
     * Checks if this property can be set. If not, it's likely initialized using the struct's
     * {@linkplain io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy instantiation strategy}.
     * @return {@literal true} if property can be set, {@literal false} otherwise
     */
    boolean canSet();
    @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String source);
}
