package io.github.speedbridgemc.config.processor.api.property;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import io.github.speedbridgemc.config.processor.impl.property.ConfigPropertyImpl;
import org.jetbrains.annotations.NotNull;

public final class ConfigPropertyBuilder {
    private final @NotNull Lazy<ConfigType> type;
    private final @NotNull String name;
    private final boolean isAccessors, isReadOnly;
    private final @NotNull String fieldName, getterName, setterName;
    private final @NotNull ImmutableSet.Builder<ConfigPropertyFlag> flagsBuilder;
    private final @NotNull ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> extensionsBuilder;

    public static @NotNull ConfigPropertyBuilder field(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                                       @NotNull String fieldName, boolean isFinal) {
        return new ConfigPropertyBuilder(type, name, false, isFinal, fieldName, "", "");
    }

    public static @NotNull ConfigPropertyBuilder field(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                                       @NotNull String fieldName) {
        return field(type, name, fieldName, false);
    }

    public static @NotNull ConfigPropertyBuilder getter(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                                       @NotNull String getterName) {
        return new ConfigPropertyBuilder(type, name, true, true, "", getterName, "");
    }

    public static @NotNull ConfigPropertyBuilder accessors(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                                       @NotNull String getterName, @NotNull String setterName) {
        return new ConfigPropertyBuilder(type, name, true, false, "", getterName, setterName);
    }

    private ConfigPropertyBuilder(@NotNull Lazy<ConfigType> type, @NotNull String name,
                          boolean isAccessors, boolean isReadOnly,
                          @NotNull String fieldName, @NotNull String getterName, @NotNull String setterName) {
        this.type = type;
        this.name = name;
        this.isAccessors = isAccessors;
        this.isReadOnly = isReadOnly;
        this.fieldName = fieldName;
        this.getterName = getterName;
        this.setterName = setterName;
        flagsBuilder = ImmutableSet.builder();
        extensionsBuilder = ImmutableClassToInstanceMap.builder();
    }

    public @NotNull ConfigPropertyBuilder flag(@NotNull ConfigPropertyFlag flag) {
        flagsBuilder.add(flag);
        return this;
    }

    public <E extends ConfigPropertyExtension> @NotNull ConfigPropertyBuilder extension(@NotNull Class<E> type, @NotNull E extension) {
        extensionsBuilder.put(type, extension);
        return this;
    }

    public @NotNull ConfigProperty build() {
        final ImmutableSet<ConfigPropertyFlag> flags = flagsBuilder.build();
        final ImmutableClassToInstanceMap<ConfigPropertyExtension> extensions = extensionsBuilder.build();
        if (isAccessors) {
            if (isReadOnly)
                return new ConfigPropertyImpl.Accessors(type, name, flags, extensions, getterName);
            else
                return new ConfigPropertyImpl.Accessors(type, name, flags, extensions, getterName, setterName);
        } else
            return new ConfigPropertyImpl.Field(type, name, flags, extensions, !isReadOnly, fieldName);
    }
}
