package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.ClassToInstanceMap;
import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.impl.ConfigFieldImpl;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import java.util.Optional;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableClassToInstanceMap;

public interface ConfigField {
    static @NotNull ConfigField field(@NotNull String name, @NotNull TypeMirror type, @NotNull ClassToInstanceMap<ConfigFieldExtension> extensions,
                                      @NotNull String fieldName) {
        return new ConfigFieldImpl.Field(name, type, toImmutableClassToInstanceMap(extensions), fieldName);
    }

    static @NotNull ConfigField property(@NotNull String name, @NotNull TypeMirror type, @NotNull ClassToInstanceMap<ConfigFieldExtension> extensions,
                                         @NotNull String getterName, @NotNull String setterName) {
        return new ConfigFieldImpl.Property(name, type, toImmutableClassToInstanceMap(extensions), getterName, setterName);
    }

    @NotNull String name();
    @NotNull TypeMirror type();
    @NotNull CodeBlock generateGet(@NotNull String obj, @NotNull String dst);
    @NotNull CodeBlock generateSet(@NotNull String obj, @NotNull String src);

    <T extends ConfigFieldExtension> @NotNull Optional<T> extension(@NotNull Class<T> type);
}
