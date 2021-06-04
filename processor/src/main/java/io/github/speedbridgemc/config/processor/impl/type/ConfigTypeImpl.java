package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableList;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeKind;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
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
    public final @NotNull ConfigTypeKind kind() {
        return kind;
    }

    @Override
    public final @NotNull String name() {
        return name;
    }

    @Override
    public final @NotNull TypeMirror asMirror() {
        return typeMirror;
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
        public Primitive(@NotNull ConfigTypeKind kind, @NotNull String name, @NotNull TypeMirror typeMirror) {
            super(kind, name, typeMirror);
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
        private final @NotNull ConfigType componentType;

        public Array(@NotNull TypeMirror typeMirror, @NotNull ConfigType componentType) {
            super(ConfigTypeKind.ARRAY, "array{" + componentType.name() + "}", typeMirror);
            this.componentType = componentType;
        }

        @Override
        public @NotNull Optional<ConfigType> componentType() {
            return Optional.of(componentType);
        }
    }

    public static final class Map extends ConfigTypeImpl {
        private final @NotNull ConfigType keyType, valueType;

        public Map(@NotNull TypeMirror typeMirror, @NotNull ConfigType keyType, @NotNull ConfigType valueType) {
            super(ConfigTypeKind.MAP, "map{" + keyType.name() + " -> " + valueType.name() + "}", typeMirror);
            this.keyType = keyType;
            this.valueType = valueType;
        }

        @Override
        public @NotNull Optional<ConfigType> keyType() {
            return Optional.of(keyType);
        }

        @Override
        public @NotNull Optional<ConfigType> valueType() {
            return Optional.of(valueType);
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
