package io.github.speedbridgemc.config.processor.api.type.provider;

import com.google.common.collect.ImmutableList;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.type.ConfigStruct;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import io.github.speedbridgemc.config.processor.impl.type.ConfigTypeImpl;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.DeclaredType;

public final class ConfigStructBuilder {
    private final @NotNull DeclaredType mirror;
    private final @NotNull ImmutableList.Builder<ConfigProperty> propertiesBuilder;
    private @NotNull StructInstantiationStrategy instantiationStrategy;

    public ConfigStructBuilder(@NotNull DeclaredType mirror) {
        this.mirror = mirror;
        propertiesBuilder = ImmutableList.builder();
        instantiationStrategy = StructInstantiationStrategy.NONE;
    }

    public @NotNull ConfigStructBuilder property(@NotNull ConfigProperty property) {
        propertiesBuilder.add(property);
        return this;
    }

    public @NotNull ConfigStructBuilder properties(@NotNull ConfigProperty @NotNull ... properties) {
        propertiesBuilder.add(properties);
        return this;
    }

    public @NotNull ConfigStructBuilder properties(@NotNull Iterable<ConfigProperty> properties) {
        propertiesBuilder.addAll(properties);
        return this;
    }

    public @NotNull ConfigStructBuilder instantiationStrategy(@NotNull StructInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
        return this;
    }

    public @NotNull ConfigStruct build() {
        return new ConfigTypeImpl.Struct(mirror, instantiationStrategy, propertiesBuilder.build());
    }
}
