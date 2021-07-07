package io.github.speedbridgemc.config.processor.impl.property;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtension;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyFlag;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableClassToInstanceMap;
import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableSet;

public abstract class ConfigPropertyImpl implements ConfigProperty {
    private final @NotNull Lazy<ConfigType> type;
    protected final @NotNull String name;
    protected final @NotNull ImmutableSet<ConfigPropertyFlag> flags;
    protected final @NotNull ImmutableClassToInstanceMap<ConfigPropertyExtension> extensions;
    protected final boolean canSet;

    protected ConfigPropertyImpl(@NotNull Lazy<ConfigType> type, @NotNull String name,
                                 @NotNull Set<ConfigPropertyFlag> flags,
                                 @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                                 boolean canSet) {
        this.type = type;
        this.name = name;
        this.flags = toImmutableSet(flags);
        this.extensions = toImmutableClassToInstanceMap(extensions);
        this.canSet = canSet;
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
    public @NotNull Set<? extends ConfigPropertyFlag> flags() {
        return flags;
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
                     @NotNull Set<ConfigPropertyFlag> flags,
                     @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                     boolean canSet,
                     @NotNull String fieldName) {
            super(typeLazy, name, flags, extensions, canSet);
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
                         @NotNull Set<ConfigPropertyFlag> flags,
                         @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                         @NotNull String getterName, @NotNull String setterName) {
            super(typeLazy, name, flags, extensions, true);
            this.getterName = getterName;
            this.setterName = setterName;
        }

        public Accessors(@NotNull Lazy<ConfigType> typeLazy, @NotNull String name,
                         @NotNull Set<ConfigPropertyFlag> flags,
                         @NotNull ClassToInstanceMap<ConfigPropertyExtension> extensions,
                         @NotNull String getterName) {
            super(typeLazy, name, flags, extensions, false);
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
