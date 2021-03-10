package io.github.speedbridgemc.config.processor.serialize.api;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public interface SerializerProvider {
    @NotNull String getId();
    void init(@NotNull ProcessingEnvironment processingEnv);
    void process(@NotNull String name, @NotNull TypeElement type,
                 @NotNull ImmutableSet<VariableElement> fields,
                 @NotNull SerializerContext ctx,
                 @NotNull TypeSpec.Builder classBuilder);
}
