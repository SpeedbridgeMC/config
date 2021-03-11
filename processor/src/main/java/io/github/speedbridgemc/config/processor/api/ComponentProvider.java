package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public interface ComponentProvider extends Provider {
    void process(@NotNull String name, @NotNull TypeElement type,
                 @NotNull ImmutableList<VariableElement> fields,
                 @NotNull ComponentContext ctx,
                 @NotNull TypeSpec.Builder classBuilder);
}
