package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.Multimap;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;

public final class ComponentContext {
    public final @NotNull String handlerName;
    public final @NotNull TypeName configType;
    public final @NotNull Multimap<String, String> params;
    public final @NotNull MethodSpec.Builder getMethodBuilder, loadMethodBuilder, saveMethodBuilder;

    public ComponentContext(@NotNull String handlerName,
                            @NotNull TypeName configType,
                            @NotNull Multimap<String, String> params,
                            MethodSpec.@NotNull Builder getMethodBuilder,
                            MethodSpec.@NotNull Builder loadMethodBuilder,
                            MethodSpec.@NotNull Builder saveMethodBuilder) {
        this.handlerName = handlerName;
        this.configType = configType;
        this.params = params;
        this.getMethodBuilder = getMethodBuilder;
        this.loadMethodBuilder = loadMethodBuilder;
        this.saveMethodBuilder = saveMethodBuilder;
    }
}
