package io.github.speedbridgemc.config.processor.serialize.api;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SerializerContext {
    public final @NotNull TypeName configType;
    public final @Nullable String basePackage, mode;
    public final @NotNull String @NotNull [] options;
    public final @NotNull MethodSpec.Builder readMethodBuilder, writeMethodBuilder;
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;

    public SerializerContext(@NotNull TypeName configType, @Nullable String basePackage, @Nullable String mode, @NotNull String @NotNull [] options,
                             MethodSpec.@NotNull Builder readMethodBuilder, MethodSpec.@NotNull Builder writeMethodBuilder, @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation) {
        this.configType = configType;
        this.basePackage = basePackage;
        this.mode = mode;
        this.options = options;
        this.readMethodBuilder = readMethodBuilder;
        this.writeMethodBuilder = writeMethodBuilder;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
    }
}
