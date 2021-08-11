package io.github.speedbridgemc.config.processor.api.type.provider;

import com.google.common.collect.ImmutableList;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.type.ConfigStruct;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import io.github.speedbridgemc.config.processor.impl.type.ConfigTypeImpl;

import javax.lang.model.type.DeclaredType;

public final class ConfigStructBuilder {
    private final DeclaredType mirror;
    private final ImmutableList.Builder<ConfigProperty> propertiesBuilder;
    private StructInstantiationStrategy instantiationStrategy;

    public ConfigStructBuilder(DeclaredType mirror) {
        this.mirror = mirror;
        propertiesBuilder = ImmutableList.builder();
        instantiationStrategy = StructInstantiationStrategy.NONE;
    }

    public ConfigStructBuilder property(ConfigProperty property) {
        propertiesBuilder.add(property);
        return this;
    }

    public ConfigStructBuilder properties(ConfigProperty ... properties) {
        propertiesBuilder.add(properties);
        return this;
    }

    public ConfigStructBuilder properties(Iterable<ConfigProperty> properties) {
        propertiesBuilder.addAll(properties);
        return this;
    }

    public ConfigStructBuilder instantiationStrategy(StructInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
        return this;
    }

    public ConfigStruct build() {
        return new ConfigTypeImpl.Struct(mirror, instantiationStrategy, propertiesBuilder.build());
    }
}
