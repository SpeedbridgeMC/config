package io.github.speedbridgemc.config.processor.impl.property;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtension;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableClassToInstanceMap;

public abstract class ConfigPropertyImpl implements ConfigProperty {
    protected final @NotNull ConfigType type;
    protected final @NotNull String name;
    protected final @NotNull ImmutableClassToInstanceMap<ConfigPropertyExtension> extensions;

    protected ConfigPropertyImpl(@NotNull ConfigType type, @NotNull String name,
                                 @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions) {
        this.type = type;
        this.name = name;
        this.extensions = toImmutableClassToInstanceMap(extensions);
    }

    @Override
    public @NotNull ConfigType type() {
        return type;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @NotNull <E extends ConfigPropertyExtension> Optional<E> extension(@NotNull Class<E> type) {
        return Optional.ofNullable(extensions.getInstance(type));
    }

    public static final class Field extends ConfigPropertyImpl {
        private final @NotNull String fieldName;

        public Field(@NotNull ConfigType type, @NotNull String name,
                     @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                     @NotNull String fieldName) {
            super(type, name, extensions);
            this.fieldName = fieldName;
        }

        @Override
        public @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String destination) {
            return CodeBlock.of("$L = $L.$L", destination, object, fieldName);
        }

        @Override
        public @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String source) {
            return CodeBlock.of("$L.$L = $L", object, source, fieldName);
        }
    }

    public static final class Accessors extends ConfigPropertyImpl {
        private final @NotNull String getterName, setterName;

        public Accessors(@NotNull ConfigType type, @NotNull String name,
                         @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                         @NotNull String getterName, @NotNull String setterName) {
            super(type, name, extensions);
            this.getterName = getterName;
            this.setterName = setterName;
        }

        @Override
        public @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String destination) {
            return CodeBlock.of("$L = $L.$L()", destination, object, getterName);
        }

        @Override
        public @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String source) {
            return CodeBlock.of("$L.$L($L)", object, setterName, source);
        }
    }
}
