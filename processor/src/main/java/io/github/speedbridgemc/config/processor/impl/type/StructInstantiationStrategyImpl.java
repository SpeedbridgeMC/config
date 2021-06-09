package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableList;

public abstract class StructInstantiationStrategyImpl implements StructInstantiationStrategy {
    public static final class ParameterImpl implements Parameter {
        private final @NotNull Lazy<ConfigType> typeLazy;
        public final @NotNull String name;
        public final @NotNull String boundProperty;
        private String toStringCache;

        public ParameterImpl(@NotNull Lazy<ConfigType> typeLazy, @NotNull String name, @NotNull String boundProperty) {
            this.typeLazy = typeLazy;
            this.name = name;
            this.boundProperty = boundProperty;
        }

        @Override
        public @NotNull ConfigType type() {
            return typeLazy.get();
        }

        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull String boundProperty() {
            return boundProperty;
        }

        @Override
        public String toString() {
            if (toStringCache == null) {
                if (name.equals(boundProperty))
                    toStringCache = type() + " " + name;
                else
                    toStringCache = type() + " " + name + "@" + boundProperty;
            }
            return toStringCache;
        }
    }

    protected final @NotNull ImmutableList<Parameter> params;
    protected final @NotNull String paramTemplate;
    protected String toStringCache;

    protected StructInstantiationStrategyImpl(@NotNull List<Parameter> params) {
        this.params = toImmutableList(params);
        final int paramCount = params.size();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramCount; i++) {
            sb.append("$").append(params.get(i).name());
            if (i < paramCount - 1)
                sb.append(", ");
        }
        paramTemplate = sb.toString();
    }

    @Override
    public boolean canInstantiate() {
        return true;
    }

    @Override
    public @NotNull List<? extends Parameter> params() {
        return params;
    }

    @Override
    public String toString() {
        if (toStringCache == null)
            toStringCache = toString0();
        return toStringCache;
    }

    protected @NotNull String toString0() {
        return "";
    }

    public static final class None extends StructInstantiationStrategyImpl {
        public static final None INSTANCE = new None();

        private None() {
            super(ImmutableList.of());
        }

        @Override
        public @NotNull CodeBlock generateNew(@NotNull String destination, @NotNull Map<String, String> paramSources) {
            throw new IllegalStateException("Can't instantiate an instance of this struct!");
        }

        @Override
        public @NotNull String toString() {
            return "none";
        }
    }

    public static final class Constructor extends StructInstantiationStrategyImpl {
        private final @NotNull TypeName typeName;

        public Constructor(@NotNull List<Parameter> params, @NotNull TypeName typeName) {
            super(params);
            this.typeName = typeName;
        }

        @Override
        public @NotNull CodeBlock generateNew(@NotNull String destination, @NotNull Map<String, String> paramSources) {
            return CodeBlock.builder()
                    .add("$L = new $T(", destination, typeName)
                    .addNamed(paramTemplate, paramSources)
                    .add(")")
                    .build();
        }

        @Override
        protected @NotNull String toString0() {
            StringBuilder sb = new StringBuilder("constructor ").append(typeName).append('(');
            if (!params.isEmpty()) {
                for (Parameter param : params)
                    sb.append(param).append(", ");
                sb.setLength(sb.length() - 2);
            }
            return sb.append(')').toString();
        }
    }

    public static final class Factory extends StructInstantiationStrategyImpl {
        private final @NotNull TypeName ownerTypeName;
        private final @NotNull String methodName;

        public Factory(@NotNull List<Parameter> params, @NotNull TypeName ownerTypeName, @NotNull String methodName) {
            super(params);
            this.ownerTypeName = ownerTypeName;
            this.methodName = methodName;
        }

        @Override
        public @NotNull CodeBlock generateNew(@NotNull String destination, @NotNull Map<String, String> paramSources) {
            return CodeBlock.builder()
                    .add("$L = $T.$N(", destination, ownerTypeName, methodName)
                    .addNamed(paramTemplate, paramSources)
                    .add(")")
                    .build();
        }

        @Override
        protected @NotNull String toString0() {
            StringBuilder sb = new StringBuilder("factory ").append(ownerTypeName).append('.').append(methodName).append('(');
            if (!params.isEmpty()) {
                for (Parameter param : params)
                    sb.append(param).append(", ");
                sb.setLength(sb.length() - 2);
            }
            return sb.append(')').toString();
        }
    }
}
