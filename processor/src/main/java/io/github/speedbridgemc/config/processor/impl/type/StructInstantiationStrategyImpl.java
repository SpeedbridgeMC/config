package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import io.github.speedbridgemc.config.processor.api.util.Lazy;

import java.util.List;
import java.util.Map;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableList;

public abstract class StructInstantiationStrategyImpl implements StructInstantiationStrategy {
    public static final class ParameterImpl implements Parameter {
        private final Lazy<ConfigType> type;
        public final String name;
        public final String boundProperty;
        private String toStringCache;

        public ParameterImpl(Lazy<ConfigType> type, String name, String boundProperty) {
            this.type = type;
            this.name = name;
            this.boundProperty = boundProperty;
        }

        @Override
        public ConfigType type() {
            return type.get();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String boundProperty() {
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

    protected final ImmutableList<Parameter> params;
    protected final String paramTemplate;
    protected String toStringCache;

    protected StructInstantiationStrategyImpl(List<Parameter> params) {
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
    public List<? extends Parameter> params() {
        return params;
    }

    @Override
    public String toString() {
        if (toStringCache == null)
            toStringCache = toString0();
        return toStringCache;
    }

    protected String toString0() {
        return "";
    }

    public static final class None extends StructInstantiationStrategyImpl {
        public static final None INSTANCE = new None();

        private None() {
            super(ImmutableList.of());
        }

        @Override
        public boolean canInstantiate() {
            return false;
        }

        @Override
        public CodeBlock generateNew(String destination, Map<String, String> paramSources) {
            throw new UnsupportedOperationException("Can't instantiate an instance of this struct!");
        }

        @Override
        public String toString() {
            return "none";
        }
    }

    public static final class Constructor extends StructInstantiationStrategyImpl {
        private final TypeName typeName;

        public Constructor(List<Parameter> params, TypeName typeName) {
            super(params);
            this.typeName = typeName;
        }

        @Override
        public CodeBlock generateNew(String destination, Map<String, String> paramSources) {
            return CodeBlock.builder()
                    .add("$L = new $T(", destination, typeName)
                    .addNamed(paramTemplate, paramSources)
                    .add(")")
                    .build();
        }

        @Override
        protected String toString0() {
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
        private final TypeName ownerTypeName;
        private final String methodName;

        public Factory(List<Parameter> params, TypeName ownerTypeName, String methodName) {
            super(params);
            this.ownerTypeName = ownerTypeName;
            this.methodName = methodName;
        }

        @Override
        public CodeBlock generateNew(String destination, Map<String, String> paramSources) {
            return CodeBlock.builder()
                    .add("$L = $T.$N(", destination, ownerTypeName, methodName)
                    .addNamed(paramTemplate, paramSources)
                    .add(")")
                    .build();
        }

        @Override
        protected String toString0() {
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
