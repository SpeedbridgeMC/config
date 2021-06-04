package io.github.speedbridgemc.config.processor.api.type;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.impl.type.StructInstantiationStrategyImpl;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import java.util.function.Supplier;

public final class StructInstantiationStrategyBuilder {
    public static @NotNull StructInstantiationStrategyBuilder constructor(@NotNull TypeMirror owner) {
        return new StructInstantiationStrategyBuilder(false, owner, "");
    }

    public static @NotNull StructInstantiationStrategyBuilder factory(@NotNull TypeMirror owner, @NotNull String name) {
        return new StructInstantiationStrategyBuilder(true, owner, name);
    }

    private final boolean isFactory;
    private final @NotNull TypeName owner;
    private final @NotNull String factoryName;
    private final @NotNull ImmutableList.Builder<StructInstantiationStrategy.Parameter> paramsBuilder;

    private StructInstantiationStrategyBuilder(boolean isFactory, @NotNull TypeMirror owner, @NotNull String factoryName) {
        this.isFactory = isFactory;
        this.owner = TypeName.get(owner).withoutAnnotations();
        this.factoryName = factoryName;
        paramsBuilder = ImmutableList.builder();
    }

    public @NotNull StructInstantiationStrategyBuilder param(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name, @NotNull String boundProperty) {
        paramsBuilder.add(new StructInstantiationStrategyImpl.ParameterImpl(typeSupplier, name, boundProperty));
        return this;
    }

    public @NotNull StructInstantiationStrategyBuilder param(@NotNull Supplier<ConfigType> typeSupplier, @NotNull String name) {
        return param(typeSupplier, name, name);
    }

    public @NotNull StructInstantiationStrategy build() {
        if (isFactory)
            return new StructInstantiationStrategyImpl.Factory(paramsBuilder.build(), owner, factoryName);
        else
            return new StructInstantiationStrategyImpl.Constructor(paramsBuilder.build(), owner);
    }
}
