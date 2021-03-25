package io.github.speedbridgemc.config.processor.serialize.api.gson;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public interface GsonDelegate extends Provider {
    boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, @NotNull CodeBlock.Builder codeBuilder);
    boolean appendWrite(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, @NotNull CodeBlock.Builder codeBuilder);
}
