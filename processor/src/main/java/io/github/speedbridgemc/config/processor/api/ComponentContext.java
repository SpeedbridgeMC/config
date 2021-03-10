package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.Multimap;
import com.squareup.javapoet.MethodSpec;
import org.jetbrains.annotations.NotNull;

public final class ComponentContext {
    public final @NotNull Multimap<String, String> params;
    public final @NotNull MethodSpec.Builder loadMethodBuilder, saveMethodBuilder;

    public ComponentContext(@NotNull Multimap<String, String> params,
                            MethodSpec.@NotNull Builder loadMethodBuilder, MethodSpec.@NotNull Builder saveMethodBuilder) {
        this.params = params;
        this.loadMethodBuilder = loadMethodBuilder;
        this.saveMethodBuilder = saveMethodBuilder;
    }
}
