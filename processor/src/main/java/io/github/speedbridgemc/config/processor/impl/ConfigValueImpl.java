package io.github.speedbridgemc.config.processor.impl;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.ConfigValue;
import io.github.speedbridgemc.config.processor.api.ConfigValueExtension;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import java.util.Optional;

public abstract class ConfigValueImpl implements ConfigValue {
    protected final @NotNull String name;
    protected final @NotNull TypeMirror type;
    protected final @NotNull ImmutableClassToInstanceMap<ConfigValueExtension> extensions;

    protected ConfigValueImpl(@NotNull String name, @NotNull TypeMirror type, @NotNull ImmutableClassToInstanceMap<ConfigValueExtension> extensions) {
        this.name = name;
        this.type = type;
        this.extensions = extensions;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @NotNull TypeMirror type() {
        return type;
    }

    @Override
    public @NotNull <T extends ConfigValueExtension> Optional<T> extension(@NotNull Class<T> type) {
        return Optional.ofNullable(extensions.getInstance(type));
    }

    public static final class Field extends ConfigValueImpl {
        private final @NotNull String fieldName;

        public Field(@NotNull String name, @NotNull TypeMirror type, @NotNull ImmutableClassToInstanceMap<ConfigValueExtension> extensions,
                     @NotNull String fieldName) {
            super(name, type, extensions);
            this.fieldName = fieldName;
        }

        @Override
        public @NotNull CodeBlock generateGet(@NotNull String obj, @NotNull String dst) {
            return CodeBlock.of("$L = $L.$L", dst, obj, fieldName);
        }

        @Override
        public @NotNull CodeBlock generateSet(@NotNull String obj, @NotNull String src) {
            return CodeBlock.of("$L.$L = $L", obj, fieldName, src);
        }

        @Override
        public String toString() {
            return "Field{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", extensions=" + extensions +
                    ", fieldName='" + fieldName + '\'' +
                    '}';
        }
    }

    public static final class Property extends ConfigValueImpl {
        private final @NotNull String getterName, setterName;

        public Property(@NotNull String name, @NotNull TypeMirror type, @NotNull ImmutableClassToInstanceMap<ConfigValueExtension> extensions,
                           @NotNull String getterName, @NotNull String setterName) {
            super(name, type, extensions);
            this.getterName = getterName;
            this.setterName = setterName;
        }

        @Override
        public @NotNull CodeBlock generateGet(@NotNull String obj, @NotNull String dst) {
            return CodeBlock.of("$L = $L.$L()", dst, obj, getterName);
        }

        @Override
        public @NotNull CodeBlock generateSet(@NotNull String obj, @NotNull String src) {
            return CodeBlock.of("$L.$L($L)", obj, setterName, src);
        }

        @Override
        public String toString() {
            return "Property{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", extensions=" + extensions +
                    ", getterName='" + getterName + '\'' +
                    ", setterName='" + setterName + '\'' +
                    '}';
        }
    }
}
