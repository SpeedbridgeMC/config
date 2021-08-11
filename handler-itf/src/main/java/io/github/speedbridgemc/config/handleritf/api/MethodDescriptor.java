package io.github.speedbridgemc.config.handleritf.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeMirror;
import java.util.Optional;

public final class MethodDescriptor {
    private final @NotNull String name;
    private final @NotNull TypeMirror @NotNull [] paramTypes;
    private final @Nullable TypeMirror returnType;

    public static @NotNull MethodDescriptor of(@NotNull String name, @NotNull TypeMirror @NotNull [] paramTypes) {
        return new MethodDescriptor(name, paramTypes.clone(), null);
    }

    public static @NotNull MethodDescriptor of(@NotNull TypeMirror returnType, @NotNull String name, @NotNull TypeMirror @NotNull [] paramTypes) {
        return new MethodDescriptor(name, paramTypes.clone(), returnType);
    }

    private MethodDescriptor(@NotNull String name, @NotNull TypeMirror @NotNull [] paramTypes, @Nullable TypeMirror returnType) {
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull TypeMirror @NotNull [] paramTypes() {
        return paramTypes.clone();
    }

    public @NotNull Optional<@NotNull TypeMirror> returnType() {
        return Optional.ofNullable(returnType);
    }
}
