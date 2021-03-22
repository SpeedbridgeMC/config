package io.github.speedbridgemc.config.processor.api;

import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class MethodSignature {
    private final @NotNull String name;
    private final @NotNull TypeName @NotNull [] parameters;
    private final boolean isDefault;
    private final @Nullable TypeName returnType;

    private MethodSignature(@NotNull String name, @NotNull TypeName @NotNull [] parameters, boolean isDefault, @Nullable TypeName returnType) {
        this.name = name;
        this.parameters = parameters;
        this.isDefault = isDefault;
        this.returnType = returnType;
    }

    public static @NotNull MethodSignature fromElement(@NotNull ExecutableElement method) {
        ArrayList<TypeName> parameters = new ArrayList<>();
        for (VariableElement parameter : method.getParameters())
            parameters.add(TypeName.get(parameter.asType()).withoutAnnotations());
        return new MethodSignature(method.getSimpleName().toString(), parameters.toArray(new TypeName[0]),
                method.isDefault(), TypeName.get(method.getReturnType()).withoutAnnotations());
    }

    public static @NotNull MethodSignature of(@NotNull String name, @NotNull TypeName... parameters) {
        return new MethodSignature(name, parameters, false, null);
    }

    public static @NotNull MethodSignature ofDefault(@NotNull String name, @NotNull TypeName... parameters) {
        return new MethodSignature(name, parameters, true, null);
    }

    public static @NotNull MethodSignature of(@NotNull TypeName returnType, @NotNull String name, @NotNull TypeName... parameters) {
        return new MethodSignature(name, parameters, false, returnType);
    }

    public static @NotNull MethodSignature ofDefault(@NotNull TypeName returnType, @NotNull String name, @NotNull TypeName... parameters) {
        return new MethodSignature(name, parameters, true, returnType);
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull TypeName @NotNull [] getParameters() {
        return parameters.clone();
    }

    public boolean isDefault() {
        return isDefault;
    }

    public @Nullable TypeName getReturnType() {
        return returnType;
    }

    public boolean matches(@NotNull MethodSignature other) {
        if (returnType == null)
            return toStringWithoutReturnType().equals(other.toStringWithoutReturnType());
        else
            return toString().equals(other.toString());
    }

    public static boolean contains(@NotNull Iterable<@NotNull ? extends MethodSignature> methods,
                                   @NotNull MethodSignature signature) {
        for (MethodSignature method : methods) {
            System.out.println("Checking if " + signature + " matches " + method);
            if (signature.matches(method))
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MethodSignature that = (MethodSignature) o;
        return isDefault == that.isDefault && name.equals(that.name)
                && Arrays.equals(parameters, that.parameters)
                && Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, isDefault, returnType);
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }

    private @Nullable String cachedNRTString, cachedFullString;

    public @NotNull String toStringWithoutReturnType() {
        if (cachedNRTString == null) {
            StringBuilder sb = new StringBuilder(name).append('(');
            for (TypeName param : parameters)
                sb.append(param.toString()).append(", ");
            if (parameters.length > 0)
                sb.setLength(sb.length() - 2);
            cachedNRTString = sb.append(')').toString();
        }
        return cachedNRTString;
    }

    @Override
    public @NotNull String toString() {
        if (cachedFullString == null) {
            StringBuilder sb = new StringBuilder();
            if (returnType == null)
                sb.append("<ignored>");
            else
                sb.append(returnType.toString());
            sb.append(' ').append(toStringWithoutReturnType());
            cachedFullString = sb.toString();
        }
        return cachedFullString;
    }
}
