package io.github.speedbridgemc.config.processor.serialize.api;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.api.IdentifiedProvider;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public interface SerializerProvider extends IdentifiedProvider {
    void process(@NotNull String name, @NotNull TypeElement type,
                 @NotNull ImmutableList<VariableElement> fields,
                 @NotNull SerializerContext ctx,
                 @NotNull TypeSpec.Builder classBuilder);
}
