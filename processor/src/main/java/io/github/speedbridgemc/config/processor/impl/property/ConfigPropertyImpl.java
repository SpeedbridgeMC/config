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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableClassToInstanceMap;
import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableSet;

public abstract class ConfigPropertyImpl implements ConfigProperty {
    private final Lazy<ConfigType> type;
    protected final String name;
    protected final ImmutableSet<ConfigPropertyFlag> flags;
    protected final ImmutableClassToInstanceMap<ConfigPropertyExtension> extensions;
    protected final boolean canSet;

    protected ConfigPropertyImpl(Lazy<ConfigType> type, String name,
                                 Set<ConfigPropertyFlag> flags,
                                 ClassToInstanceMap<ConfigPropertyExtension> extensions,
                                 boolean canSet) {
        this.type = type;
        this.name = name;
        this.flags = toImmutableSet(flags);
        this.extensions = toImmutableClassToInstanceMap(extensions);
        this.canSet = canSet;
    }

    @Override
    public ConfigType type() {
        return type.get();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<? extends ConfigPropertyFlag> flags() {
        return flags;
    }

    @Override
    public <E extends ConfigPropertyExtension> Optional<E> extension(Class<E> type) {
        return Optional.ofNullable(extensions.getInstance(type));
    }

    @Override
    public boolean canSet() {
        return canSet;
    }

    public static final class Field extends ConfigPropertyImpl {
        private final String fieldName;

        public Field(Lazy<ConfigType> typeLazy, String name,
                     Set<ConfigPropertyFlag> flags,
                     ClassToInstanceMap<ConfigPropertyExtension> extensions,
                     boolean canSet,
                     String fieldName) {
            super(typeLazy, name, flags, extensions, canSet);
            this.fieldName = fieldName;
        }

        @Override
        public CodeBlock generateGet(String object, String destination) {
            return CodeBlock.of("$L = $L.$L", destination, object, fieldName);
        }

        @Override
        public CodeBlock generateSet(String object, String source) {
            if (!canSet)
                throw new IllegalStateException("Can't set this property!");
            return CodeBlock.of("$L.$L = $L", object, source, fieldName);
        }
    }

    public static final class Accessors extends ConfigPropertyImpl {
        private final String getterName;
        private final @Nullable String setterName;

        public Accessors(Lazy<ConfigType> typeLazy, String name,
                         Set<ConfigPropertyFlag> flags,
                         ClassToInstanceMap<ConfigPropertyExtension> extensions,
                         String getterName, String setterName) {
            super(typeLazy, name, flags, extensions, true);
            this.getterName = getterName;
            this.setterName = setterName;
        }

        public Accessors(Lazy<ConfigType> typeLazy, String name,
                         Set<ConfigPropertyFlag> flags,
                         ClassToInstanceMap<ConfigPropertyExtension> extensions,
                         String getterName) {
            super(typeLazy, name, flags, extensions, false);
            this.getterName = getterName;
            setterName = null;
        }

        @Override
        public CodeBlock generateGet(String object, String destination) {
            return CodeBlock.of("$L = $L.$L()", destination, object, getterName);
        }

        @Override
        public CodeBlock generateSet(String object, String source) {
            if (!canSet)
                throw new IllegalStateException("Can't set this property!");
            return CodeBlock.of("$L.$L($L)", object, setterName, source);
        }
    }
}
