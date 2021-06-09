package io.github.speedbridgemc.config.processor.api.type;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import io.github.speedbridgemc.config.processor.impl.type.StructInstantiationStrategyImpl;
import org.jetbrains.annotations.NotNull;

public final class StructInstantiationStrategyBuilder {
    public static @NotNull StructInstantiationStrategyBuilder constructor(@NotNull TypeName ownerName) {
        return new StructInstantiationStrategyBuilder(false, ownerName, "");
    }

    public static @NotNull StructInstantiationStrategyBuilder factory(@NotNull TypeName ownerName, @NotNull String name) {
        return new StructInstantiationStrategyBuilder(true, ownerName, name);
    }

    private final boolean isFactory;
    private final @NotNull TypeName ownerName;
    private final @NotNull String factoryName;
    private final @NotNull ImmutableList.Builder<StructInstantiationStrategy.Parameter> paramsBuilder;

    private StructInstantiationStrategyBuilder(boolean isFactory, @NotNull TypeName ownerName, @NotNull String factoryName) {
        this.isFactory = isFactory;
        this.ownerName = ownerName.withoutAnnotations();
        this.factoryName = factoryName;
        paramsBuilder = ImmutableList.builder();
    }

    public @NotNull StructInstantiationStrategyBuilder param(@NotNull Lazy<ConfigType> type, @NotNull String name, @NotNull String boundProperty) {
        paramsBuilder.add(new StructInstantiationStrategyImpl.ParameterImpl(type, name, boundProperty));
        return this;
    }

    public @NotNull StructInstantiationStrategyBuilder param(@NotNull Lazy<ConfigType> type, @NotNull String name) {
        return param(type, name, name);
    }

    public @NotNull StructInstantiationStrategy build() {
        if (isFactory)
            return new StructInstantiationStrategyImpl.Factory(paramsBuilder.build(), ownerName, factoryName);
        else
            return new StructInstantiationStrategyImpl.Constructor(paramsBuilder.build(), ownerName);
    }
}
