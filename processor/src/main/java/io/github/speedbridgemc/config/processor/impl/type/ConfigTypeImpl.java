package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableList;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeKind;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableList;

public abstract class ConfigTypeImpl implements ConfigType {
    protected final @NotNull ConfigTypeKind kind;
    protected final @NotNull String name;
    protected final @NotNull TypeMirror typeMirror;

    protected ConfigTypeImpl(@NotNull ConfigTypeKind kind, @NotNull String name, @NotNull TypeMirror typeMirror) {
        this.kind = kind;
        this.name = name;
        this.typeMirror = typeMirror;
    }

    @Override
    public @NotNull ConfigTypeKind kind() {
        return kind;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @NotNull TypeMirror asMirror() {
        return typeMirror;
    }

    @Override
    public boolean isNullable() {
        return true;
    }

    @Override
    public @NotNull List<? extends String> enumConstants() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull Optional<ConfigType> componentType() {
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<ConfigType> keyType() {
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<ConfigType> valueType() {
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<StructInstantiationStrategy> instantiationStrategy() {
        return Optional.empty();
    }

    @Override
    public @NotNull List<? extends ConfigProperty> properties() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return name;
    }

    public static final class Primitive extends ConfigTypeImpl {
        private final boolean isNullable;

        public Primitive(@NotNull ConfigTypeKind kind, @NotNull String name, @NotNull TypeMirror typeMirror, boolean isNullable) {
            super(kind, name, typeMirror);
            this.isNullable = isNullable;
        }

        @Override
        public boolean isNullable() {
            return isNullable;
        }
    }

    public static final class Enum extends ConfigTypeImpl {
        private final @NotNull ImmutableList<String> constants;

        public Enum(@NotNull TypeMirror typeMirror, @NotNull List<String> constants) {
            super(ConfigTypeKind.ENUM, "enum{" + typeMirror + "=" + String.join(",", constants) + "}", typeMirror);
            this.constants = toImmutableList(constants);
        }

        @Override
        public @NotNull List<? extends String> enumConstants() {
            return constants;
        }
    }

    public static final class Array extends ConfigTypeImpl {
        private final @NotNull Lazy<ConfigType> componentType;

        public Array(@NotNull TypeMirror typeMirror, @NotNull Lazy<ConfigType> componentType) {
            super(ConfigTypeKind.ARRAY, "", typeMirror);
            this.componentType = componentType;
        }

        @Override
        public @NotNull String name() {
            return "array{" + componentType.get().name() + "}";
        }

        @Override
        public @NotNull Optional<ConfigType> componentType() {
            return Optional.of(componentType.get());
        }
    }

    public static final class Map extends ConfigTypeImpl {
        private final @NotNull Lazy<ConfigType> keyType, valueType;

        public Map(@NotNull TypeMirror typeMirror, @NotNull Lazy<ConfigType> keyType, @NotNull Lazy<ConfigType> valueType) {
            super(ConfigTypeKind.MAP, "", typeMirror);
            this.keyType = keyType;
            this.valueType = valueType;
        }

        @Override
        public @NotNull String name() {
            return "map{" + keyType.get().name() + " -> " + valueType.get().name() + "}";
        }

        @Override
        public @NotNull Optional<ConfigType> keyType() {
            return Optional.of(keyType.get());
        }

        @Override
        public @NotNull Optional<ConfigType> valueType() {
            return Optional.of(valueType.get());
        }
    }

    public static final class Struct extends ConfigTypeImpl {
        private final @NotNull StructInstantiationStrategy instantiationStrategy;
        private final @NotNull ImmutableList<ConfigProperty> properties;

        public Struct(@NotNull TypeMirror typeMirror,
                         @NotNull StructInstantiationStrategy instantiationStrategy,
                         @NotNull List<ConfigProperty> properties) {
            super(ConfigTypeKind.STRUCT, "struct{" + typeMirror + "}", typeMirror);
            this.instantiationStrategy = instantiationStrategy;
            this.properties = toImmutableList(properties);
        }

        @Override
        public @NotNull Optional<StructInstantiationStrategy> instantiationStrategy() {
            return Optional.of(instantiationStrategy);
        }

        @Override
        public @NotNull List<? extends ConfigProperty> properties() {
            return properties;
        }
    }
}
