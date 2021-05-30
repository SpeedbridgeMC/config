package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableList;

public abstract class StructInstantiationStrategyImpl implements StructInstantiationStrategy {
    protected final @NotNull ImmutableList<Parameter> params;
    protected final @NotNull String paramTemplate;

    protected StructInstantiationStrategyImpl(@NotNull List<Parameter> params) {
        this.params = toImmutableList(params);
        final int paramCount = params.size();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramCount; i++) {
            sb.append("$").append(params.get(i).name);
            if (i < paramCount - 1)
                sb.append(", ");
        }
        paramTemplate = sb.toString();
    }

    @Override
    public @NotNull List<? extends Parameter> params() {
        return params;
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
    }
}
