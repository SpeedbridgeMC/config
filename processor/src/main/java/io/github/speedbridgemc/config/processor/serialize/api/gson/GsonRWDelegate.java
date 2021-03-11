package io.github.speedbridgemc.config.processor.serialize.api.gson;

import com.squareup.javapoet.CodeBlock;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

public interface GsonRWDelegate {
    void init(@NotNull ProcessingEnvironment processingEnv);
    boolean appendRead(@NotNull GsonRWContext ctx, @NotNull VariableElement field, @NotNull CodeBlock.Builder codeBuilder);
    boolean appendWrite(@NotNull GsonRWContext ctx, @NotNull VariableElement field, @NotNull CodeBlock.Builder codeBuilder);
}
