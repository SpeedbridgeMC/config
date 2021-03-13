package io.github.speedbridgemc.config.processor.serialize.api.jankson;

import com.squareup.javapoet.CodeBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public interface JanksonDelegate {
    void init(@NotNull ProcessingEnvironment processingEnv);
    boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, @NotNull CodeBlock.Builder codeBuilder);
    boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, @NotNull CodeBlock.Builder codeBuilder);
}
