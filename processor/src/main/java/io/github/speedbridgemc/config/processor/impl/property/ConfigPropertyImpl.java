package io.github.speedbridgemc.config.processor.impl.property;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtension;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableClassToInstanceMap;

public abstract class ConfigPropertyImpl implements ConfigProperty {
    private final @NotNull Lazy<ConfigType> type;
    protected final @NotNull String name;
    protected final @NotNull ImmutableClassToInstanceMap<ConfigPropertyExtension> extensions;
    protected final boolean canSet, isOptional;

    protected ConfigPropertyImpl(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                 @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                                 boolean canSet, boolean isOptional) {
        this.type = type;
        this.name = name;
        this.extensions = toImmutableClassToInstanceMap(extensions);
        this.canSet = canSet;
        this.isOptional = isOptional;
    }

    @Override
    public @NotNull ConfigType type() {
        return type.get();
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public @NotNull <E extends ConfigPropertyExtension> Optional<E> extension(@NotNull Class<E> type) {
        return Optional.ofNullable(extensions.getInstance(type));
    }

    @Override
    public boolean canSet() {
        return canSet;
    }

    public static final class Field extends ConfigPropertyImpl {
        private final @NotNull String fieldName;

        public Field(@NotNull Lazy<ConfigType> typeLazy, @NotNull String name,
                     @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                     boolean canSet, boolean isOptional,
                     @NotNull String fieldName) {
            super(typeLazy, name, extensions, canSet, isOptional);
            this.fieldName = fieldName;
        }

        @Override
        public @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String destination) {
            return CodeBlock.of("$L = $L.$L", destination, object, fieldName);
        }

        @Override
        public @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String source) {
            if (!canSet)
                throw new IllegalStateException("Can't set this property!");
            return CodeBlock.of("$L.$L = $L", object, source, fieldName);
        }
    }

    public static final class Accessors extends ConfigPropertyImpl {
        private final @NotNull String getterName;
        private final @Nullable String setterName;

        public Accessors(@NotNull Lazy<ConfigType> typeLazy, @NotNull String name,
                         @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                         boolean isOptional, @NotNull String getterName, @NotNull String setterName) {
            super(typeLazy, name, extensions, true, isOptional);
            this.getterName = getterName;
            this.setterName = setterName;
        }

        public Accessors(@NotNull Lazy<ConfigType> typeLazy, @NotNull String name,
                         @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                         boolean isOptional, @NotNull String getterName) {
            super(typeLazy, name, extensions, false, isOptional);
            this.getterName = getterName;
            setterName = null;
        }

        @Override
        public @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String destination) {
            return CodeBlock.of("$L = $L.$L()", destination, object, getterName);
        }

        @Override
        public @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String source) {
            if (!canSet)
                throw new IllegalStateException("Can't set this property!");
            return CodeBlock.of("$L.$L($L)", object, setterName, source);
        }
    }
}
