package io.github.speedbridgemc.config.handleritf.api;

import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Arrays;
import java.util.Objects;
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

    public boolean matches(Types types, MethodDescriptor other) {
        if (!this.name.equals(other.name))
            return false;
        if (!Arrays.equals(this.paramTypes, other.paramTypes))
            return false;
        if (this.returnType == null)
            return true;
        else if (other.returnType == null)
            return false;
        else
            return types.isSameType(this.returnType, other.returnType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodDescriptor)) return false;
        MethodDescriptor that = (MethodDescriptor) o;
        return name.equals(that.name) && Arrays.equals(paramTypes, that.paramTypes) && Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, returnType);
        result = 31 * result + Arrays.hashCode(paramTypes);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType == null ? "<any>" : returnType.toString())
                .append(' ')
                .append(name)
                .append('(');
        for (int i = 0; i < paramTypes.length; i++) {
            sb.append(paramTypes[i]);
            if (i < paramTypes.length - 1)
                sb.append(", ");
        }
        return sb.append(')').toString();
    }
}
