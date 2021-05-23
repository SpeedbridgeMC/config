package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.ClassToInstanceMap;
import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.impl.ConfigValueImpl;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import java.util.Optional;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableClassToInstanceMap;

public interface ConfigValue {
    static @NotNull ConfigValue field(@NotNull String name, @NotNull TypeMirror type, @NotNull ClassToInstanceMap<ConfigValueExtension> extensions,
                                      @NotNull String fieldName) {
        return new ConfigValueImpl.Field(name, type, toImmutableClassToInstanceMap(extensions), fieldName);
    }

    static @NotNull ConfigValue property(@NotNull String name, @NotNull TypeMirror type, @NotNull ClassToInstanceMap<ConfigValueExtension> extensions,
                                         @NotNull String getterName, @NotNull String setterName) {
        return new ConfigValueImpl.Property(name, type, toImmutableClassToInstanceMap(extensions), getterName, setterName);
    }

    @NotNull String name();
    @NotNull TypeMirror type();
    @NotNull CodeBlock generateGet(@NotNull String obj, @NotNull String dst);
    @NotNull CodeBlock generateSet(@NotNull String obj, @NotNull String src);

    <T extends ConfigValueExtension> @NotNull Optional<T> extension(@NotNull Class<T> type);
}
