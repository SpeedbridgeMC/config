package io.github.speedbridgemc.config.processor.api.type.provider;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import io.github.speedbridgemc.config.processor.impl.type.StructInstantiationStrategyImpl;

public final class StructInstantiationStrategyBuilder {
    public static StructInstantiationStrategyBuilder constructor(TypeName ownerName) {
        return new StructInstantiationStrategyBuilder(false, ownerName, "");
    }

    public static StructInstantiationStrategyBuilder factory(TypeName ownerName, String name) {
        return new StructInstantiationStrategyBuilder(true, ownerName, name);
    }

    private final boolean isFactory;
    private final TypeName ownerName;
    private final String factoryName;
    private final ImmutableList.Builder<StructInstantiationStrategy.Parameter> paramsBuilder;

    private StructInstantiationStrategyBuilder(boolean isFactory, TypeName ownerName, String factoryName) {
        this.isFactory = isFactory;
        this.ownerName = ownerName.withoutAnnotations();
        this.factoryName = factoryName;
        paramsBuilder = ImmutableList.builder();
    }

    public StructInstantiationStrategyBuilder param(Lazy<ConfigType> type, String name, String boundProperty) {
        paramsBuilder.add(new StructInstantiationStrategyImpl.ParameterImpl(type, name, boundProperty));
        return this;
    }

    public StructInstantiationStrategyBuilder param(Lazy<ConfigType> type, String name) {
        return param(type, name, name);
    }

    public StructInstantiationStrategy build() {
        if (isFactory)
            return new StructInstantiationStrategyImpl.Factory(paramsBuilder.build(), ownerName, factoryName);
        else
            return new StructInstantiationStrategyImpl.Constructor(paramsBuilder.build(), ownerName);
    }
}
