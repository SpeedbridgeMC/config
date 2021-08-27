package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableList;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.type.*;
import io.github.speedbridgemc.config.processor.api.util.Lazy;

import javax.lang.model.type.TypeMirror;
import java.util.List;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableList;

public abstract class ConfigTypeImpl implements ConfigType {
    protected final ConfigTypeKind kind;
    protected final String name;
    protected final TypeMirror typeMirror;

    protected ConfigTypeImpl(ConfigTypeKind kind, String name, TypeMirror typeMirror) {
        this.kind = kind;
        this.name = name;
        this.typeMirror = typeMirror;
    }

    @Override
    public ConfigTypeKind kind() {
        return kind;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public TypeMirror asMirror() {
        return typeMirror;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public ConfigType get() {
        return this;
    }

    public static final class Primitive extends ConfigTypeImpl {
        public Primitive(ConfigTypeKind kind, String name, TypeMirror typeMirror) {
            super(kind, name, typeMirror);
        }

    }

    public static final class Enum extends ConfigTypeImpl implements ConfigEnum {
        private final ImmutableList<String> constants;

        public Enum(TypeMirror typeMirror, List<String> constants) {
            super(ConfigTypeKind.ENUM, "enum{" + typeMirror + "=" + String.join(",", constants) + "}", typeMirror);
            this.constants = toImmutableList(constants);
        }

        @Override
        public List<? extends String> enumConstants() {
            return constants;
        }
    }

    public static final class Array extends ConfigTypeImpl implements ConfigArray {
        private final Lazy<ConfigType> componentType;

        public Array(TypeMirror typeMirror, Lazy<ConfigType> componentType) {
            super(ConfigTypeKind.ARRAY, "", typeMirror);
            this.componentType = componentType;
        }

        @Override
        public String name() {
            return "array{" + componentType.get().name() + "}";
        }

        @Override
        public ConfigType componentType() {
            return componentType.get();
        }
    }

    public static final class Map extends ConfigTypeImpl implements ConfigMap {
        private final Lazy<ConfigType> keyType, valueType;

        public Map(TypeMirror typeMirror, Lazy<ConfigType> keyType, Lazy<ConfigType> valueType) {
            super(ConfigTypeKind.MAP, "", typeMirror);
            this.keyType = keyType;
            this.valueType = valueType;
        }

        @Override
        public String name() {
            return "map{" + keyType.get().name() + " -> " + valueType.get().name() + "}";
        }

        @Override
        public ConfigType keyType() {
            return keyType.get();
        }

        @Override
        public ConfigType valueType() {
            return valueType.get();
        }
    }

    public static final class Struct extends ConfigTypeImpl implements ConfigStruct {
        private final StructInstantiationStrategy instantiationStrategy;
        private final ImmutableList<ConfigProperty> properties;

        public Struct(TypeMirror typeMirror,
                         StructInstantiationStrategy instantiationStrategy,
                         List<ConfigProperty> properties) {
            super(ConfigTypeKind.STRUCT, "struct{" + typeMirror + "}", typeMirror);
            this.instantiationStrategy = instantiationStrategy;
            this.properties = toImmutableList(properties);
        }

        @Override
        public StructInstantiationStrategy instantiationStrategy() {
            return instantiationStrategy;
        }

        @Override
        public List<? extends ConfigProperty> properties() {
            return properties;
        }
    }
}
