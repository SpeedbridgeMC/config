package io.github.speedbridgemc.config.processor.api.property;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import io.github.speedbridgemc.config.processor.impl.property.ConfigPropertyImpl;

public final class ConfigPropertyBuilder {
    private final Lazy<ConfigType> type;
    private final String name;
    private final boolean isAccessors, isReadOnly;
    private final String fieldName, getterName, setterName;
    private final ImmutableSet.Builder<ConfigPropertyFlag> flagsBuilder;
    private final ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> extensionsBuilder;

    public static ConfigPropertyBuilder field(Lazy<ConfigType> type, String name,
                                                       String fieldName, boolean isFinal) {
        return new ConfigPropertyBuilder(type, name, false, isFinal, fieldName, "", "");
    }

    public static ConfigPropertyBuilder field(Lazy<ConfigType> type, String name,
                                                       String fieldName) {
        return field(type, name, fieldName, false);
    }

    public static ConfigPropertyBuilder getter(Lazy<ConfigType> type, String name,
                                                       String getterName) {
        return new ConfigPropertyBuilder(type, name, true, true, "", getterName, "");
    }

    public static ConfigPropertyBuilder accessors(Lazy<ConfigType> type, String name,
                                                       String getterName, String setterName) {
        return new ConfigPropertyBuilder(type, name, true, false, "", getterName, setterName);
    }

    private ConfigPropertyBuilder(Lazy<ConfigType> type, String name,
                          boolean isAccessors, boolean isReadOnly,
                          String fieldName, String getterName, String setterName) {
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

    public ConfigPropertyBuilder flag(ConfigPropertyFlag flag) {
        flagsBuilder.add(flag);
        return this;
    }

    public <E extends ConfigPropertyExtension> ConfigPropertyBuilder extension(Class<E> type, E extension) {
        extensionsBuilder.put(type, extension);
        return this;
    }

    public ConfigProperty build() {
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
