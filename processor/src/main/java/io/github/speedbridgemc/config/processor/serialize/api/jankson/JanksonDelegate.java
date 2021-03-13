package io.github.speedbridgemc.config.processor.serialize.api.jankson;

import com.squareup.javapoet.CodeBlock;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

public interface JanksonDelegate {
    void init(@NotNull ProcessingEnvironment processingEnv);
    boolean appendRead(@NotNull JanksonContext ctx, @NotNull VariableElement field, @NotNull String dest, @NotNull CodeBlock.Builder codeBuilder);
    boolean appendWrite(@NotNull JanksonContext ctx, @NotNull VariableElement field, @NotNull String src, @NotNull CodeBlock.Builder codeBuilder);
}
