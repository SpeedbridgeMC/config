package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.TypeElement;

/**
 * The component processing context.
 */
public final class ComponentContext {
    /**
     * The name of the configuration POJO class.
     */
    public final @NotNull TypeName configName;
    /**
     * The name of the handler implementation class.
     */
    public final @NotNull ClassName handlerName;
    /**
     * The name of the handler interface.
     */
    public final @NotNull TypeName handlerInterfaceName;
    /**
     * The {@link TypeElement} of the handler interface.
     */
    public final @NotNull TypeElement handlerInterfaceTypeElement;
    private final @NotNull ImmutableList<MethodSignature> handlerInterfaceMethods;
    /**
     * The name of the non-null annotation, or {@code null} if no non-null annotation is specified.
     */
    public final @Nullable ClassName nonNullAnnotation;
    /**
     * The name of the nullable annotation, or {@code null} if no nullable annotation is specified.
     */
    public final @Nullable ClassName nullableAnnotation;
    /**
     * The parameters specified for this component.
     */
    public final @NotNull Multimap<String, String> params;
    /**
     * The {@code get} method's builder.
     */
    public final @NotNull MethodSpec.Builder getMethodBuilder;
    /**
     * The {@code reset} method's builder.
     */
    public final @NotNull MethodSpec.Builder resetMethodBuilder;
    /**
     * The {@code load} method's builder.
     */
    public final @NotNull MethodSpec.Builder loadMethodBuilder;
    /**
     * The {@code save} method's builder.
     */
    public final @NotNull MethodSpec.Builder saveMethodBuilder;
    /**
     * This builder's contents will be appended to the end of the {@code load} method.
     */
    public final @NotNull CodeBlock.Builder postLoadBuilder;
    /**
     * This builder's contents will be appended to the end of the {@code save} method.
     */
    public final @NotNull CodeBlock.Builder postSaveBuilder;
    /**
     * The {@code set} method's builder, or {@code null} if the handler interface doesn't define a {@code set} method.
     */
    public final @Nullable MethodSpec.Builder setMethodBuilder;

    @ApiStatus.Internal
    public ComponentContext(@NotNull TypeName configName, @NotNull ClassName handlerName,
                            @NotNull TypeName handlerInterfaceName,
                            @NotNull TypeElement handlerInterfaceTypeElement,
                            @NotNull ImmutableList<MethodSignature> handlerInterfaceMethods,
                            @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation,
                            @NotNull Multimap<String, String> params,
                            MethodSpec.@NotNull Builder getMethodBuilder, MethodSpec.@NotNull Builder resetMethodBuilder,
                            MethodSpec.@NotNull Builder loadMethodBuilder, MethodSpec.@NotNull Builder saveMethodBuilder,
                            CodeBlock.@NotNull Builder postLoadBuilder, CodeBlock.@NotNull Builder postSaveBuilder,
                            MethodSpec.@Nullable Builder setMethodBuilder) {
        this.handlerName = handlerName;
        this.handlerInterfaceName = handlerInterfaceName;
        this.handlerInterfaceTypeElement = handlerInterfaceTypeElement;
        this.handlerInterfaceMethods = handlerInterfaceMethods;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
        this.configName = configName;
        this.params = params;
        this.getMethodBuilder = getMethodBuilder;
        this.resetMethodBuilder = resetMethodBuilder;
        this.loadMethodBuilder = loadMethodBuilder;
        this.saveMethodBuilder = saveMethodBuilder;
        this.postLoadBuilder = postLoadBuilder;
        this.postSaveBuilder = postSaveBuilder;
        this.setMethodBuilder = setMethodBuilder;
    }

    /**
     * Checks if the handler interface defines/implements a method.
     * @param signature signature to check
     * @return {@code true} if a match was found, {@code false} otherwise
     */
    public boolean hasMethod(@NotNull MethodSignature signature) {
        return MethodSignature.contains(handlerInterfaceMethods, signature);
    }
}
