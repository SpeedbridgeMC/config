package io.github.speedbridgemc.config.processor.api.type;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.impl.type.StructInstantiationStrategyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface StructInstantiationStrategy {
    @NotNull StructInstantiationStrategy NONE = StructInstantiationStrategyImpl.None.INSTANCE;

    interface Parameter {
        @NotNull ConfigType type();
        @NotNull String name();
        @NotNull String boundProperty();
    }

    boolean canInstantiate();
    @NotNull List<? extends Parameter> params();
    @NotNull CodeBlock generateNew(@NotNull String destination, @NotNull Map<String, String> paramSources);

    default @NotNull CodeBlock generateNew(@NotNull String destination, @NotNull String @NotNull ... paramSources) {
        if (paramSources.length == 0)
            return generateNew(destination, Collections.emptyMap());
        if ((paramSources.length & 1) == 1)
            throw new IllegalArgumentException("Varargs count should be even!");
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < paramSources.length; i += 2)
            map.put(paramSources[i], paramSources[i + 1]);
        return generateNew(destination, map);
    }
}
