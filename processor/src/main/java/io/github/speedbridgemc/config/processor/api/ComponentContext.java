package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public final class ComponentContext {
    public final @NotNull String handlerName;
    public final @NotNull TypeName handlerInterfaceTypeName;
    public final @NotNull TypeElement handlerInterfaceTypeElement;
    public final @NotNull ImmutableList<ExecutableElement> handlerInterfaceMethods;
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;
    public final @NotNull TypeName configType;
    public final @NotNull Multimap<String, String> params;
    public final @NotNull MethodSpec.Builder getMethodBuilder, loadMethodBuilder, saveMethodBuilder;

    public ComponentContext(@NotNull String handlerName,
                            @NotNull TypeName handlerInterfaceTypeName,
                            @NotNull TypeElement handlerInterfaceTypeElement,
                            @NotNull ImmutableList<ExecutableElement> handlerInterfaceMethods,
                            @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation,
                            @NotNull TypeName configType,
                            @NotNull Multimap<String, String> params,
                            MethodSpec.@NotNull Builder getMethodBuilder,
                            MethodSpec.@NotNull Builder loadMethodBuilder,
                            MethodSpec.@NotNull Builder saveMethodBuilder) {
        this.handlerName = handlerName;
        this.handlerInterfaceTypeName = handlerInterfaceTypeName;
        this.handlerInterfaceTypeElement = handlerInterfaceTypeElement;
        this.handlerInterfaceMethods = handlerInterfaceMethods;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
        this.configType = configType;
        this.params = params;
        this.getMethodBuilder = getMethodBuilder;
        this.loadMethodBuilder = loadMethodBuilder;
        this.saveMethodBuilder = saveMethodBuilder;
    }
}
