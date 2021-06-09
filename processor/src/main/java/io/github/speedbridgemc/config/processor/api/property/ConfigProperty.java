package io.github.speedbridgemc.config.processor.api.property;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.impl.property.ConfigPropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public interface ConfigProperty {
    static @NotNull ConfigProperty field(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                                         @NotNull String fieldName, boolean isFinal, boolean isOptional) {
        return new ConfigPropertyImpl.Field(typeSupplier, name, ImmutableClassToInstanceMap.of(), !isFinal, isOptional, fieldName);
    }

    static @NotNull ConfigProperty field(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                                         @NotNull String fieldName, boolean isOptional) {
        return field(typeSupplier, name, fieldName, false, isOptional);
    }

    static @NotNull ConfigProperty accessors(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                                             @NotNull String getterName, @NotNull String setterName, boolean isOptional) {
        return new ConfigPropertyImpl.Accessors(typeSupplier, name, ImmutableClassToInstanceMap.of(), isOptional, getterName, setterName);
    }

    static @NotNull ConfigProperty getter(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                                          @NotNull String getterName, boolean isOptional) {
        return new ConfigPropertyImpl.Accessors(typeSupplier, name, ImmutableClassToInstanceMap.of(), isOptional, getterName);
    }

    static @NotNull ConfigPropertyBuilder fieldBuilder(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                                                       @NotNull String fieldName, boolean isFinal) {
        return new ConfigPropertyBuilder(typeSupplier, name, false, !isFinal, fieldName, "", "");
    }

    static @NotNull ConfigPropertyBuilder fieldBuilder(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                                                       @NotNull String fieldName) {
        return fieldBuilder(typeSupplier, name, fieldName, false);
    }

    static @NotNull ConfigPropertyBuilder accessorsBuilder(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                                                           @NotNull String getterName, @NotNull String setterName) {
        return new ConfigPropertyBuilder(typeSupplier, name, true, false, "", getterName, setterName);
    }

    static @NotNull ConfigPropertyBuilder getterBuilder(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                                                        @NotNull String getterName) {
        return new ConfigPropertyBuilder(typeSupplier, name, true, true, "", getterName, "");
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
