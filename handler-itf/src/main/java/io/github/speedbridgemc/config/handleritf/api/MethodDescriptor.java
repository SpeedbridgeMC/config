package io.github.speedbridgemc.config.handleritf.api;

import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeMirror;
import java.util.Optional;

public final class MethodDescriptor {
    private final String name;
    private final TypeMirror [] paramTypes;
    private final @Nullable TypeMirror returnType;

    public static MethodDescriptor of(String name, TypeMirror [] paramTypes) {
        return new MethodDescriptor(name, paramTypes.clone(), null);
    }

    public static MethodDescriptor of(TypeMirror returnType, String name, TypeMirror [] paramTypes) {
        return new MethodDescriptor(name, paramTypes.clone(), returnType);
    }

    private MethodDescriptor(String name, TypeMirror [] paramTypes, @Nullable TypeMirror returnType) {
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    public String name() {
        return name;
    }

    public TypeMirror [] paramTypes() {
        return paramTypes.clone();
    }

    public Optional<TypeMirror> returnType() {
        return Optional.ofNullable(returnType);
    }
}
