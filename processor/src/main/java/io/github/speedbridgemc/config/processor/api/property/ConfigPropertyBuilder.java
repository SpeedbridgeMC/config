package io.github.speedbridgemc.config.processor.api.property;

import com.google.common.collect.ImmutableClassToInstanceMap;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.impl.property.ConfigPropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class ConfigPropertyBuilder {
    private final @NotNull Supplier<ConfigType> typeSupplier;
    private final @NotNull String name;
    private final boolean isAccessors, isReadOnly;
    private final @NotNull String fieldName, getterName, setterName;
    private final @NotNull ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> extensionsBuilder;

    ConfigPropertyBuilder(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name,
                          boolean isAccessors, boolean isReadOnly,
                          @NotNull String fieldName, @NotNull String getterName, @NotNull String setterName) {
        this.typeSupplier = typeSupplier;
        this.name = name;
        this.isAccessors = isAccessors;
        this.isReadOnly = isReadOnly;
        this.fieldName = fieldName;
        this.getterName = getterName;
        this.setterName = setterName;
        extensionsBuilder = ImmutableClassToInstanceMap.builder();
    }

    public <E extends ConfigPropertyExtension> @NotNull ConfigPropertyBuilder extension(@NotNull Class<E> type, @NotNull E extension) {
        extensionsBuilder.put(type, extension);
        return this;
    }

    public @NotNull ConfigProperty build() {
        final ImmutableClassToInstanceMap<ConfigPropertyExtension> extensions = extensionsBuilder.build();
        if (isAccessors) {
            if (isReadOnly)
                return new ConfigPropertyImpl.Accessors(typeSupplier, name, extensions, getterName);
            else
                return new ConfigPropertyImpl.Accessors(typeSupplier, name, extensions, getterName, setterName);
        } else
            return new ConfigPropertyImpl.Field(typeSupplier, name, extensions, !isReadOnly, fieldName);
    }
}
